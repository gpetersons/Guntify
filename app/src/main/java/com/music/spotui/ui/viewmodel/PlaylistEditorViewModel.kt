package com.music.spotui.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.spotify.Spotify
import com.music.spotui.data.api.*
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.ui.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class PlaylistEditorViewModel @Inject constructor(private val repository: AppRepository,
    @ApplicationContext private val context: Context): ViewModel() {
    data class State(val loading: Boolean = true, val busy: Boolean = false, val editable: Boolean = false,
        val songs: List<SongsModel> = emptyList(), val error: String = "", val changes: Int = 0)
    private val mutable = MutableStateFlow(State())
    val state = mutable.asStateFlow()
    private var playlistId = ""
    private var manageCover = false
    private var loadJob: Job? = null
    fun load(id: String, newlyCreated: Boolean = false) {
        loadJob?.cancel(); playlistId = id
        manageCover = newlyCreated || PlaylistCover.localPath(context, id) != null
        mutable.value = State(editable = newlyCreated, changes = mutable.value.changes)
        loadJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    check(SpotifyTokenProvider.ensureToken(context)) { "Sign in to Spotify again" }
                    val editable = newlyCreated || run {
                        val owner = Spotify.playlist(id).getOrThrow().owner?.id.orEmpty()
                        val me = Spotify.me().getOrThrow().id
                        owner.isNotBlank() && owner == me
                    }
                    mutable.update { it.copy(editable = editable) }
                    repository.providePlaylistSongs(id).collect { response ->
                        when(response) {
                            is Response.Success -> mutable.update { it.copy(songs = response.data, loading = false, error = "") }
                            is Response.Error -> mutable.update { it.copy(loading = false, error = "Couldn't load these songs. Try again.") }
                            is Response.Loading -> Unit
                        }
                    }
                }
            } catch(c: CancellationException) { throw c
            } catch(e: Exception) { mutable.update { it.copy(loading = false, error = SpotifyMutations.describe(e)) } }
        }
    }
    fun add(songs: List<SongsModel>) {
        val selected = songs.filter { it.spotifyTrackId.isNotBlank() }.distinctBy { it.spotifyTrackId }
        if (selected.isEmpty() || selected.size > 50) return
        change { Spotify.addTracksToPlaylist(playlistId, selected.map { "spotify:track:${it.spotifyTrackId}" }) }
    }
    fun remove(song: SongsModel) {
        if (song.playlistItemUid.isBlank()) { mutable.update { it.copy(error = "Refresh this playlist before removing the song.") }; return }
        change { Spotify.removeTracksFromPlaylist(playlistId, listOf(Spotify.PlaylistItemRef("spotify:track:${song.spotifyTrackId}", song.playlistItemUid))) }
    }
    private fun change(write: suspend () -> Result<Unit>) {
        if (mutable.value.busy || mutable.value.loading || !mutable.value.editable) return
        mutable.update { it.copy(busy = true, error = "") }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { SpotifyMutations.run(context, write) }
                result.getOrThrow()
                SpotifySync.invalidatePlaylist(playlistId)
                // Never remove a row locally before Spotify has accepted the write.
                mutable.update { it.copy(changes = it.changes + 1) }
                withContext(Dispatchers.IO) { repository.providePlaylistSongs(playlistId).collect { response ->
                    when(response) {
                        is Response.Success -> mutable.update { it.copy(songs = response.data) }
                        is Response.Error -> mutable.update { it.copy(error = "Saved to Spotify. Refresh to see the latest songs.") }
                        is Response.Loading -> Unit
                    }
                } }
                if (manageCover && mutable.value.songs.isNotEmpty()) withContext(Dispatchers.IO) {
                    runCatching { PlaylistCover.generate(context, playlistId, mutable.value.songs)?.let { PlaylistCover.upload(playlistId, it) } }
                }
            } catch(c: CancellationException) { throw c
            } catch(e: Exception) { mutable.update { it.copy(error = SpotifyMutations.describe(e)) }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
