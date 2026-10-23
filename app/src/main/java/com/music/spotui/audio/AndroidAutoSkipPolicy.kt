package com.music.spotui.audio

/** A small debounce affects only manual Android Auto Next, never automatic transitions. */
object AndroidAutoSkipPolicy {
    const val RAPID_NEXT_LOOKUP_DELAY_MS = 180L
    fun lookupDelayMs(manual: Boolean, forward: Boolean): Long =
        if (manual && forward) RAPID_NEXT_LOOKUP_DELAY_MS else 0L
}
