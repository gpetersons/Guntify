package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.SongsModel
import org.junit.Assert.*
import org.junit.Test
class RadioRefreshTest {
    private fun song(id: Int) = SongsModel(id, "Song $id", "", "", "", "q$id", "s$id")
    @Test fun keepsCurrentTrackFirstAndPrioritizesNovelSpotifySongs() {
        val current = song(1)
        val result = RadioQueuePolicy.refreshed(current, (1..15).map(::song), (1..40).map(::song))
        assertEquals(current, result.first())
        assertEquals(11, result.size)
        assertTrue(result.drop(1).all { it.id > 15 })
    }
    @Test fun failedLookupReturnsNoReplacementSoCallerCanKeepExistingQueue() {
        assertTrue(RadioQueuePolicy.refreshed(song(1), (1..15).map(::song), emptyList()).isEmpty())
    }
    @Test fun duplicateCandidatesCannotEnterRefreshedQueue() {
        val result = RadioQueuePolicy.refreshed(song(1), listOf(song(1)), listOf(song(2),song(2),song(1),song(3)))
        assertEquals(3, result.size)
        assertEquals(3, result.map { it.spotifyTrackId }.distinct().size)
    }
    @Test fun unchangedSpotifyPoolIsReorderedWhenMoreThanOneUpcomingSongExists() {
        val original = (1..5).map(::song)
        repeat(20) { assertNotEquals(original, RadioQueuePolicy.refreshed(original.first(), original, original, shuffled = true)) }
    }
    @Test fun refreshSeedPolicyNeverUsesTheResumedSong() {
        assertEquals(listOf("original"), RadioQueuePolicy.seeds("original", listOf(song(99))))
    }
}
