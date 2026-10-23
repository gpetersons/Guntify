package com.music.spotui.data.recommendation

import androidx.media3.common.Timeline
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.ui.notification.CarQueueTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarQueueTimelineTest {
    private fun song(id: Int) = SongsModel(
        id = id, title = "Track $id", album = "Album",
        singer = "Artist $id", coverUri = "", url = "stream:$id",
        durationMs = 180000,
    )

    @Test fun entireQueueIsProjectedWithCorrectArtistAndSource() {
        val timeline = CarQueueTimeline(listOf(song(5), song(6), song(7)), "Endless radio")
        assertEquals(3, timeline.windowCount)
        assertEquals(3, timeline.periodCount)
        assertEquals("Track 6", timeline.getWindow(1, Timeline.Window()).mediaItem.mediaMetadata.title.toString())
        assertEquals("Artist 6", timeline.getWindow(1, Timeline.Window()).mediaItem.mediaMetadata.artist.toString())
        assertEquals("Endless radio", timeline.getWindow(1, Timeline.Window()).mediaItem.mediaMetadata.albumTitle.toString())
        assertEquals(1, timeline.currentIndex(6))
        assertEquals(0, timeline.currentIndex(-1))
        assertEquals("car-queue/7", timeline.itemAt(2).mediaId)
    }

    @Test fun everyPeriodHasStableQueueIdentity() {
        val timeline = CarQueueTimeline(listOf(song(2), song(9)), "Liked Songs")
        val first = timeline.getUidOfPeriod(0)
        val second = timeline.getUidOfPeriod(1)
        assertEquals(0, timeline.getIndexOfPeriod(first))
        assertEquals(1, timeline.getIndexOfPeriod(second))
        assertEquals(-1, timeline.getIndexOfPeriod("unknown"))
        assertEquals(1, timeline.getPeriod(1, Timeline.Period(), true).windowIndex)
        assertTrue(timeline.getWindow(0, Timeline.Window()).isSeekable)
    }

    @Test fun queueSupportsEmptyAndSingleTrackWithoutFabricatedItems() {
        assertEquals(0, CarQueueTimeline(emptyList(), "").windowCount)
        assertEquals(1, CarQueueTimeline(listOf(song(5)), "").windowCount)
    }
}
