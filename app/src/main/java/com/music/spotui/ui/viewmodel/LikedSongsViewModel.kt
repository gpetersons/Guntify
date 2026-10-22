package com.music.spotui.ui.viewmodel

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.CurrentSongState
import com.music.spotui.ui.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikedSongsViewModel @Inject constructor(
    private val repository: AppRepository,
    private val currentSongState: CurrentSongState,
) : ViewModel() {

    private val _songs = MutableStateFlow<Response<List<SongsModel>>>(Response.Loading())
    val songs: StateFlow<Response<List<SongsModel>>> = _songs

    val currentSongPlayingState: State<Boolean> get() = currentSongState.playingState
    val currentSongId: State<Int> get() = currentSongState.songId

    val queue: State<List<SongsModel>> get() = currentSongState.queue
    fun updateQueue(songs: List<SongsModel>) { currentSongState.updateQueue(songs); markCollection(songs) }

    val shuffleState get() = currentSongState.shuffle
    fun updateShuffleState(enabled: Boolean) = currentSongState.updateShuffleState(enabled)
    fun cycleShuffle() = currentSongState.cycleShuffle()
    val smartShuffleActive: Boolean get() = shuffleActive && currentSongState.smartShuffle.value

    fun startShuffled(songs: List<SongsModel>): SongsModel? { val first = currentSongState.startShuffled(songs); markCollection(songs); return first }

    val collectionActive: Boolean get() = com.music.spotui.audio.CollectionPlaybackPolicy.active("playlist:liked-songs", currentSongState.collection.value?.key, currentSongId.value, currentSongState.queue.value.map { it.id })
    val shuffleActive: Boolean get() = shuffleState.value && collectionActive
    private fun markCollection(songs: List<SongsModel>) {
        com.music.spotui.data.preferences.HomeHiddenItems.beginReplay(com.music.spotui.MyApplication.instance, "playlist", "liked-songs")
        currentSongState.collection.value = com.music.spotui.data.entity.PlaybackCollection(
            "playlist:liked-songs", "playlist", "liked-songs", "Liked Songs", songs.firstOrNull()?.coverUri.orEmpty(), "Playlist")
    }
    /** Pause/resume global playback (the header play button stays visible while playing). */
    fun setPlaying(playing: Boolean) {
        if (playing) com.music.spotui.di.SongPlayer.play() else com.music.spotui.di.SongPlayer.pause()
        currentSongState.updatePlayingState(playing)
    }

    fun updateSongState(coverUri: String, title: String, singer: String, playingState: Boolean, songId: Int, songIndex: Int = 0, album: String = "") {
        currentSongState.updateSongState(coverUri, title, singer, playingState, songId, songIndex, album)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.provideLikedSongs().collect { _songs.value = it }
        }
    }

    /** Drops an unliked song from the displayed list without a refetch. */
    fun removeLocally(songId: Int) {
        val current = _songs.value
        if (current is Response.Success) {
            _songs.value = Response.Success(current.data.filterNot { it.id == songId })
        }
    }
}
