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
        com.music.spotui.data.backup.BackupRestoreEffects.apply(context, current)
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
