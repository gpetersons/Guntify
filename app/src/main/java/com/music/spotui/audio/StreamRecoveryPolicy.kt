package com.music.spotui.audio

/** Two retries on the first failed song; skip once, then hold and retry. */
class StreamRecoveryPolicy {
    enum class Action { IGNORE, RETRY, SKIP }
    private var retries = 0
    private var holding = false
    fun failure(failed: String, current: String): Action {
        if (failed.isBlank() || failed != current) return Action.IGNORE
        if (holding) return Action.RETRY
        if (retries < 2) { retries++; return Action.RETRY }
        holding = true
        return Action.SKIP
    }
    fun recovered() { retries = 0; holding = false }
}
