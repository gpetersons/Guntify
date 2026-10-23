package com.music.spotui.ui.notification

/**
 * Pure presentation state for Android Auto custom transport actions.
 *
 * A host can show some of these actions only in the overflow menu. Keeping
 * their labels and icons derived from the phone's existing playback state
 * avoids toggles that disagree between the car and handset.
 */
object CarNowPlayingControlsPolicy {
    data class State(
        val hasTrack: Boolean,
        val hasMultipleTracks: Boolean,
        val radioActive: Boolean,
        val shuffleActive: Boolean,
        val repeatActive: Boolean,
        val liked: Boolean,
    )

    fun radioLabel(state: State) = if (state.radioActive) "Radio playing" else "Play Radio"
    fun favoriteLabel(state: State) =
        if (state.liked) "Remove from Liked Songs" else "Add to Liked Songs"
    fun shuffleLabel(state: State) = if (state.shuffleActive) "Shuffle on" else "Shuffle"
    fun repeatLabel(state: State) = if (state.repeatActive) "Repeat on" else "Repeat"
}
