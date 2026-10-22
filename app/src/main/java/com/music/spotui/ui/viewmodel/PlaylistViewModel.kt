package com.music.spotui.ui.viewmodel

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.AlbumsModel
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
class PlaylistViewModel @Inject constructor(
    private val repository: AppRepository,
    private val currentSongState: CurrentSongState,
) : ViewModel() {

    val currentSongPlayingState: State<Boolean> get() = currentSongState.playingState
    val currentSongId: State<Int> get() = currentSongState.songId

    private val _songs: MutableStateFlow<Response<List<SongsModel>>> = MutableStateFlow(Response.Loading())
    val songs: StateFlow<Response<List<SongsModel>>> = _songs

    private val _playlist: MutableStateFlow<Response<AlbumsModel>> = MutableStateFlow(Response.Loading())
    val playlist: StateFlow<Response<AlbumsModel>> = _playlist

    val queue: State<List<SongsModel>> get() = currentSongState.queue

    fun updateQueue(songs: List<SongsModel>) { currentSongState.updateQueue(songs); markCollection(songs) }

    val shuffleState get() = currentSongState.shuffle
    fun updateShuffleState(enabled: Boolean) = currentSongState.updateShuffleState(enabled)
    fun cycleShuffle() = currentSongState.cycleShuffle()
    val smartShuffleActive: Boolean get() = shuffleActive && currentSongState.smartShuffle.value

    fun startShuffled(songs: List<SongsModel>): SongsModel? { val first = currentSongState.startShuffled(songs); markCollection(songs); return first }

    /** Pause/resume global playback (the header play button stays visible while playing). */
    fun setPlaying(playing: Boolean) {
        if (playing) com.music.spotui.di.SongPlayer.play() else com.music.spotui.di.SongPlayer.pause()
        currentSongState.updatePlayingState(playing)
    }

    fun updateSongState(coverUri: String, title: String, singer: String, playingState: Boolean, songId: Int, songIndex: Int = 0, album: String = "") {
        currentSongState.updateSongState(coverUri, title, singer, playingState, songId, songIndex, album)
    }

    val collectionActive: Boolean get() = com.music.spotui.audio.CollectionPlaybackPolicy.active("playlist:$playlistKey", currentSongState.collection.value?.key, currentSongId.value, currentSongState.queue.value.map { it.id })
    val shuffleActive: Boolean get() = shuffleState.value && collectionActive
    private fun markCollection(songs: List<SongsModel>) {
        val id = playlistKey ?: return
        com.music.spotui.data.preferences.HomeHiddenItems.beginReplay(com.music.spotui.MyApplication.instance, "playlist", id)
        val meta = (_playlist.value as? Response.Success)?.data
        currentSongState.collection.value = com.music.spotui.data.entity.PlaybackCollection(
            "playlist:$id", "playlist", id, meta?.name ?: "Playlist", meta?.coverUri ?: songs.firstOrNull()?.coverUri.orEmpty(), "Playlist")
    }

    val likeState = currentSongState.likeState

    fun updateLikeState(likeState: Boolean) {
        currentSongState.updateLikeState(likeState)
    }

    private var playlistKey: String? = null

    fun loadPlaylist(playlistId: String) {
        if (playlistKey == playlistId) return
        playlistKey = playlistId
        viewModelScope.launch(Dispatchers.IO) {
            repository.providePlaylist(playlistId).collect { _playlist.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.providePlaylistSongs(playlistId).collect { _songs.value = it }
        }
    }
}
