package com.music.spotui.data.recommendation

import com.music.spotui.audio.CollectionPlaybackPolicy
import com.music.spotui.audio.NextTrackPreparationPolicy
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import org.junit.Assert.*
import org.junit.Test

class HomeAndPreparationTest {
    private val rows = (1..12).map { HomeSection("More like Artist $it", listOf(HomeItem.Playlist("Mix $it", "", "", "$it"))) }
    @Test fun initialHomeIsBoundedWithOriginalSectionIdentity() {
        val visible = HomeLayoutPolicy.visible(rows, 4)
        assertEquals(4, visible.size)
        assertEquals(rows.take(4), visible)
    }
    @Test fun moreRowsAreAvailableWithoutChangingTheirContent() {
        assertEquals(rows.take(8), HomeLayoutPolicy.visible(rows, 8))
        assertEquals(rows, HomeLayoutPolicy.visible(rows, 100))
    }
    @Test fun emptySectionsDoNotConsumePreviewSlots() {
        val input = listOf(HomeSection("Empty", emptyList())) + rows
        assertEquals(rows.take(4), HomeLayoutPolicy.visible(input, 4))
    }
    @Test fun preparationBeginsAtTenSecondsRemaining() {
        assertFalse(NextTrackPreparationPolicy.shouldPrepare(180_000, 169_999, true, false, 0))
        assertTrue(NextTrackPreparationPolicy.shouldPrepare(180_000, 170_000, true, false, 0))
    }
    @Test fun bufferingPauseRepeatAndUnknownDurationDoNotPrepare() {
        assertFalse(NextTrackPreparationPolicy.shouldPrepare(180_000, 175_000, false, false, 0))
        assertFalse(NextTrackPreparationPolicy.shouldPrepare(180_000, 175_000, true, true, 0))
        assertFalse(NextTrackPreparationPolicy.shouldPrepare(-1, 0, true, false, 0))
        assertFalse(NextTrackPreparationPolicy.shouldPrepare(180_000, 180_000, true, false, 0))
    }
    @Test fun longCrossfadeGetsLeadTimeBeforeItsBlend() {
        assertTrue(NextTrackPreparationPolicy.shouldPrepare(180_000, 155_000, true, false, 20_000))
        assertFalse(NextTrackPreparationPolicy.shouldPrepare(180_000, 154_999, true, false, 20_000))
    }
    @Test fun shuffleOrNewSessionDiscardsTheStaleNextSong() {
        assertFalse(NextTrackPreparationPolicy.stillNext("A", "B", true))
        assertFalse(NextTrackPreparationPolicy.stillNext("A", "A", false))
        assertFalse(NextTrackPreparationPolicy.stillNext("A", null, true))
        assertTrue(NextTrackPreparationPolicy.stillNext("A", "A", true))
    }
    @Test fun sameSongInAnotherPlaylistDoesNotActivateThisHeader() {
        assertFalse(CollectionPlaybackPolicy.active("playlist:one", "playlist:two", 42, listOf(42)))
        assertFalse(CollectionPlaybackPolicy.active("album:one", "radio:one", 42, listOf(42)))
    }
    @Test fun onlyTheActualCollectionAndCurrentQueueActivateItsHeader() {
        assertTrue(CollectionPlaybackPolicy.active("playlist:one", "playlist:one", 42, listOf(42)))
        assertFalse(CollectionPlaybackPolicy.active("playlist:one", "playlist:one", 42, listOf(43)))
    }
}
