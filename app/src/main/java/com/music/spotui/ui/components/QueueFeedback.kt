package com.music.spotui.ui.components

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** One brief app-level confirmation shared by swipe and menu queue actions. */
object QueueFeedback {
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages = events.asSharedFlow()
    fun added(success: Boolean) { events.tryEmit(if (success) "Added to queue" else "Already in queue") }
}
