package com.music.spotui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.AccountModel
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.preferences.cacheFollowedArtists
import com.music.spotui.data.preferences.cacheLibraryEntries
import com.music.spotui.data.preferences.getCachedFollowedArtists
import com.music.spotui.data.preferences.getCachedLibraryEntries
import com.music.spotui.ui.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: AppRepository,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private var recommendationJob: kotlinx.coroutines.Job? = null
    private var libraryLoadJob: kotlinx.coroutines.Job? = null

    private val _entries: MutableStateFlow<Response<List<LibraryEntry>>> = MutableStateFlow(Response.Loading())
    val entries: StateFlow<Response<List<LibraryEntry>>> = _entries

    private val _account: MutableStateFlow<Response<AccountModel>> = MutableStateFlow(Response.Loading())
    val account: StateFlow<Response<AccountModel>> = _account

    private val _followedArtists: MutableStateFlow<List<com.music.spotui.data.entity.ArtistsModel>> =
        MutableStateFlow(emptyList())
    val followedArtists: StateFlow<List<com.music.spotui.data.entity.ArtistsModel>> = _followedArtists

    init {
        // Seed UI with the last-seen library immediately so it's visible offline.
        val cached = com.music.spotui.data.preferences.CreatedPlaylists.merge(context, getCachedLibraryEntries(context))
        if (cached.isNotEmpty()) _entries.value = Response.Success(com.music.spotui.data.recommendation.LibraryIdentityPolicy.unique(cached))
        val cachedArtists = getCachedFollowedArtists(context)
        if (cachedArtists.isNotEmpty()) _followedArtists.value = cachedArtists

        load()
        loadAccount()
        loadFollowedArtists()
    }

    private val _actionError = MutableStateFlow("")
    val actionError: StateFlow<String> = _actionError
    fun clearActionError() { _actionError.value = "" }
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    private val _created = MutableStateFlow<com.metrolist.spotify.models.SpotifyPlaylist?>(null)
    val created: StateFlow<com.metrolist.spotify.models.SpotifyPlaylist?> = _created
    private val _suggestions = MutableStateFlow<List<com.music.spotui.data.entity.SongsModel>>(emptyList())
    val suggestions: StateFlow<List<com.music.spotui.data.entity.SongsModel>> = _suggestions
    private val _added = MutableStateFlow<List<com.music.spotui.data.entity.SongsModel>>(emptyList())
    val added: StateFlow<List<com.music.spotui.data.entity.SongsModel>> = _added

    fun clearMessage() { _message.value = "" }
    fun finishCreation() { if (!_busy.value) { recommendationJob?.cancel(); _created.value = null; _added.value = emptyList(); _suggestions.value = emptyList() } }
    fun remove(entry: LibraryEntry) {
        if (_busy.value || entry.spotifyId in listOf(com.music.spotui.data.api.Api.LIKED_SONGS_ID, com.music.spotui.data.api.Api.DOWNLOADS_ID)) return
        _actionError.value = ""
        _busy.value = true
        libraryLoadJob?.cancel()
        viewModelScope.launch {
            try {
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.music.spotui.data.api.SpotifyMutations.run(context) {
                        com.metrolist.spotify.Spotify.removeFromLibrary(listOf("spotify:${if (entry.isPlaylist) "playlist" else "album"}:${entry.spotifyId}"))
                    }
                }
                val ok = result.isSuccess
                if (ok) {
                    if(entry.isPlaylist)com.music.spotui.data.preferences.CreatedPlaylists.remove(context, entry.spotifyId)
                    val remaining = (_entries.value as? Response.Success)?.data.orEmpty().filterNot { it.spotifyId == entry.spotifyId }
                    _entries.value = Response.Success(remaining)
                    com.music.spotui.data.api.Api.HomeCache.library = remaining
                    cacheLibraryEntries(context, remaining)
                    _message.value = "Removed from your Spotify library."
                } else _actionError.value = com.music.spotui.data.api.SpotifyMutations.describe(result.exceptionOrNull())
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) { _actionError.value = com.music.spotui.data.api.SpotifyMutations.describe(error)
            } finally { _busy.value = false }
        }
    }
    fun create(name: String) {
        if (_busy.value || name.isBlank()) return
        _actionError.value = ""
        _busy.value = true
        libraryLoadJob?.cancel()
        val creationSession = com.music.spotui.data.preferences.CreatedPlaylists.session(context)
        viewModelScope.launch {
            try {
                val createdResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.music.spotui.data.api.SpotifyMutations.run(context) { com.metrolist.spotify.Spotify.createPlaylist(name.trim()) }
                }
                val playlist = createdResult.getOrNull()
                if (playlist == null || playlist.id.isBlank()) {
                    _actionError.value = com.music.spotui.data.api.SpotifyMutations.describe(createdResult.exceptionOrNull())
                    return@launch
                }
                com.music.spotui.data.preferences.CreatedPlaylists.record(context, creationSession, playlist.id, playlist.name,
                    playlist.images.firstOrNull()?.url.orEmpty(), pending = true)
                _created.value = playlist
                playlist.libraryWarning?.let { _message.value = it + ". Open the created playlist; do not create another copy." }
                _added.value = emptyList()
                val entry = LibraryEntry(playlist.id, playlist.name, "Playlist", "", true)
                val current = (_entries.value as? Response.Success)?.data.orEmpty()
                val merged = com.music.spotui.data.recommendation.LibraryIdentityPolicy.unique(current + entry)
                _entries.value = Response.Success(merged)
                com.music.spotui.data.api.Api.HomeCache.library = merged
                cacheLibraryEntries(context, merged)
                val saved = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.music.spotui.data.api.SpotifyMutations.run(context) { com.metrolist.spotify.Spotify.addToLibrary(listOf("spotify:playlist:${playlist.id}")) }
                }
                if (saved.isSuccess) {
                    com.music.spotui.data.preferences.CreatedPlaylists.markSaved(context, playlist.id)
                    _message.value = "Created and saved to Spotify"
                } else {
                    _created.value = playlist.copy(libraryWarning = "Spotify library save is pending")
                    _actionError.value = "Your playlist was created and kept in Guntify. Retry saving this playlist, rather than creating another.\n" +
                        com.music.spotui.data.api.SpotifyMutations.describe(saved.exceptionOrNull())
                }
                recommendationJob = viewModelScope.launch {
                try {
                _suggestions.value = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val history = com.music.spotui.data.preferences.getListeningHistory(context)
                        .filter { it.source != "spotify" && com.music.spotui.audio.ListeningQualification.qualifies(it.listenedMs) && it.spotifyTrackId.isNotBlank() }
                    val localSeeds = history.groupBy { it.spotifyTrackId }.entries.sortedByDescending { (_, plays) ->
                        plays.sumOf { kotlin.math.exp(-((System.currentTimeMillis() - it.ts).coerceAtLeast(0L) / 86_400_000.0) / 30.0) }
                    }.take(3).map { it.key }
                    var spotifySongs = emptyList<com.music.spotui.data.entity.SongsModel>()
                    repository.provideSongs().collect { if (it is Response.Success) spotifySongs = it.data }
                    val local = if (localSeeds.isEmpty()) emptyList() else repository.provideRecommendations(listOf(localSeeds.first()))
                    val spotify = if (spotifySongs.isEmpty()) emptyList() else repository.provideRecommendations(listOf(spotifySongs.first().spotifyTrackId))
                    // Interleave both sources so a long local list cannot crowd out Spotify.
                    (0 until maxOf(local.size, spotify.size)).flatMap { i -> listOfNotNull(local.getOrNull(i), spotify.getOrNull(i)) }
                        .plus(spotifySongs).filter { it.spotifyTrackId.isNotBlank() }.distinctBy { it.spotifyTrackId }.take(20)
                }
                } catch(c: kotlinx.coroutines.CancellationException) { throw c } catch(_: Exception) { _suggestions.value = emptyList() }
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                if (_created.value != null) _message.value = "Playlist created, but recommendations could not load. Open it to add songs."
                else _actionError.value = com.music.spotui.data.api.SpotifyMutations.describe(e)
            } finally { _busy.value = false }
        }
    }
    fun saveCreated() {
        val p = _created.value ?: return
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) { com.music.spotui.data.api.SpotifyMutations.run(context) {
                    com.metrolist.spotify.Spotify.addToLibrary(listOf("spotify:playlist:${p.id}"))
                } }
                if (result.isSuccess) { com.music.spotui.data.preferences.CreatedPlaylists.markSaved(context, p.id); _created.value = p.copy(libraryWarning = null); _message.value = "Saved to your Spotify library"; _actionError.value = "" }
                else _actionError.value = com.music.spotui.data.api.SpotifyMutations.describe(result.exceptionOrNull())
            } finally { _busy.value = false }
        }
    }
    fun addRecommendation(song: com.music.spotui.data.entity.SongsModel) {
        val playlist = _created.value ?: return
        if (_busy.value || song in _added.value) return
        _actionError.value = ""
        _busy.value = true
        viewModelScope.launch {
            var songAdded = false
            try {
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.music.spotui.data.api.SpotifyMutations.run(context) { com.metrolist.spotify.Spotify.addTracksToPlaylist(playlist.id, listOf("spotify:track:${song.spotifyTrackId}")) }
                }
                val ok = result.isSuccess
                if (!ok) { _actionError.value = com.music.spotui.data.api.SpotifyMutations.describe(result.exceptionOrNull()); return@launch }
                songAdded = true
                _added.value = _added.value + song
                val cover = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.music.spotui.data.api.PlaylistCover.generate(context, playlist.id, _added.value)
                }
                cover?.let { path ->
                    val current = (_entries.value as? Response.Success)?.data.orEmpty().map { if (it.spotifyId == playlist.id) it.copy(coverUri = path) else it }
                    _entries.value = Response.Success(current)
                    cacheLibraryEntries(context, current)
                    com.music.spotui.data.api.Api.HomeCache.library = current
                }
                if (cover != null) {
                    val uploaded = kotlinx.coroutines.withContext(Dispatchers.IO) { com.music.spotui.data.api.PlaylistCover.upload(playlist.id, cover) }
                    if (!uploaded) _message.value = "Song added. Cover is saved locally; Spotify did not accept the cover upload."
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) { _message.value = if (songAdded) "Song added, but cover could not be generated." else "Could not add this song. Try again."
            } finally { _busy.value = false }
        }
    }

    fun load() {
        libraryLoadJob?.cancel()
        libraryLoadJob = viewModelScope.launch(Dispatchers.IO) {
        repository.provideLibrary().collect { response ->
            // Offline: keep the saved library instead of replacing it with a
            // spinner/error, so downloads stay reachable after a restart (#51).
            if (response !is Response.Success && _entries.value is Response.Success) return@collect
            // Nothing saved yet and no network: offer just the downloads (not cached).
            if (response is Response.Error) {
                _entries.value = Response.Success(listOf(com.music.spotui.data.api.Api.DOWNLOADED_ENTRY))
                return@collect
            }
            if (response is Response.Success) {
                val items = com.music.spotui.data.recommendation.LibraryIdentityPolicy.unique(response.data).map { entry ->
                    val cover = com.music.spotui.data.api.PlaylistCover.localPath(context, entry.spotifyId)
                    if (entry.isPlaylist && cover != null) entry.copy(coverUri = cover) else entry
                }
                _entries.value = Response.Success(items)
                cacheLibraryEntries(context, items)
            } else _entries.value = response
        }
    }

    }

    private fun loadFollowedArtists() = viewModelScope.launch(Dispatchers.IO) {
        val artists = repository.provideFollowedArtists()
        _followedArtists.value = artists
        if (artists.isNotEmpty()) cacheFollowedArtists(context, artists)
    }

    private fun loadAccount() = viewModelScope.launch(Dispatchers.IO) {
        repository.provideAccount().collect { _account.value = it }
    }
}
