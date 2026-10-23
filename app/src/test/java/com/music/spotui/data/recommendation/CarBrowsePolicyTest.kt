package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import com.music.spotui.ui.notification.CarBrowsePolicy
import org.junit.Assert.*
import org.junit.Test

class CarBrowsePolicyTest {
    private fun song(id: Int) = SongsModel(id, "Song $id", "Album", "Artist", "art", "q$id", "s$id")
    @Test fun personalizedRootHasStableBrowsableSections() {
        val root = CarBrowsePolicy.root()
        assertEquals(listOf("Playing queue", "Continue listening", "Recommended for you",
            "Made for you", "Recently played", "Your radios", "Liked Songs", "Library"),
            root.map { it.title })
        assertTrue(root.all { !it.playable })
        assertEquals(root.map { it.id }, CarBrowsePolicy.root("Bones Radio").map { it.id })
        assertEquals("Playing: Bones Radio", CarBrowsePolicy.root("Bones Radio").first().title)
    }
    @Test fun radioRowIsPlayableWithOriginalStationIdentity() {
        val seed = song(1)
        val row = CarBrowsePolicy.recent(listOf(HomeItem.Radio(seed))).single()
        assertTrue(row.playable)
        assertEquals("s1", CarBrowsePolicy.radioStation(row.id))
        assertEquals("Song 1 Radio", row.title)
    }
    @Test fun mixedRadioKeepsItsOwnNameAndSyntheticId() {
        val seed = song(1).copy(title = "My mix", spotifyTrackId = "uspot-mix:one")
        val row = CarBrowsePolicy.recent(listOf(HomeItem.Radio(seed))).single()
        assertEquals("My mix", row.title)
        assertEquals(seed.spotifyTrackId, CarBrowsePolicy.radioStation(row.id))
    }
    @Test fun localRadioWithoutSpotifyIdStillHasStableIdentity() {
        val seed = song(42).copy(spotifyTrackId = "")
        assertEquals("42", CarBrowsePolicy.radioStation(CarBrowsePolicy.recent(listOf(HomeItem.Radio(seed))).single().id))
    }
    @Test fun radioIdsRoundTripSpecialCharacters() {
        val station = "mix:a/b + é%"
        assertEquals(station, CarBrowsePolicy.radioStation(CarBrowsePolicy.radioId(station)))
        assertNull(CarBrowsePolicy.radioStation("playlist/s1"))
        assertNull(CarBrowsePolicy.radioStation("radio/%broken"))
    }
    @Test fun recentsExcludeSongsArtistsAndDuplicateCollections() {
        val radio = HomeItem.Radio(song(1))
        val rows = CarBrowsePolicy.recent(listOf(HomeItem.RecentTrack(song(2)), HomeItem.Artist("Artist", "art", "a"), radio, radio))
        assertEquals(1, rows.size)
    }
    @Test fun collectionOrderIsPreservedAndBounded() {
        val rows = CarBrowsePolicy.recent((1..60).map { HomeItem.Radio(song(it)) })
        assertEquals(40, rows.size)
        assertEquals("s1", CarBrowsePolicy.radioStation(rows.first().id))
        assertTrue(CarBrowsePolicy.recent(listOf(HomeItem.Radio(song(1))), -1).isEmpty())
    }
    @Test fun likedAndDownloadsUseTheirExistingBrowseNodes() {
        val rows = CarBrowsePolicy.recent(listOf(HomeItem.Playlist("Liked", "art", "", "liked-songs"), HomeItem.Playlist("Offline", "art", "", "downloaded-offline")))
        assertEquals(listOf("liked", "downloads"), rows.map { it.id })
        assertTrue(rows.none { it.playable })
    }
    @Test fun albumNamesAndArtistsCannotBreakBrowsePath() {
        val row = CarBrowsePolicy.recent(listOf(HomeItem.Album("A/B", "art", "Artist", "C/D"))).single()
        assertEquals("album/A%2FB/C%2FD", row.id)
    }
    @Test fun sameSongInTwoCollectionsHasDifferentPlaybackIds() {
        assertNotEquals(CarBrowsePolicy.trackId("playlist/a", song(1)), CarBrowsePolicy.trackId("playlist/b", song(1)))
    }
    @Test fun savedRadioQueueWinsOverSeedAndOtherResumeTrack() {
        val saved = (3..20).map(::song)
        assertEquals(saved.take(11), CarBrowsePolicy.radioSongs(song(1), song(2), listOf(song(1)), saved))
    }
    @Test fun missingQueueStartsFromLastTrackThenOriginalAnchor() {
        assertEquals(listOf(song(2)), CarBrowsePolicy.radioSongs(song(1), song(2), listOf(song(1)), emptyList()))
        val marker = song(1).copy(url = "", spotifyTrackId = "uspot-mix:a")
        assertEquals(listOf(song(3)), CarBrowsePolicy.radioSongs(marker, null, listOf(song(3)), emptyList()))
        assertTrue(CarBrowsePolicy.radioSongs(marker, null, emptyList(), emptyList()).isEmpty())
    }
    @Test fun recentRadiosIncludeLocalAndCatalogRadioPlaylistsOnly() {
        val items = listOf(HomeItem.Playlist("Country Radio", "art", "", "p1"),
            HomeItem.Album("Rock Radio", "art", "", "Artist"), HomeItem.Radio(song(1)),
            HomeItem.Playlist("Radiohead Essentials", "art", "", "p2"), HomeItem.RecentTrack(song(2)))
        val rows = CarBrowsePolicy.recentRadios(items)
        assertEquals(listOf("playlist/p1", "radio/s1"), rows.map { it.id })
        assertEquals("Spotify playlist", rows.first().subtitle)
        assertFalse(rows.first().playable)
        assertTrue(rows.last().playable)
    }
    @Test fun radioFilteringHappensBeforeLimitAndKeepsNewestOrder() {
        val albums = (1..50).map { HomeItem.Album("Album $it", "", "", "Artist") }
        val radio = HomeItem.Radio(song(1))
        assertEquals("radio/s1", CarBrowsePolicy.recentRadios(albums + radio).single().id)
        assertTrue(CarBrowsePolicy.recentRadios(listOf(radio), 0).isEmpty())
        assertEquals(1, CarBrowsePolicy.recentRadios(listOf(radio, radio)).size)
    }
    @Test fun radioTitleRuleAvoidsOrdinaryNamesAndIgnoresCase() {
        assertTrue(CarBrowsePolicy.isRadioPlaylist("Artist rAdIo  "))
        assertFalse(CarBrowsePolicy.isRadioPlaylist("Radiohead"))
        assertFalse(CarBrowsePolicy.isRadioPlaylist("Best country"))
    }
    @Test fun downloadsRemainBrowsableUnderLibrary() {
        assertEquals(listOf("playlists", "albums", "downloads"), CarBrowsePolicy.library().map { it.id })
        assertTrue(CarBrowsePolicy.library().all { !it.playable })
    }

}
