package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SongsModel
import com.music.spotui.ui.notification.CarRadioActionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarRadioActionPolicyTest {
    private fun song(id: Int, url: String = "query$id") =
        SongsModel(id, "Song $id", "Album", "Artist", "art", url, "spotify$id")

    @Test fun commandUsesCurrentlyPlayingTrackNotFirstQueueEntry() {
        val tracks = listOf(song(1), song(2), song(3))
        assertEquals(tracks[1], CarRadioActionPolicy.currentTrack(tracks, 2))
    }

    @Test fun commandDoesNotStartRadioWithStaleOrMissingSelection() {
        assertNull(CarRadioActionPolicy.currentTrack(listOf(song(1)), 99))
        assertNull(CarRadioActionPolicy.currentTrack(emptyList(), 1))
    }

    @Test fun commandRejectsUnplayablePlaceholderTrack() {
        assertNull(CarRadioActionPolicy.currentTrack(listOf(song(1, "")), 1))
    }
}
