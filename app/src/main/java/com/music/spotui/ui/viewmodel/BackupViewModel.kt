package com.music.spotui.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.backup.UserBackup
import com.music.spotui.data.preferences.*
import com.music.spotui.di.CurrentSongState
import com.music.spotui.di.SongPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

object BackupSignals { val revision = MutableStateFlow(0L) }

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val current: CurrentSongState,
) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    fun saveBackup(uri: Uri, audio: Boolean) {
        if (!start("Creating backup…")) return
        viewModelScope.launch {
            try {
        check(com.music.spotui.di.Downloads.downloadingSnapshot().isEmpty()) { "Wait for current downloads to finish, then export again." }
        saveLastPosition(context, SongPlayer.getCurrentPosition())
        withContext(Dispatchers.IO) { UserBackup.saveBackup(context, uri, audio) }
        _message.value = "Backup saved. Keep it outside the app before reinstalling."
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _message.value = "Backup failed. " + error.message.orEmpty()
            } finally { _busy.value = false }
        }
    }
    fun restore(uri: Uri) {
        if (!start("Checking and restoring backup…")) return
        viewModelScope.launch {
            try {
        SongPlayer.pause(); SongPlayer.stop()
        withContext(Dispatchers.IO) { UserBackup.restore(context, uri) }
        invalidateDownloadCache()
        com.music.spotui.ui.theme.AppearanceSettings.reload(context)
        com.music.spotui.audio.PlaybackEqualizer.reload(context)
        val playback = loadLastPlayback(context)
        val snapshot = loadPlaybackQueue(context)
        if (playback != null) {
            val (song, position) = playback
            if (snapshot != null && snapshot.songs.any { it.id == song.id }) current.restore(snapshot) else current.updateQueue(listOf(song))
            current.updateSongState(song.coverUri, song.title, song.singer, false, song.id, current.queue.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0), song.album)
            SongPlayer.setRestorePoint(song.url, position)
        } else {
            current.updateQueue(emptyList()); current.updateSongState("", "", "", false, 0, 0, "")
        }
        savePlaybackActive(context, false)
        ListeningHistorySignals.revision.value++
        BackupSignals.revision.value++
        _message.value = "Backup restored. Sign in again if needed; reselect external local music folders."
        android.widget.Toast.makeText(context, "Backup restored", android.widget.Toast.LENGTH_LONG).show()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _message.value = "Backup failed. " + error.message.orEmpty()
            } finally { _busy.value = false }
        }
    }
    private fun start(progress: String): Boolean {
        if (_busy.value) return false
        _busy.value = true
        _message.value = progress
        return true
    }
}
