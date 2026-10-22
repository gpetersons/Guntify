package com.music.spotui.data.recommendation

import com.music.spotui.audio.PlaybackRestorePolicy
import com.music.spotui.data.entity.PlaybackCollection
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.PlaybackQueueSnapshot
import org.junit.Assert.*
import org.junit.Test

class PlaybackRestoreTest {
    private fun song(id: Int) = SongsModel(id, "Song $id", "Album", "Artist", "", "query$id", "spotify$id")
    private val station = PlaybackCollection("radio:original", "radio", "original", "Original Radio", "cover", "Artist")

    @Test fun noSavedQueueRestoresTheLastSongWithoutInventingACollection() {
        val restored = PlaybackRestorePolicy.snapshot(song(3), null)
        assertEquals(listOf(song(3)), restored.songs)
        assertNull(restored.collection)
        assertNull(restored.radioSeed)
        assertFalse(restored.shuffled)
    }

    @Test fun unrelatedSavedQueueCannotReplaceTheLastPlayedSong() {
        val saved = PlaybackQueueSnapshot(listOf(song(1)), station, "original", true, listOf(song(1)))
        assertEquals(PlaybackRestorePolicy.snapshot(song(3), null), PlaybackRestorePolicy.snapshot(song(3), saved))
    }

    @Test fun shuffledAlbumRestoresTheExactQueueAndOriginalOrder() {
        val album = station.copy(key = "album:a", type = "album", id = "a")
        val saved = PlaybackQueueSnapshot(listOf(song(3), song(1), song(2)), album, null, true, (1..3).map(::song))
        assertEquals(saved, PlaybackRestorePolicy.snapshot(song(1), saved))
    }

    @Test fun radioRestoresStationIdentityAndCurrentPlusTenUpcomingTracks() {
        val saved = PlaybackQueueSnapshot((1..50).map(::song), station, "original", true, (1..50).map(::song))
        val result = PlaybackRestorePolicy.snapshot(song(15), saved)
        assertEquals((1..25).map(::song), result.songs)
        assertEquals(station, result.collection)
        assertEquals("original", result.radioSeed)
        assertEquals(result.songs, result.original)
        assertTrue(result.shuffled)
    }
}
