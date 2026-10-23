package com.music.spotui.ui.notification

import com.music.spotui.data.entity.SongsModel

/** Resolve media-session actions against the current app queue, not stream IDs. */
object CarRadioActionPolicy {
    fun currentTrack(queue: List<SongsModel>, songId: Int): SongsModel? =
        queue.firstOrNull { it.id == songId && it.url.isNotBlank() }
}
