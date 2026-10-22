package com.music.spotui.audio

/** Audible elapsed time for the current visit; seeks, pauses and buffering add nothing. */
class PreparationClock {
    private var identity = ""
    private var elapsed = 0L
    private var lastTick = 0L
    private var wasAudible = false
    fun tick(key: String, audible: Boolean, nowMs: Long): Long {
        if (key != identity) {
            identity = key; elapsed = 0L; lastTick = nowMs; wasAudible = false
        }
        if (audible && wasAudible) elapsed += (nowMs - lastTick).coerceIn(0L, 1_000L)
        lastTick = nowMs
        wasAudible = audible
        return elapsed
    }
}
