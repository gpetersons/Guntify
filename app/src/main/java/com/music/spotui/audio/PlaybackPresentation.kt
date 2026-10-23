package com.music.spotui.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation of actual player/lookup events; never changes the playback engine. */
object PlaybackPresentation {
    enum class Stage { IDLE, CONNECTING, BUFFERING, PLAYING, PAUSED, RETRYING }
    private val mutable = MutableStateFlow(Stage.IDLE)
    val state = mutable.asStateFlow()
    fun set(stage: Stage) { mutable.value = stage }
    fun label(stage: Stage): String = when(stage) {
        Stage.CONNECTING -> "Connecting…"
        Stage.BUFFERING -> "Buffering…"
        Stage.RETRYING -> "Waiting for audio · retrying…"
        else -> ""
    }
}
