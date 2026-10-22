package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import com.music.spotui.data.preferences.HomeMixPolicy
import org.junit.Assert.*
import org.junit.Test

class SpotifyHomeFallbackTest {
    private fun playlist(id: Int) = HomeItem.Playlist("Playlist $id", "art:$id", "Spotify", "$id")
    @Test fun artistOnlyFirstSectionStillFillsRotationFromSpotify() {
        val feed = HomeFeedModel(sections = listOf(HomeSection("Your favorite artists", listOf(HomeItem.Artist("Artist", "", "artist"))),
            HomeSection("Made for Example User", (1..12).map(::playlist))))
        assertEquals((1..8).map(::playlist), HomeLayoutPolicy.rotation(emptyList(), HomeLayoutPolicy.spotifyCollections(feed)))
    }
    @Test fun shortcutsAndRecentSpotifyCollectionsLeadFallbackWithoutRetitlingFeed() {
        val feed = HomeFeedModel(sections = listOf(HomeSection("More like Eli", listOf(playlist(3))),
            HomeSection("Recently played", listOf(playlist(2))), HomeSection("", listOf(playlist(1)))))
        assertEquals(listOf(playlist(1), playlist(2), playlist(3)), HomeLayoutPolicy.spotifyCollections(feed))
        assertEquals("More like Eli", feed.sections.first().title)
    }
    @Test fun localActivityReplacesFallbackGraduallyAndSpotifyBackfillsRemainingSlots() {
        val spotify = (1..10).map(::playlist)
        assertEquals(listOf(playlist(10), playlist(9)) + (1..6).map(::playlist), HomeLayoutPolicy.rotation(listOf(playlist(10), playlist(9)), spotify))
        assertEquals((10 downTo 3).map(::playlist), HomeLayoutPolicy.rotation((10 downTo 3).map(::playlist), spotify))
    }
    @Test fun duplicatesAndIndividualSongsDoNotOccupyRotationSlots() {
        val track = HomeItem.RecentTrack(SongsModel(1, "Song", "Album", "Artist", "", "query"))
        assertEquals(listOf(playlist(1), playlist(2)), HomeLayoutPolicy.rotation(listOf(playlist(1), track), listOf(playlist(1), playlist(2), track)))
    }
    @Test fun sameAlbumTitleFromDifferentArtistsRemainsDistinct() {
        val albums = listOf(HomeItem.Album("Greatest hits", "", "", "A"), HomeItem.Album("Greatest hits", "", "", "B"))
        assertEquals(albums, HomeLayoutPolicy.rotation(emptyList(), albums))
    }
    @Test fun freshListeningDataKeepsSpotifyMixAtOneHundredPercent() {
        assertEquals(0.0, HomeMixPolicy.localFraction(0), 0.001)
        assertEquals(0.0, HomeMixPolicy.localFraction(2), 0.001)
        assertEquals(0.2, HomeMixPolicy.localFraction(3), 0.001)
        assertEquals(0.8, HomeMixPolicy.localFraction(150), 0.001)
        assertEquals(1.0, HomeMixPolicy.localFraction(300), 0.001)
    }
}
