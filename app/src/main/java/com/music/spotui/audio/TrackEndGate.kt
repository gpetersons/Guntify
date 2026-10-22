package com.music.spotui.audio

/** Coalesces listener/watchdog completion, but leaves a failed crossfade eligible. */
class TrackEndGate {
    private var notified = false
    fun reset() { notified = false }
    fun take(ended: Boolean, hasMedia: Boolean, crossfading: Boolean): Boolean {
        if (notified || !ended || !hasMedia || crossfading) return false
        notified = true
        return true
    }
}
