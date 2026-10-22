package com.music.spotui.data.recommendation

import com.music.spotui.audio.QueueOrderPolicy
import com.music.spotui.audio.PlaybackRestorePolicy
import com.music.spotui.data.entity.PlaybackCollection
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.PlaybackQueueSnapshot
import org.junit.Assert.*
import org.junit.Test

class QueueHistoryTest {
    private fun song(id: Int) = SongsModel(id, "Song $id", "Album", "Artist", "", "q$id", "s$id")

    @Test fun repeatedRadioRefillsKeepTheSongSeveralPreviousPressesAway() {
        var queue = (1..11).map(::song)
        for (current in 2..6) queue = EvolvingRadioPolicy.fill(song(current), queue, (20..60).map(::song), false, current)
        var index = queue.indexOfFirst { it.id == 6 }
        repeat(3) { index = QueueOrderPolicy.previousIndex(index) }
        assertEquals(song(3), queue[index])
        assertEquals((1..6).map(::song), queue.take(6))
    }

    @Test fun automaticRotationDoesNotRewriteThePlayedSequence() {
        val result = EvolvingRadioPolicy.fill(song(6), (1..15).map(::song), (1..40).map(::song), true, 6)
        assertEquals((1..6).map(::song), result.take(6))
        assertEquals(10, result.drop(6).size)
        assertTrue(result.drop(6).all { it.id > 15 })
    }

    @Test fun refreshWhileBrowsingBackwardKeepsAlreadyHeardTracksAheadToo() {
        val result = RadioQueuePolicy.refreshed(song(3), (1..15).map(::song), (16..40).map(::song), 8)
        assertEquals((1..8).map(::song), result.take(8))
        assertEquals(10, result.drop(8).size)
    }

    @Test fun automaticRefillWhileBrowsingBackwardPreservesThePlaybackBoundary() {
        val result = EvolvingRadioPolicy.fill(song(3), (1..15).map(::song), (16..40).map(::song), true, 8)
        assertEquals((1..8).map(::song), result.take(8))
        assertEquals(10, result.drop(8).size)
    }

    @Test fun playlistAndAlbumShufflePreservesPlayedPrefixEvenWhileGoingBackward() {
        val queue = (1..20).map(::song)
        repeat(20) {
            val shuffled = QueueOrderPolicy.shuffled(queue, 3, 8)
            assertEquals(queue.take(8), shuffled.take(8))
            assertEquals(queue.toSet(), shuffled.toSet())
            val restored = QueueOrderPolicy.unshuffled(shuffled, 3, 8, queue)
            assertEquals(queue, restored)
        }
    }

    @Test fun turningOffShuffleDoesNotUndoTheActualPlayedOrder() {
        val queue = listOf(song(4), song(1), song(3), song(5), song(2))
        val restored = QueueOrderPolicy.unshuffled(queue, 1, 1, (1..5).map(::song))
        assertEquals(listOf(song(4), song(1)), restored.take(2))
        assertEquals(listOf(song(2), song(3), song(5)), restored.drop(2))
    }

    @Test fun previousAtTheBeginningCannotWrapToAnUnplayedTrack() {
        assertEquals(0, QueueOrderPolicy.previousIndex(0))
        assertEquals(0, QueueOrderPolicy.previousIndex(1))
        assertEquals(3, QueueOrderPolicy.previousIndex(4))
    }

    @Test fun restartRetainsPlayedTracksOnBothSidesOfTheCurrentSong() {
        val station = PlaybackCollection("radio:seed", "radio", "seed", "Station", "", "")
        val saved = PlaybackQueueSnapshot((1..50).map(::song), station, "seed", false, null, playedThroughId = 8)
        val restored = PlaybackRestorePolicy.snapshot(song(3), saved)
        assertEquals((1..18).map(::song), restored.songs)
        assertEquals(8, restored.playedThroughId)
        assertEquals(station, restored.collection)
    }

    @Test fun longRadioSessionsBoundPreviousHistoryWithoutExpandingNewUpcomingSongs() {
        val result = EvolvingRadioPolicy.window((1..400).map(::song), 250)
        assertEquals((150..260).map(::song), result)
        assertEquals(100, result.indexOfFirst { it.id == 250 })
        assertEquals(10, result.drop(101).size)
    }
}
