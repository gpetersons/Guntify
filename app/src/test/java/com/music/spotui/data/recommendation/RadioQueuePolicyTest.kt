package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.HomeMixPolicy
import org.junit.Assert.*
import org.junit.Test

class RadioQueuePolicyTest {
    private fun song(id: Int, spotify: String = "track$id") = SongsModel(id, "Song $id", "Album", "Artist", "", "", spotify)
    @Test fun continuationKeepsOriginalSeedAfterResume() {
        assertEquals(listOf("original"), RadioQueuePolicy.seeds("original", listOf(song(20), song(21))))
    }
    @Test fun ordinaryAutoplayUsesRecentQueue() {
        assertEquals((6..10).map { "track$it" }, RadioQueuePolicy.seeds(null, (1..10).map(::song)))
    }
    @Test fun resumesLastTrackFirstAndCapsAtTenUpcoming() {
        val result = RadioQueuePolicy.resumed(song(1), song(9), (1..30).map(::song), 3)
        assertEquals(11, result.size)
        assertEquals(9, result.first().id)
        assertFalse(result.any { it.id == 1 })
        assertEquals(11, result.map { it.spotifyTrackId }.distinct().size)
    }
    @Test fun duplicatesUseSpotifyIdentityRatherThanIntegerHash() {
        val result = RadioQueuePolicy.resumed(song(1), song(2), listOf(song(3, "same"), song(4, "same"), song(5)))
        assertEquals(1, result.count { it.spotifyTrackId == "same" })
    }
    @Test fun missingRecommendationsPreserveResumeTrack() {
        assertEquals(listOf(song(9)), RadioQueuePolicy.resumed(song(1), song(9), emptyList()))
    }
    @Test fun newRadioFallsBackToSeed() {
        assertEquals(listOf(song(1)), RadioQueuePolicy.resumed(song(1), null, emptyList()))
    }
    @Test fun missingSeedNeverInventsTracks() {
        assertTrue(RadioQueuePolicy.resumed(null, null, emptyList()).isEmpty())
    }
    @Test fun rotationDoesNotAffectFirstTrack() {
        val result = RadioQueuePolicy.resumed(song(1), song(2), (3..20).map(::song), -1)
        assertEquals(2, result.first().id)
        assertEquals(20, result[1].id)
    }
    @Test fun localMixStagesReachOneHundredPercentGradually() {
        val expected = mapOf(0 to 0.0, 2 to 0.0, 3 to 0.2, 19 to 0.2, 20 to 0.4, 59 to 0.4, 60 to 0.6, 149 to 0.6, 150 to 0.8, 299 to 0.8, 300 to 1.0, 500 to 1.0)
        expected.forEach { (plays, fraction) -> assertEquals(fraction, HomeMixPolicy.localFraction(plays), 0.0) }
    }
    @Test fun sliderControlsTheEventualLocalShare() {
        assertEquals(0.5, HomeMixPolicy.fraction(300, 50), 0.0)
        assertEquals(1.0, HomeMixPolicy.fraction(300, 100), 0.0)
        assertEquals(0.0, HomeMixPolicy.fraction(300, 0), 0.0)
        assertEquals(0.0, HomeMixPolicy.fraction(0, 100), 0.0)
        assertEquals(0.2, HomeMixPolicy.fraction(20, 50), 0.0)
    }
    @Test fun replacementCountsRespectSmallRowsAndAvailableRecommendations() {
        assertEquals(5, HomeMixPolicy.replacementCount(5, 1.0, 5))
        assertEquals(2, HomeMixPolicy.replacementCount(5, 1.0, 2))
        assertEquals(0, HomeMixPolicy.replacementCount(5, 0.01, 5))
        assertEquals(0, HomeMixPolicy.replacementCount(5, 0.0, 5))
    }
}
