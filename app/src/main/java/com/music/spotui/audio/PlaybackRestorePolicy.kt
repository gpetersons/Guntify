package com.music.spotui.audio

import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.PlaybackQueueSnapshot
import com.music.spotui.data.recommendation.EvolvingRadioPolicy

/** Restore a saved queue without changing its collection, station identity or shuffle order. */
object PlaybackRestorePolicy {
    fun snapshot(last: SongsModel, saved: PlaybackQueueSnapshot?): PlaybackQueueSnapshot {
        if (saved == null || saved.songs.none { it.id == last.id }) {
            return PlaybackQueueSnapshot(listOf(last), null, null, false, null)
        }
        if (saved.collection?.type != "radio") return saved
        val window = EvolvingRadioPolicy.window(saved.songs, last.id, saved.playedThroughId ?: last.id)
        val ids = window.map { it.id }.toSet()
        return saved.copy(songs = window, original = saved.original?.filter { it.id in ids })
    }
}
