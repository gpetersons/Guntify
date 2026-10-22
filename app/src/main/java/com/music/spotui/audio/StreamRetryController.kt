package com.music.spotui.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One non-overlapping retry per failed playback visit. All calls use the player thread. */
class StreamRetryController(
    private val scope: CoroutineScope,
    private val current: () -> Ticket?,
    private val playing: () -> Boolean,
    private val retry: (Ticket) -> Unit,
    private val skip: () -> Boolean,
    private val onWaiting: () -> Unit = {},
) {
    data class Ticket(val query: String, val visit: Long)
    private val policy = StreamRecoveryPolicy()
    private var pending: Job? = null
    private var lastFailed: Ticket? = null
    private var automated = false
    var isRecovering: Boolean = false
        private set

    /** User Play/Pause/Stop/new selection cancels recovery; our own retry/skip does not. */
    fun requestChanged() { if (!automated) recovered() }
    fun recovered() {
        pending?.cancel(); pending = null
        lastFailed = null; isRecovering = false; policy.recovered()
    }
    private inline fun automatically(action: () -> Unit) {
        automated = true
        try { action() } finally { automated = false }
    }
    fun failed(ticket: Ticket) {
        if (ticket.query.isBlank() || current() != ticket || playing() || lastFailed == ticket) return
        lastFailed = ticket
        pending?.cancel(); pending = null
        isRecovering = true
        val action = policy.failure(ticket.query, current()?.query.orEmpty())
        if (action == StreamRecoveryPolicy.Action.IGNORE) return
        if (action == StreamRecoveryPolicy.Action.SKIP) {
            var advanced = false
            automatically { advanced = skip() }
            if (advanced) return
            // No next track: retain this song rather than wrapping to a failed one.
        }
        onWaiting()
        pending = scope.launch {
            delay(RETRY_DELAY_MS)
            pending = null
            if (current() != ticket || playing()) { recovered(); return@launch }
            automatically { retry(ticket) }
        }
    }
    companion object { const val RETRY_DELAY_MS = 10_000L }
}
