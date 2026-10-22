package com.music.spotui.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.ui.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@HiltViewModel
class RadioCreatorViewModel @Inject constructor(private val repository:AppRepository):ViewModel() {
    private val _results=MutableStateFlow<Response<List<SongsModel>>>(Response.Success(emptyList()))
    val results:StateFlow<Response<List<SongsModel>>> = _results
    private var searchJob:Job?=null
    fun search(text:String) {
        searchJob?.cancel()
        if(text.isBlank()){_results.value=Response.Success(emptyList());return}
        _results.value=Response.Loading()
        searchJob=viewModelScope.launch(Dispatchers.IO) {
            delay(350)
            try { repository.searchSongs(text.trim()).collect { state -> _results.value=if(state is Response.Error)Response.Error("Search unavailable. Check your connection or Spotify sign-in.")else state } } catch(cancelled:CancellationException){throw cancelled} catch(error:Exception){_results.value=Response.Error("Couldn't search Spotify. Try again.")}
        }
    }
    private val _library=MutableStateFlow<Response<List<com.music.spotui.data.entity.LibraryEntry>>>(Response.Loading())
    val library:StateFlow<Response<List<com.music.spotui.data.entity.LibraryEntry>>> = _library
    private val _tracks=MutableStateFlow<Response<List<SongsModel>>>(Response.Success(emptyList()))
    val tracks:StateFlow<Response<List<SongsModel>>> = _tracks
    private var libraryJob:Job?=null
    private var tracksJob:Job?=null
    fun loadLibrary() {
        libraryJob?.cancel()
        _library.value=Response.Loading()
        libraryJob=viewModelScope.launch(Dispatchers.IO) {
            try { repository.provideLibrary().collect { state ->
                _library.value=when(state) {
                    is Response.Success -> Response.Success(state.data.filter{it.isPlaylist && it.spotifyId!="downloaded-offline" && it.spotifyId!="downloads"}.distinctBy{it.spotifyId})
                    is Response.Error -> Response.Error("Couldn't load your Spotify playlists. Try again.")
                    is Response.Loading -> Response.Loading()
                }
            } } catch(cancelled:CancellationException){throw cancelled
            } catch(error:Exception){_library.value=Response.Error("Couldn't load your Spotify playlists. Try again.")}
        }
    }
    fun openPlaylist(id:String) {
        tracksJob?.cancel();_tracks.value=Response.Loading()
        tracksJob=viewModelScope.launch(Dispatchers.IO) {
            try {
                val source=if(id=="liked-songs")repository.provideLikedSongs()else repository.providePlaylistSongs(id)
                source.collect { state -> _tracks.value=if(state is Response.Error)Response.Error("Couldn't load these songs. Try again.")else state }
            } catch(cancelled:CancellationException){throw cancelled
            } catch(error:Exception){_tracks.value=Response.Error("Couldn't load these songs. Try again.")}
        }
    }
    fun closePlaylist(){tracksJob?.cancel();_tracks.value=Response.Success(emptyList())}
    suspend fun create(context:Context,songs:List<SongsModel>,name:String,mix:Boolean):SongsModel=withContext(Dispatchers.IO) {
        val app=context.applicationContext
        val marker=com.music.spotui.data.preferences.RadioSessions.create(app,songs,name,mix)
        if(mix)viewModelScope.launch(Dispatchers.IO) {
            try {
                val path=com.music.spotui.data.api.PlaylistCover.generate(app,marker.spotifyTrackId.replace(':','_'),songs)
                if(path!=null)com.music.spotui.data.preferences.RadioSessions.updateArtwork(app,marker.spotifyTrackId,path)
            } catch(cancelled:CancellationException){throw cancelled
            } catch(error:Exception){android.util.Log.w("RadioCreator","Mix cover deferred",error)}
        }
        marker
    }
}
