package com.music.spotui.audio

object NextTrackPreparationPolicy {
    fun shouldPrepareEarly(audibleMs: Long, playing: Boolean, repeatOne: Boolean): Boolean =
        playing && !repeatOne && audibleMs >= 2_000L

    fun shouldPrepare(durationMs: Long, positionMs: Long, playing: Boolean, repeatOne: Boolean, crossfadeMs: Int): Boolean {
        if (!playing || repeatOne || durationMs <= 0 || positionMs < 0 || positionMs >= durationMs) return false
        // Long crossfades need the same lead time before their blend begins.
        val window = maxOf(10_000L, crossfadeMs.coerceAtLeast(0).toLong() + 5_000L)
        return durationMs - positionMs <= window
    }
    fun stillNext(expected: String, actual: String?, sameSession: Boolean): Boolean = sameSession && expected == actual
}
