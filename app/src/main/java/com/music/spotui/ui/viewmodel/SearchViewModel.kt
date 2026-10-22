package com.music.spotui.ui.viewmodel

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.SearchResults
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.CurrentSongState
import com.music.spotui.ui.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val repository: AppRepository, private val currentSongState: CurrentSongState) : ViewModel() {

    private val _songs : MutableStateFlow<Response<List<SongsModel>>> = MutableStateFlow(Response.Loading())
    val songs : StateFlow<Response<List<SongsModel>>> = _songs

    private val _results : MutableStateFlow<Response<SearchResults>> = MutableStateFlow(Response.Success(SearchResults()))
    val results : StateFlow<Response<SearchResults>> = _results

    val likeState = currentSongState.likeState

    val currentSongId: State<Int> get() = currentSongState.songId


    fun updateLikeState(likeState : Boolean){
        currentSongState.updateLikeState(likeState)
    }

    private var searchJob: Job? = null
    private var pageJob: Job? = null
    private var activeQuery = ""
    private var searchVersion = 0L
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore
    private val _pageError = MutableStateFlow("")
    val pageError: StateFlow<String> = _pageError

    init {
        // Start empty — results come from live searches, not the rate-limited
        // personalized top-tracks feed (which returned 429 and blanked the screen).
        _songs.value = Response.Success(emptyList())
    }

    fun updateQueue(songs: List<SongsModel>) = currentSongState.updateQueue(songs)

    /** Search selection is ordinary playback; Radio remains an explicit menu action. */
    fun startSong(song: SongsModel) {
        currentSongState.updateQueue(listOf(song))
    }

    fun updateSongState(coverUri: String, title: String, singer: String, playingState: Boolean, songId : Int, songIndex : Int = 0, album : String = "") {
        currentSongState.updateSongState(coverUri, title, singer, playingState, songId, songIndex, album)
    }

    fun search(query: String) {
        searchJob?.cancel()
        pageJob?.cancel()
        pageJob = null
        activeQuery = query.trim()
        val version = ++searchVersion
        _loadingMore.value = false
        _pageError.value = ""
        _results.value = Response.Loading()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(150) // short debounce for snappy real-time results
            repository.searchEverything(query).collect { result ->
                if (searchVersion != version) return@collect
                if (pageJob != null) return@collect
                _results.value = result
                // Keep the legacy songs flow in sync for any remaining consumers.
                _songs.value = when (result) {
                    is Response.Success -> Response.Success(result.data.songs)
                    is Response.Error -> Response.Error(result.error)
                    is Response.Loading -> Response.Loading()
                }
            }
        }
    }
    fun loadMore() {
        if (_loadingMore.value) return
        val previous = (_results.value as? Response.Success)?.data ?: return
        val offset = previous.nextOffset ?: return
        val query = activeQuery
        val version = searchVersion
        _loadingMore.value = true
        _pageError.value = ""
        pageJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.searchEverything(query, offset).collect { response ->
                    if (searchVersion != version) return@collect
                    when (response) {
                        is Response.Success -> {
                            val merged = com.music.spotui.data.recommendation.SearchPagePolicy.append(previous, response.data)
                            _results.value = Response.Success(merged)
                            _songs.value = Response.Success(merged.songs)
                        }
                        is Response.Error -> _pageError.value = response.error
                        is Response.Loading -> Unit
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (searchVersion == version) _pageError.value = error.message ?: "Could not load more results."
            } finally {
                if (searchVersion == version) _loadingMore.value = false
            }
        }
    }

}