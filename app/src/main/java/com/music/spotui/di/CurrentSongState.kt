package com.music.spotui.di

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.music.spotui.data.entity.SongsModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentSongState @Inject constructor() {
    @Volatile var activeRadioSeed: String? = null
        private set
    val collection = mutableStateOf<com.music.spotui.data.entity.PlaybackCollection?>(null)
    @Volatile private var queueGeneration = 0L
    val generation: Long get() = queueGeneration
    fun activateRadio(seedId: String) { activeRadioSeed = seedId.takeIf { it.isNotBlank() } }
    private val _title: MutableState<String> = mutableStateOf("")
    val title: State<String> get() = _title

    private val _album: MutableState<String> = mutableStateOf("")
    val album : State<String> get() = _album

    private val _singer: MutableState<String> = mutableStateOf("")
    val singer: State<String> get() = _singer

    private val _coverUri: MutableState<String> = mutableStateOf("")
    val coverUri: State<String> get() = _coverUri

    private val _playingState: MutableState<Boolean> = mutableStateOf(false)
    val playingState: State<Boolean> get() = _playingState

    private val _songIndex: MutableState<Int> = mutableStateOf(0)
    val songIndex : State<Int> get() = _songIndex

    private val _songId: MutableState<Int> = mutableStateOf(0)
    val songId : State<Int> get() = _songId

    // The actual list the user is playing (album tracks, search results, liked
    // songs…). Next/previous operate on THIS, not on a re-derived global feed.
    private val _queue: MutableState<List<SongsModel>> = mutableStateOf(emptyList())
    val queue: State<List<SongsModel>> get() = _queue
    private val manualQueuedIds = linkedSetOf<Int>()
    private fun pendingManualSongs(): List<SongsModel> {
        val q = _queue.value
        val index = q.indexOfFirst { it.id == _songId.value }
        return q.drop(index + 1).filter { it.id in manualQueuedIds }
    }
    fun addToQueue(song: SongsModel): Boolean {
        val next = com.music.spotui.audio.ManualQueuePolicy.add(_queue.value, _songId.value, manualQueuedIds, song) ?: return false
        manualQueuedIds.add(song.id)
        SongPlayer.invalidateUpcomingPreparation(); queueGeneration++
        updateQueue(next, preserveSession = true)
        return true
    }
    val smartShuffle = mutableStateOf(false)
    private var smartBaseQueue: List<SongsModel>? = null
    private val smartAddedIds = mutableSetOf<Int>()
    var onSmartShuffleRequested: (() -> Unit)? = null
    fun cycleShuffle() {
        if (collection.value?.type == "radio" || collection.value?.key == "local:downloads") { updateShuffleState(!shuffle.value); return }
        when {
            smartShuffle.value -> {
                smartShuffle.value = false
                val end = com.music.spotui.audio.QueueOrderPolicy.playedIndex(_queue.value, _songId.value, playedThroughId)
                updateQueue(_queue.value.take(end + 1) + _queue.value.drop(end + 1).filter { it.id !in smartAddedIds }, preserveSession = true)
                updateShuffleState(false)
            }
            shuffle.value -> {
                smartBaseQueue = unshuffledQueue ?: _queue.value.filter { it.id !in smartAddedIds }
                smartShuffle.value = true
                saveQueue()
                onSmartShuffleRequested?.invoke()
            }
            else -> updateShuffleState(true)
        }
    }
    fun isSmartSuggestion(id: Int) = id in smartAddedIds
    fun addSmartSuggestions(suggestions: List<SongsModel>) {
        if (!smartShuffle.value || collection.value?.type == "radio") return
        val q = _queue.value
        val end = com.music.spotui.audio.QueueOrderPolicy.playedIndex(q, _songId.value, playedThroughId)
        val upcoming = q.drop(end + 1)
        if (upcoming.count { it.id in smartAddedIds } >= 3) return
        val (mixed, added) = com.music.spotui.audio.SmartShufflePolicy.inject(upcoming, suggestions, q.map { it.id }.toSet())
        if (added.isEmpty()) return
        smartAddedIds.addAll(added)
        SongPlayer.invalidateUpcomingPreparation(); queueGeneration++
        updateQueue(com.music.spotui.audio.ManualQueuePolicy.protect(q.take(end + 1) + mixed, _songId.value, pendingManualSongs()), preserveSession = true)
    }
    fun nextCollectionLoop(): SongsModel? {
        val base = smartBaseQueue ?: _queue.value.filter { it.id !in smartAddedIds }
        if (base.isEmpty()) return null
        val next = if (shuffle.value) base.shuffled() else base
        playedThroughId = null; smartAddedIds.clear(); queueUndo.value = null
        SongPlayer.invalidateUpcomingPreparation(); queueGeneration++
        updateQueue(next, preserveSession = true)
        return next.first()
    }
    data class QueueUndo(val songs: List<SongsModel>, val original: List<SongsModel>?, val songId: Int, val generation: Long, val label: String, val manualQueued: Set<Int> = emptySet())
    val queueUndo = mutableStateOf<QueueUndo?>(null)
    fun canUndoQueue(): Boolean = queueUndo.value?.let { it.songId == _songId.value && it.generation == queueGeneration } == true
    fun editQueue(songs: List<SongsModel>, label: String) {
        if (songs.none { it.id == _songId.value }) return
        val previous = _queue.value
        val original = unshuffledQueue
        val previousManual = manualQueuedIds.toSet()
        SongPlayer.invalidateUpcomingPreparation()
        queueGeneration++
        updateQueue(songs, preserveSession = true)
        _songIndex.value = songs.indexOfFirst { it.id == _songId.value }
        queueUndo.value = QueueUndo(previous, original, _songId.value, queueGeneration, label, previousManual)
    }
    fun undoQueueEdit(): Boolean {
        val snapshot = queueUndo.value ?: return false
        if (!canUndoQueue()) { queueUndo.value = null; return false }
        SongPlayer.invalidateUpcomingPreparation()
        queueGeneration++
        manualQueuedIds.clear(); manualQueuedIds.addAll(snapshot.manualQueued)
        updateQueue(snapshot.songs, preserveSession = true)
        unshuffledQueue = snapshot.original
        _songIndex.value = snapshot.songs.indexOfFirst { it.id == _songId.value }
        queueUndo.value = null
        saveQueue()
        return true
    }
    var playedThroughId: Int? = null
        private set

    fun updateQueue(queue: List<SongsModel>, preserveSession: Boolean = false) {
        if (!preserveSession) {
            manualQueuedIds.clear()
            smartShuffle.value = false; smartBaseQueue = null; smartAddedIds.clear()
            queueUndo.value = null
            playedThroughId = null
            activeRadioSeed = null
            collection.value = null
            queueGeneration++
            unshuffledQueue = null
            shuffle.value = false
        }
        // Playback finds the current track by id, and the queue screen keys rows by
        // id, so a track listed twice (e.g. in a playlist) is queued once.
        val songs = queue.distinctBy { it.id }
        // A queue replacement unrelated to the active radio ends its tracking.
        if (activeRadioSeed != null && _queue.value.isNotEmpty() &&
            songs.none { next -> _queue.value.any { old -> old.id == next.id } }) {
            activeRadioSeed = null
        }
        _queue.value = songs
        manualQueuedIds.retainAll(songs.map { it.id }.toSet())
        if (preserveSession) saveQueue()
        // Seed the lossless resolver: map each track's play query → its Spotify id so
        // SongPlayer can resolve a FLAC stream from a play site that only has the query.
        SongPlayer.registerLossless(songs.map { it.url to it.spotifyTrackId })
        SongPlayer.registerAlternativeKeys(songs.map {
            it.url to com.music.spotui.data.preferences.alternativeStreamKey(it)
        })
        // Seed explicit flags so the YouTube fallback picks the matching edit.
        SongPlayer.registerExplicit(songs.map { it.url to it.explicit })
        // Seed durations so the YouTube match can reject same-title wrong-artist
        // songs (they almost always have a different length).
        SongPlayer.registerDuration(songs.mapNotNull { s -> if (s.durationMs > 0) s.url to s.durationMs else null })
        // Seed exact title/artist/album metadata so the YouTube fallback can score
        // candidates against the actual Spotify track instead of only the search
        // query string.
        SongPlayer.registerMetadata(songs.map {
            it.url to SongPlayer.TrackMatchMetadata(
                title = it.title,
                artist = it.singer,
                album = it.album,
            )
        })
        // Seed the lyrics resolver with track ids so it can use Spotify's own
        // color-lyrics endpoint (exact synced lyrics) instead of LRCLIB matching.
        com.music.spotui.data.api.LyricsApi.registerTracks(songs)
    }

    fun replaceRadioQueue(proposed: List<SongsModel>, undoable: Boolean = false, preserveManual: Boolean = true) {
        val songs = if (preserveManual) com.music.spotui.audio.ManualQueuePolicy.protect(proposed, _songId.value, pendingManualSongs()) else proposed
        val currentIndex = songs.indexOfFirst { it.id == songId.value }
        if (collection.value?.type != "radio" || currentIndex < 0) return
        val previous = _queue.value
        val original = unshuffledQueue
        val previousManual = manualQueuedIds.toSet()
        SongPlayer.invalidateUpcomingPreparation()
        queueGeneration++
        updateQueue(songs, preserveSession = true)
        unshuffledQueue = if (shuffle.value) songs else null
        _songIndex.value = currentIndex
        if (undoable) queueUndo.value = QueueUndo(previous, original, _songId.value, queueGeneration, "Radio refreshed", previousManual)
        saveQueue()
    }

    val shuffle = mutableStateOf(false)
    val repeat = mutableStateOf(false)
    val likeState = mutableStateOf(false)

    // Original queue order, kept while shuffle is on so turning it off restores
    // the list instead of leaving it permanently scrambled.
    private var unshuffledQueue: List<SongsModel>? = null

    /**
     * Toggling shuffle reorders the queue ONCE: the current track stays where it
     * is and the rest follow in random order. (Skipping used to re-shuffle the
     * whole list on every tap, which could repeat or skip songs.)
     */
    fun updateShuffleState(newShuffleState: Boolean) {
        if (newShuffleState == shuffle.value) return
        val pending = pendingManualSongs()
        shuffle.value = newShuffleState
        SongPlayer.invalidateUpcomingPreparation()
        queueGeneration++
        val q = _queue.value
        if (newShuffleState) {
            unshuffledQueue = q
            _queue.value = com.music.spotui.audio.QueueOrderPolicy.shuffled(q, _songId.value, playedThroughId)
        } else {
            val original = unshuffledQueue
            unshuffledQueue = null
            // Restore only if we're still inside that queue (it may have been
            // replaced by another list while shuffled). Keep tracks appended in
            // the meantime (queue edits, autoplay radio).
            if (original != null && original.any { it.id == _songId.value }) {
                val restored = com.music.spotui.audio.QueueOrderPolicy.unshuffled(q, _songId.value, playedThroughId, original)
                _queue.value = restored
                val idx = restored.indexOfFirst { it.id == _songId.value }
                if (idx >= 0) _songIndex.value = idx
            }
        }
        _queue.value = com.music.spotui.audio.ManualQueuePolicy.protect(_queue.value, _songId.value, pending)
        _queue.value.indexOfFirst { it.id == _songId.value }.takeIf { it >= 0 }?.let { _songIndex.value = it }
        saveQueue()
    }

    /**
     * Starts shuffled playback of a full list (the shuffle button on
     * playlist/album/liked screens): the queue becomes a shuffled copy, shuffle
     * turns on, and the caller plays the returned first track.
     */
    fun startShuffled(songs: List<SongsModel>): SongsModel? {
        if (songs.isEmpty()) return null
        updateQueue(com.music.spotui.data.recommendation.RadioQueuePolicy.shuffled(songs))
        unshuffledQueue = songs
        shuffle.value = true
        return _queue.value.firstOrNull()
    }
    fun restore(snapshot: com.music.spotui.data.preferences.PlaybackQueueSnapshot) {
        updateQueue(snapshot.songs)
        collection.value = snapshot.collection
        activeRadioSeed = snapshot.radioSeed
        shuffle.value = snapshot.shuffled
        unshuffledQueue = snapshot.original
        smartShuffle.value = snapshot.smartShuffle
        smartBaseQueue = snapshot.smartBase
        smartAddedIds.addAll(snapshot.smartAdded)
        val cachedManual = snapshot.radioSeed?.let { com.music.spotui.data.preferences.RadioMemory.queue(com.music.spotui.MyApplication.instance, it)?.manualQueued }.orEmpty()
        manualQueuedIds.addAll((snapshot.manualQueued + cachedManual).filter { id -> snapshot.songs.any { it.id == id } })
        playedThroughId = snapshot.playedThroughId ?: com.music.spotui.data.preferences.loadLastPlayback(com.music.spotui.MyApplication.instance)
            ?.first?.id?.takeIf { id -> snapshot.songs.any { it.id == id } }
        snapshot.radioSeed?.let { station ->
            val context=com.music.spotui.MyApplication.instance
            val current=com.music.spotui.data.preferences.loadLastPlayback(context)?.first?.id
                ?.takeIf{id->snapshot.songs.any{it.id==id}} ?: snapshot.songs.firstOrNull()?.id
            if(current!=null)com.music.spotui.data.preferences.RadioMemory.saveQueue(context,station,snapshot.songs,current,snapshot.shuffled,manualQueued = manualQueuedIds.toSet())
        }
    }
    private fun saveQueue() {
        activeRadioSeed?.let { station ->
            com.music.spotui.data.preferences.RadioMemory.saveQueue(com.music.spotui.MyApplication.instance,station,_queue.value,_songId.value,shuffle.value,manualQueued = manualQueuedIds.toSet())
        }
        com.music.spotui.data.preferences.savePlaybackQueue(com.music.spotui.MyApplication.instance,
            com.music.spotui.data.preferences.PlaybackQueueSnapshot(_queue.value, collection.value, activeRadioSeed, shuffle.value, unshuffledQueue, playedThroughId,
                smartShuffle.value, smartBaseQueue, smartAddedIds.toSet(), manualQueuedIds.toSet()))
    }
    fun updateRepeatState(newRepeatState : Boolean){
        repeat.value = newRepeatState
    }

    /** Sync the play/pause state without touching the rest of the now-playing
     *  metadata — used to reflect the web player's real state (e.g. after the
     *  system notification's pause button) back into the in-app UI. */
    fun updatePlayingState(playing: Boolean) {
        _playingState.value = playing
    }

    fun updateLikeState(newLikeState : Boolean){
        likeState.value = newLikeState
    }

    fun updateSongState(coverUri: String, title: String, singer: String, playingState: Boolean, songId : Int, songIndex : Int, album : String) {
        if (songId != _songId.value) queueUndo.value = null
        manualQueuedIds.remove(songId)
        if (playingState) {
            val index = _queue.value.indexOfFirst { it.id == songId }
            val previousEnd = _queue.value.indexOfFirst { it.id == playedThroughId }
            if (index >= 0 && index >= previousEnd) playedThroughId = songId
        }
        _coverUri.value = coverUri
        _title.value = title
        _album.value = album
        _singer.value = singer
        _playingState.value = playingState
        _songIndex.value = songIndex
        _songId.value = songId
        if (playingState) {
            activeRadioSeed?.let { seedId ->
                _queue.value.firstOrNull { it.id == songId }?.let { track ->
                    com.music.spotui.data.preferences.RadioSessions.saveLastTrack(
                        com.music.spotui.MyApplication.instance, seedId, track)
                }
            }
        }
        if (playingState) saveQueue()
        // Feed the system media notification (MediaSession) with the current track.
        SongPlayer.setNowPlayingMeta(title, singer, coverUri)
        // Persist the current track so a fresh launch can restore the session.
        if (playingState && title.isNotBlank()) {
            _queue.value.firstOrNull { it.id == songId }?.let { track ->
                com.music.spotui.data.preferences.saveLastPlayback(
                    com.music.spotui.MyApplication.instance, track)
            }
        }
        // Keep the separate History screen's playback-start log. Home ignores these until the audio clock qualifies them.
        if (playingState && title.isNotBlank()) {
            _queue.value.firstOrNull { it.id == songId }?.let { track ->
                com.music.spotui.data.preferences.addListeningHistory(com.music.spotui.MyApplication.instance,
                    com.music.spotui.data.preferences.HistoryEntry(System.currentTimeMillis(), track.id, track.title,
                        track.singer, track.album, track.coverUri, track.url, track.spotifyTrackId))
            }
        }
        // Warm the lyrics cache in the background so they're already loaded by the
        // time the user opens the player / scrolls to the lyrics card.
        if (playingState && title.isNotBlank()) {
            // With the length, so a fallback match is checked against it before caching.
            val durationSec = (_queue.value.firstOrNull { it.id == songId }?.durationMs ?: 0) / 1000
            com.music.spotui.data.api.LyricsApi.prefetch(title, singer, album, durationSec)

        }
    }
}
