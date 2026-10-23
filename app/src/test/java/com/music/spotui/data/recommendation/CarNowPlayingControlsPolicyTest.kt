package com.music.spotui.data.recommendation

import com.music.spotui.ui.notification.CarNowPlayingControlsPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CarNowPlayingControlsPolicyTest {
    private fun state(
        shuffle: Boolean = false,
        smart: Boolean = false,
        repeat: Boolean = false,
        liked: Boolean = false,
    ) = CarNowPlayingControlsPolicy.State(
        hasTrack = true, hasMultipleTracks = true,
        shuffleActive = shuffle,
        smartShuffleActive = smart,
        repeatActive = repeat, liked = liked,
    )

    @Test fun radioActionAlwaysUsesNeutralLabel() {
        assertEquals("Play Radio", CarNowPlayingControlsPolicy.radioLabel(state()))
        assertEquals("Play Radio", CarNowPlayingControlsPolicy.radioLabel(state(shuffle = true)))
    }

    @Test fun likedSongsActionReflectsSavedStatus() {
        assertEquals("Add to Liked Songs", CarNowPlayingControlsPolicy.favoriteLabel(state()))
        assertEquals("Remove from Liked Songs", CarNowPlayingControlsPolicy.favoriteLabel(state(liked = true)))
    }

    @Test fun shuffleAndRepeatLabelsReflectPlaybackState() {
        assertEquals("Shuffle", CarNowPlayingControlsPolicy.shuffleLabel(state()))
        assertEquals("Shuffle on", CarNowPlayingControlsPolicy.shuffleLabel(state(shuffle = true)))
        assertEquals("Smart Shuffle", CarNowPlayingControlsPolicy.shuffleLabel(state(shuffle = true, smart = true)))
        assertEquals("Repeat", CarNowPlayingControlsPolicy.repeatLabel(state()))
        assertEquals("Repeat on", CarNowPlayingControlsPolicy.repeatLabel(state(repeat = true)))
    }

    @Test fun singleSongChangeDoesNotRequireLayoutRefreshWhenActionStatesMatch() {
        assertEquals(state(), state())
        assertNotEquals(state(), state(liked = true))
        assertNotEquals(state(), state(repeat = true))
        assertNotEquals(state(shuffle = true), state(shuffle = true, smart = true))
    }
}
