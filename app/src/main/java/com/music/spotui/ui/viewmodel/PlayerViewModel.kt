package com.music.spotui.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.CurrentSongState
import com.music.spotui.di.SongPlayer
import com.music.spotui.audio.RadioPlaybackController
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(private val currentSongState: CurrentSongState, private val repository: AppRepository, private val radioPlayback: RadioPlaybackController) : ViewModel(){

    val currentSongTitle: State<String> get() = currentSongState.title
    val currentSongSinger: State<String> get() = currentSongState.singer
    val currentSongCoverUri: State<String> get() = currentSongState.coverUri
    val currentSongPlayingState: State<Boolean> get() = currentSongState.playingState
    val currentSongIndex : State<Int> get() = currentSongState.songIndex
    val currentSongAlbum : State<String> get() = currentSongState.album

    val currentSongId : State<Int> get() = currentSongState.songId

    val queue : State<List<SongsModel>> get() = currentSongState.queue

    fun updateQueue(songs: List<SongsModel>) = currentSongState.updateQueue(songs)
    fun startShuffled(songs: List<SongsModel>) = currentSongState.startShuffled(songs)

    /** Phone and Android Auto must take the same route into Radio playback. */
    fun startRadioFromSong(song: SongsModel, context: Context) {
        radioPlayback.startRadioFromSong(song, context, viewModelScope)
    }

    /** Restart a saved radio from its last played track and a freshly curated queue. */
    fun resumeRadio(seed: SongsModel, songs: List<SongsModel>, context: Context, shuffled: Boolean = false, savedShuffle:Boolean = false) {
        if (songs.isEmpty()) return
        com.music.spotui.data.preferences.RadioSessions.remember(context, seed)
        if (shuffled) currentSongState.startShuffled(songs) else currentSongState.restore(com.music.spotui.data.preferences.PlaybackQueueSnapshot(songs,null,null,savedShuffle,null))
        currentSongState.activateRadio(seed.spotifyTrackId.ifBlank { seed.id.toString() })
        markRadio(seed)
        val song = currentSongState.queue.value.first()
        updateSongState(song.coverUri, song.title, song.singer, true, song.id, 0, song.album)
        SongPlayer.playSong(song.url, context)
    }

    val activeCollection get() = currentSongState.collection
    fun restoreQueue(snapshot: com.music.spotui.data.preferences.PlaybackQueueSnapshot) {
        val currentId = com.music.spotui.data.preferences.loadLastPlayback(com.music.spotui.MyApplication.instance)?.first?.id
        if (snapshot.collection?.type == "radio" && currentId != null) {
            val songs = com.music.spotui.data.recommendation.EvolvingRadioPolicy.window(snapshot.songs, currentId, snapshot.playedThroughId ?: currentId)
            val ids = songs.map { it.id }.toSet()
            currentSongState.restore(snapshot.copy(songs = songs, original = snapshot.original?.filter { it.id in ids }))
        } else currentSongState.restore(snapshot)
    }
    private fun markRadio(seed: SongsModel) {
        val id = seed.spotifyTrackId.ifBlank { seed.id.toString() }
        com.music.spotui.data.preferences.HomeHiddenItems.beginReplay(com.music.spotui.MyApplication.instance, "radio", id)
        currentSongState.collection.value = com.music.spotui.data.entity.PlaybackCollection(
            "radio:$id", "radio", id, com.music.spotui.data.preferences.RadioSessions.stationName(seed), seed.coverUri, seed.singer)
    }
    val activeRadioSeed: String? get() = currentSongState.activeRadioSeed

    private val _radioRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val radioRefreshing: kotlinx.coroutines.flow.StateFlow<Boolean> = _radioRefreshing
    private val _radioRefreshMessage = kotlinx.coroutines.flow.MutableStateFlow("")
    val radioRefreshMessage: kotlinx.coroutines.flow.StateFlow<String> = _radioRefreshMessage
    fun clearRadioRefreshMessage() { _radioRefreshMessage.value = "" }
    private var radioRefreshJob:kotlinx.coroutines.Job?=null
    private var radioRefreshRequest=0L
    fun refreshRadioQueue() {
        val seed = currentSongState.activeRadioSeed ?: return
        if (currentSongState.collection.value?.type != "radio") return
        radioRefreshJob?.cancel()
        val request=++radioRefreshRequest
        val tasteRevision=com.music.spotui.data.preferences.RadioTaste.revision.value
        val generation = currentSongState.generation
        val requestSeed = com.music.spotui.data.preferences.RadioSessions.recommendationSeed(com.music.spotui.MyApplication.instance, seed)
        _radioRefreshing.value = true
        _radioRefreshMessage.value = ""
        radioRefreshJob=viewModelScope.launch {
            try {
                val recommendations = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(25_000L) { repository.provideRadioRecommendations(seed, requestSeed) }.orEmpty()
                }
                if (request!=radioRefreshRequest || tasteRevision!=com.music.spotui.data.preferences.RadioTaste.revision.value || generation != currentSongState.generation || seed != currentSongState.activeRadioSeed || requestSeed != com.music.spotui.data.preferences.RadioSessions.recommendationSeed(com.music.spotui.MyApplication.instance, seed)) return@launch
                val previous = currentSongState.queue.value
                val current = previous.firstOrNull { it.id == currentSongId.value } ?: return@launch
                val refreshed = com.music.spotui.data.recommendation.RadioQueuePolicy.refreshed(current, previous, recommendations, currentSongState.playedThroughId, currentSongState.shuffle.value)
                if (refreshed.isEmpty()) _radioRefreshMessage.value = "Spotify didn't return more songs. Your current queue is unchanged."
                else {
                    currentSongState.replaceRadioQueue(refreshed, undoable = true)
                    _radioRefreshMessage.value = "Radio queue refreshed. Current song kept playing."
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) { _radioRefreshMessage.value = "Couldn't refresh Radio. Your current queue is unchanged."
            } finally { if(request==radioRefreshRequest)_radioRefreshing.value = false }
        }
    }

    suspend fun radioRecommendations(seedId: String): List<SongsModel> =
        runCatching { repository.provideRadioRecommendations(seedId, com.music.spotui.data.preferences.RadioSessions.recommendationSeed(com.music.spotui.MyApplication.instance, seedId)) }.getOrDefault(emptyList())

    /** Insert a track right after the one currently playing ("Play next"). */
    fun playNext(song: SongsModel) {
        val q = currentSongState.queue.value.toMutableList()
        q.removeAll { it.id == song.id }
        val cur = q.indexOfFirst { it.id == currentSongId.value }
        val insertAt = (if (cur >= 0) cur + 1 else 0).coerceAtMost(q.size)
        q.add(insertAt, song)
        currentSongState.updateQueue(q, preserveSession = true)
    }

    /** Queue explicit additions ahead of the collection's remaining tracks. */
    fun addToQueue(song: SongsModel): Boolean = currentSongState.addToQueue(song)

    /** Reorder the queue, moving the track at [from] to [to] (both absolute indices). */
    fun moveQueueItem(from: Int, to: Int) {
        val q = currentSongState.queue.value.toMutableList()
        if (from !in q.indices || to !in q.indices || from == to) return
        q.add(to, q.removeAt(from))
        currentSongState.updateQueue(q, preserveSession = true)
    }

    /** Remove a track from the queue entirely. */
    fun removeFromQueue(song: SongsModel) {
        if (song.id == currentSongId.value) return
        val q = currentSongState.queue.value.toMutableList()
        q.removeAll { it.id == song.id }
        currentSongState.editQueue(q, "Song removed")
    }

    val queueUndo get() = currentSongState.queueUndo
    fun canUndoQueue() = currentSongState.canUndoQueue()
    fun undoQueueEdit() = currentSongState.undoQueueEdit()
    fun dismissQueueUndo() { currentSongState.queueUndo.value = null }
    fun clearUnplayedQueue() {
        val q = currentSongState.queue.value
        val end = com.music.spotui.audio.QueueOrderPolicy.playedIndex(q, currentSongId.value, currentSongState.playedThroughId)
        currentSongState.editQueue(q.take(end + 1), "Upcoming songs cleared")
    }
    fun applyRadioExclusions() {
        if (currentSongState.collection.value?.type != "radio") return
        val q = currentSongState.queue.value
        val end = com.music.spotui.audio.QueueOrderPolicy.playedIndex(q, currentSongId.value, currentSongState.playedThroughId)
        val kept = q.take(end + 1) + q.drop(end + 1).filter {
            com.music.spotui.data.preferences.RadioTaste.allowed(com.music.spotui.MyApplication.instance, it)
        }
        currentSongState.replaceRadioQueue(kept, preserveManual = false)
        refreshRadioQueue()
    }


    val shuffleState = currentSongState.shuffle
    val repeatState = currentSongState.repeat
    val likeState = currentSongState.likeState


    private val _songs : MutableStateFlow<Response<List<SongsModel>>> = MutableStateFlow(Response.Loading())
    val songs : StateFlow<Response<List<SongsModel>>> = _songs

    val playingArtist by mutableStateOf(currentSongSinger.value)

    init {
        fetchSongs()
    }


    //val songsResponse = (songs.value as Response.Success).data

    // Resolve where we currently are in the queue. The stored index can be stale
    // (e.g. queue swapped out), so match by song id first and fall back to the index.
    private fun currentPositionIn(queueSongs: List<SongsModel>): Int {
        val byId = queueSongs.indexOfFirst { it.id == currentSongId.value }
        if (byId >= 0) return byId
        return currentSongIndex.value.coerceIn(0, queueSongs.size - 1)
    }

    // ── Autoplay radio (Spotify recommendations) ──
    // When the queue nears its end, fetch Spotify-recommended tracks seeded by the original Radio seed (or the recent queue for ordinary autoplay)
    // and append them, so music keeps going like Spotify's autoplay instead of
    // looping the same list. On by default; can be turned off via [autoplayRadioEnabled].
    var autoplayRadioEnabled = true
    @Volatile private var radioLoading = false

    private fun maybeExtendRadio(queueSongs: List<SongsModel>, cur: Int) {
        if (currentSongState.collection.value?.type != "radio") return
        if (!autoplayRadioEnabled || radioLoading) return
        // Only start fetching when we're within one track of the end.
        if (cur < queueSongs.size - 2) return
        val generation = currentSongState.generation
        val seeds = com.music.spotui.data.recommendation.RadioQueuePolicy.seeds(currentSongState.activeRadioSeed, queueSongs)
        if (seeds.isEmpty()) return
        val tasteRevision=com.music.spotui.data.preferences.RadioTaste.revision.value
        radioLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val station = currentSongState.activeRadioSeed
                val recs = if (station != null) repository.provideRadioRecommendations(station,
                    com.music.spotui.data.preferences.RadioSessions.recommendationSeed(com.music.spotui.MyApplication.instance, station))
                else repository.provideRecommendations(seeds)
                val existing = currentSongState.queue.value
                val existingIds = existing.map { it.id }.toSet()
                val candidates = recs.filter { it.id !in existingIds }
                val currentIndex = existing.indexOfFirst { it.id == currentSongId.value }.coerceAtLeast(0)
                val fresh = if (station != null) candidates.take((10 - (existing.size - currentIndex - 1)).coerceAtLeast(0)) else candidates
                if (fresh.isNotEmpty() && generation == currentSongState.generation && tasteRevision==com.music.spotui.data.preferences.RadioTaste.revision.value) {
                    withContext(Dispatchers.Main) {
                        if (generation == currentSongState.generation && tasteRevision==com.music.spotui.data.preferences.RadioTaste.revision.value) currentSongState.updateQueue(currentSongState.queue.value + fresh, preserveSession = true)
                    }
                }
            } finally {
                radioLoading = false
            }
        }
    }

    // Function to play the next song in the album
    fun playNextSongs(queueSongs : List<SongsModel>, context: Context) {
        if (queueSongs.isEmpty()) return
        // A crossfade is already advancing the queue itself — don't double-skip.
        if (SongPlayer.isCrossfadeActive()) return
        SongPlayer.recordManualSkip(context)
        val cur = currentPositionIn(queueSongs)
        // Top up the queue with Spotify recommendations as we approach the end.
        maybeExtendRadio(queueSongs, cur)
        if (cur >= queueSongs.size - 1 && autoplayRadioEnabled && currentSongState.collection.value?.type == "radio") {
            // End of the queue (e.g. a single). Don't loop back to the start —
            // wait for the radio fetch kicked off above to append tracks and
            // continue into them, like Spotify's autoplay.
            continueIntoRadio(queueSongs, context)
            return
        }
        if (cur == queueSongs.lastIndex && currentSongState.collection.value?.type != "radio") {
            val first = currentSongState.nextCollectionLoop() ?: return
            updateSongState(first.coverUri, first.title, first.singer, true, first.id, 0, first.album)
            SongPlayer.playSong(first.url, context)
            if (currentSongState.smartShuffle.value) currentSongState.onSmartShuffleRequested?.invoke()
            return
        }
        val nextIdx = if (cur < queueSongs.size - 1) cur + 1 else 0
        val nextSong = queueSongs[nextIdx]
        updateSongState(
            nextSong.coverUri,
            nextSong.title,
            nextSong.singer,
            true,
            nextSong.id,
            nextIdx,
            nextSong.album
        )
        SongPlayer.playSong(nextSong.url, context)
    }

    /**
     * Opens the artist page for a track: resolves the EXACT artist (name + id)
     * from the track's Spotify id when available, so a display name like "RAM"
     * can't fuzzy-match to "Rammstein". Falls back to the display name.
     */
    // Spotify Canvas (looping video) URL for the current track, or null. Fetched
    // per track; null means no canvas / not resolved yet.
    private val _canvasUrl = mutableStateOf<String?>(null)
    val canvasUrl: State<String?> get() = _canvasUrl
    @Volatile private var canvasRequestId: String = ""

    fun loadCanvas(trackId: String) {
        // Queue edits re-run this for the same track; clearing would rebuild the
        // video player and flash black for a second.
        if (trackId == canvasRequestId && _canvasUrl.value != null) return
        _canvasUrl.value = null
        canvasRequestId = trackId
        if (trackId.isBlank()) return
        viewModelScope.launch {
            val url = withContext(Dispatchers.IO) { repository.provideCanvasUrl(trackId) }
            // Ignore a slow fetch for a track the user already skipped past.
            if (canvasRequestId == trackId) _canvasUrl.value = url
        }
    }

    fun goToArtist(trackId: String, fallbackName: String, navigate: (route: String) -> Unit) {
        viewModelScope.launch {
            val route = withContext(Dispatchers.IO) {
                val artist = if (trackId.isNotBlank())
                    com.metrolist.spotify.Spotify.track(trackId).getOrNull()?.artists?.firstOrNull()
                else null
                artistRoute(
                    artist?.name?.ifBlank { null } ?: fallbackName.substringBefore(",").trim(),
                    artist?.id.orEmpty(),
                )
            }
            navigate(route)
        }
    }

    @Volatile private var awaitingRadioContinue = false

    /** Waits (max ~10s) for the autoplay radio to extend the queue past
     *  [queueSongs] and plays the first appended track; falls back to looping
     *  the queue if no radio tracks arrive. */
    private fun continueIntoRadio(queueSongs: List<SongsModel>, context: Context) {
        if (awaitingRadioContinue) return
        awaitingRadioContinue = true
        val generation = currentSongState.generation
        val endedId = currentSongId.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repeat(40) {
                    if (generation != currentSongState.generation || endedId != currentSongId.value) return@launch
                    val q = currentSongState.queue.value
                    if (q.size > queueSongs.size) {
                        val next = q[queueSongs.size]
                        withContext(Dispatchers.Main) {
                            updateSongState(next.coverUri, next.title, next.singer, true, next.id, queueSongs.size, next.album)
                            SongPlayer.playSong(next.url, context)
                        }
                        return@launch
                    }
                    delay(250L)
                }
                // Radio never arrived — stop cleanly instead of looping the same
                // track again (issue 1: song replayed itself when repeat was off).
                withContext(Dispatchers.Main) {
                    SongPlayer.stop()
                    currentSongState.updatePlayingState(false)
                }
            } finally {
                awaitingRadioContinue = false
            }
        }
    }

    /**
     * Play the track at an absolute [index] in [queueSongs]. Used by the now-playing
     * swipe pager, where the artwork follows the finger and settles on a chosen page.
     * No-op if [index] is already the current track (prevents a feedback replay when
     * the pager is merely snapping to reflect an external track change).
     */
    fun playSongAt(queueSongs: List<SongsModel>, index: Int, context: Context) {
        if (index !in queueSongs.indices) return
        val song = queueSongs[index]
        if (song.id == currentSongId.value) return
        currentSongState.updateQueue(queueSongs, preserveSession = true)
        maybeExtendRadio(queueSongs, index)
        updateSongState(song.coverUri, song.title, song.singer, true, song.id, index, song.album)
        SongPlayer.playSong(song.url, context)
    }

    // Function to play the previous song in the album
    fun playPreviousSong(queueSongs : List<SongsModel>, context: Context) {
        if (queueSongs.isEmpty()) return
        val cur = currentPositionIn(queueSongs)
        val prevIdx = com.music.spotui.audio.QueueOrderPolicy.previousIndex(cur)
        val previousSong = queueSongs[prevIdx]
        updateSongState(previousSong.coverUri, previousSong.title, previousSong.singer, true, previousSong.id, prevIdx, previousSong.album)
        SongPlayer.playSong(previousSong.url, context)
    }

    private fun fetchSongs() = viewModelScope.launch(Dispatchers.IO) {

        repository.provideSongs().collect { songs ->
            _songs.value = songs as Response<List<SongsModel>>

        }
    }
    fun formatDuration(durationMillis: Long): String {
        val safeDuration = com.music.spotui.audio.PlaybackClockPolicy.duration(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safeDuration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safeDuration) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%01d:%02d", minutes, seconds)
    }
    fun updateSongState(coverUri: String, title: String, singer: String, playingState: Boolean, songId : Int, songIndex : Int = 0, album : String = "") {
        currentSongState.updateSongState(coverUri, title, singer, playingState, songId, songIndex, album)
    }

    fun updateShuffleState(shuffleState : Boolean){
        currentSongState.updateShuffleState(shuffleState)
    }
    val smartShuffleState get() = currentSongState.smartShuffle
    fun cycleShuffle() = currentSongState.cycleShuffle()
    fun isSmartSuggestion(id: Int) = currentSongState.isSmartSuggestion(id)
    fun updateRepeatState(repeatState : Boolean){
        currentSongState.updateRepeatState(repeatState)
    }
    fun updateLikeState(likeState : Boolean){
        currentSongState.updateLikeState(likeState)
    }
}
