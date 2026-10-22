package com.music.spotui.data.backup

import android.content.Context
import com.music.spotui.data.preferences.*
import com.music.spotui.di.CurrentSongState
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.viewmodel.BackupSignals

/** Shared post-restore updates for native and LAN imports; call on the main thread. */
object BackupRestoreEffects {
    fun apply(context: Context, current: CurrentSongState) {
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
    }
}
