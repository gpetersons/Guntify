package com.music.spotui.ui.components

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Brief visible recovery status; background playback does not launch UI. */
object PlaybackFeedback {
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages = events.asSharedFlow()
    fun retrying() { events.tryEmit("Couldn't load audio. Retrying in 10 seconds.") }
    fun skipping() { events.tryEmit("Couldn't load this song. Trying the next song.") }
}
