package com.music.spotui.audio

/** Shared audible-time requirement for a local play to influence listening features. */
object ListeningQualification {
    const val MIN_AUDIBLE_MS = 30_000L
    fun qualifies(audibleMs: Long): Boolean = audibleMs >= MIN_AUDIBLE_MS
}
