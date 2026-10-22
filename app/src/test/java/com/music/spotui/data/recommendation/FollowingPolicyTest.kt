package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class FollowingPolicyTest {
    private fun track(id: Int, spotify: String = "$id", plays: Long? = null) = ArtistTrackUi(
        SongsModel(id = id, title = "$id", album = "", singer = "Artist", coverUri = "", url = "", spotifyTrackId = spotify), plays)

    @Test fun popularMixUsesPlayCountsAndDeduplicatesCollaborations() {
        val mix = FollowingPolicy.popular(listOf(track(1, plays = 50), track(2, "shared", 10), track(3, "shared", 100), track(4)))
        assertEquals(listOf(3, 1, 4), mix.map { it.id })
    }
    @Test fun newReleasesRejectYearOnlyDatesAndFutureReleases() {
        val today = LocalDate.of(2026, 10, 14)
        assertFalse(FollowingPolicy.isRecent("2026", today))
        assertFalse(FollowingPolicy.isRecent("2026-10-15", today))
        assertFalse(FollowingPolicy.isRecent("2026-06-01", today))
        assertTrue(FollowingPolicy.isRecent("2026-10-14T00:00:00Z", today))
        assertTrue(FollowingPolicy.isRecent("2026-07-16", today))
    }
    @Test fun releasesUseFullDatesAndDeduplicateAcrossFollowedArtists() {
        fun release(id: String, date: String) = FollowingRelease(id, id, "Artist", "", date)
        val list = FollowingPolicy.newest(listOf(release("a", "2026-01-01"), release("b", "2026-10-01"), release("b", "2026-10-01")))
        assertEquals(listOf("b", "a"), list.map { it.id })
    }
}
