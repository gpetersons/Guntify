package com.music.spotui.audio

/** Counts audible wall-clock time, never seek position, buffering, or time while the app is suspended. */
class ListeningClock {
    private var identity = ""
    private var lastTick = 0L
    private var wasAudible = false
    private var elapsed = 0L
    private var recorded = false
    val audibleMs: Long get() = elapsed
    fun tick(key: String, audible: Boolean, nowMs: Long): Long? {
        if (key != identity) { identity = key; elapsed = 0; recorded = false; wasAudible = false; lastTick = nowMs }
        if (audible && wasAudible) elapsed += (nowMs - lastTick).coerceIn(0L, 1000L)
        lastTick = nowMs
        wasAudible = audible
        if (!recorded && com.music.spotui.audio.ListeningQualification.qualifies(elapsed)) { recorded = true; return elapsed }
        return null
    }
}
object PlaybackClockPolicy {
    fun duration(value: Long): Long = value.takeIf { it in 1L..604_800_000L } ?: 0L
    fun position(value: Long, duration: Long): Long {
        if (value !in 0L..604_800_000L) return 0L
        return if (duration > 0L) value.coerceAtMost(duration) else value
    }
}
