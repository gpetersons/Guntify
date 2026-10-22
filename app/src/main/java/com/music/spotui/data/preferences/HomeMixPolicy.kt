package com.music.spotui.data.preferences

/** Maturity uses qualifying local listens (at least 30 seconds) within the user's selected history window. */
object HomeMixPolicy {
    fun localFraction(plays: Int): Double = when {
        plays < 3 -> 0.0
        plays < 20 -> 0.2
        plays < 60 -> 0.4
        plays < 150 -> 0.6
        plays < 300 -> 0.8
        else -> 1.0
    }

    /** The slider is the eventual local share; maturity controls the transition to it. */
    fun fraction(plays: Int, strength: Int): Double = localFraction(plays) * strength.coerceIn(0, 100) / 100.0

    fun replacementCount(total: Int, fraction: Double, available: Int): Int =
        (total.coerceAtLeast(0) * fraction.coerceIn(0.0, 1.0)).toInt().coerceAtMost(available.coerceAtLeast(0))
}
