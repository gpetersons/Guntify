package com.music.spotui.audio

/** Failed background preparation yields to playback and backs off after completion. */
class PreparationBackoff {
    private var failures = 0
    private var nextAt = 0L
    @Synchronized fun reset() { failures = 0; nextAt = 0 }
    @Synchronized fun ready(now: Long): Boolean = now >= nextAt
    @Synchronized fun failed(now: Long) {
        val delay = longArrayOf(10_000, 20_000, 40_000, 60_000)[failures.coerceAtMost(3)]
        failures = (failures + 1).coerceAtMost(4)
        nextAt = now + delay
    }
}
