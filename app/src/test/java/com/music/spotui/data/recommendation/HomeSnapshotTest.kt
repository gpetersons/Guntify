package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import com.music.spotui.data.preferences.HomeSnapshotCodec
import org.junit.Assert.*
import org.junit.Test

class HomeSnapshotTest {
    private val song = SongsModel(42, "Track", "Album", "Artist", "https://art", "spotify:track:abc", "abc", true, 123000)
    private val feed = HomeFeedModel("Evening", listOf(HomeSection("Made For account name", listOf(
        HomeItem.Album("Album", "art", "Album • Artist", "Artist"), HomeItem.Playlist("Mix", "art2", "Playlist", "list-id"),
        HomeItem.Artist("Artist", "art3", "artist-id"), HomeItem.Radio(song), HomeItem.RecentTrack(song)
    ))))
    @Test fun roundTripPreservesTitlesOrderingIdsAndPlayableRadio() {
        assertEquals(feed, HomeSnapshotCodec.decode(HomeSnapshotCodec.encode("owner", "30", feed), "owner", "30"))
    }
    @Test fun anotherAccountCannotReadSnapshot() {
        assertNull(HomeSnapshotCodec.decode(HomeSnapshotCodec.encode("owner", "30", feed), "other", "30"))
    }
    @Test fun changedHistoryWindowRejectsOldDiscoveries() {
        assertNull(HomeSnapshotCodec.decode(HomeSnapshotCodec.encode("owner", "30", feed), "owner", "90"))
    }
    @Test fun corruptOrFutureFormatFallsBackSafely() {
        for (raw in listOf("broken", "{}", HomeSnapshotCodec.encode("owner", "30", feed).replace("\"version\":1", "\"version\":9")))
            assertNull(HomeSnapshotCodec.decode(raw, "owner", "30"))
    }
    @Test fun failedRefreshKeepsLastGoodShelf() {
        val fresh = listOf(HomeSection(feed.sections[0].title, emptyList()))
        assertEquals(feed.sections, HomeSnapshotCodec.retainAvailable(fresh, feed.sections))
    }
    @Test fun successfulRefreshReplacesCardsWithoutChangingTitle() {
        val fresh = listOf(HomeSection(feed.sections[0].title, listOf(HomeItem.Radio(song.copy(title = "New")))))
        assertEquals(fresh, HomeSnapshotCodec.retainAvailable(fresh, feed.sections))
    }
    @Test fun firstLoginWithoutCacheCanUseNewResults() {
        assertEquals(feed.sections, HomeSnapshotCodec.retainAvailable(feed.sections, emptyList()))
    }
}
