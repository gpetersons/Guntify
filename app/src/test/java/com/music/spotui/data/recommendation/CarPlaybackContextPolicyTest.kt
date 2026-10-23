package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.PlaybackCollection
import com.music.spotui.ui.notification.CarPlaybackContextPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class CarPlaybackContextPolicyTest {
    @Test fun radioShowsStationName() {
        val station = PlaybackCollection("radio:abc","radio","abc","Endless", "")
        assertEquals("Endless radio", CarPlaybackContextPolicy.title(station,"A song"))
        assertEquals("Endless radio", CarPlaybackContextPolicy.title(station.copy(name="Endless radio"),"Other"))
    }

    @Test fun playlistAndAlbumShowSourceName() {
        assertEquals("Daily Mix", CarPlaybackContextPolicy.title(PlaybackCollection("playlist:1","playlist","1","Daily Mix",""), "Track"))
        assertEquals("My Album", CarPlaybackContextPolicy.title(PlaybackCollection("album:1","album","1","My Album",""), "Track"))
    }

    @Test fun likedSongsAndIndividualTracksHaveUsefulTitles() {
        assertEquals("Dziesmas, kas tev patīk", CarPlaybackContextPolicy.title(PlaybackCollection("playlist:liked-songs","playlist","liked-songs","Liked Songs",""), "Track"))
        assertEquals("Endless", CarPlaybackContextPolicy.title(null,"Endless"))
    }
}
