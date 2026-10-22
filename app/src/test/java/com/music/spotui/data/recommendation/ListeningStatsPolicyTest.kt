package com.music.spotui.data.recommendation

import com.music.spotui.data.preferences.HistoryEntry
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ListeningStatsPolicyTest {
    private val now = Instant.parse("2026-10-14T01:00:00Z").toEpochMilli()
    private val day = 86_400_000L
    private fun visit(id: Int = 1, age: Long = 0, singer: String = "Artist", played: Long = 30000) =
        HistoryEntry(now - age, id, "Song $id", singer, image = "art$id", url = "query:$id", spotifyTrackId = "track$id", listenedMs = played)
    private fun stats(vararg entries: HistoryEntry, days: Int = 30) = ListeningStatsPolicy.summary(entries.toList(), now, days, ZoneId.of("UTC"))

    @Test fun onlyQualifiedLocalAudiblePlaysCount() {
        val result = stats(visit(), visit(2, played = 10000), visit(3).copy(source = "spotify"), visit(4).copy(ts = now + 1))
        assertEquals(1, result.plays); assertEquals(30000L, result.recordedMs)
    }
    @Test fun ranksByPlayCountInsteadOfRecommendationRecency() {
        val result = stats(visit(1, 20 * day), visit(1, 19 * day), visit(2))
        assertEquals(1, result.songs.first().song.id); assertEquals(2, result.songs.first().plays)
    }
    @Test fun sameSpotifyTrackGroupsAcrossLocalIdsAndKeepsLatestPlayableMetadata() {
        val result = stats(visit(1, day), visit(2).copy(spotifyTrackId = "track1", title = "Latest"))
        assertEquals(1, result.songs.size); assertEquals(2, result.songs.single().plays)
        assertEquals("query:2", result.songs.single().song.url); assertEquals("Latest", result.songs.single().song.title)
    }
    @Test fun localTracksRemainDistinctWithoutSpotifyIds() {
        val result = stats(visit(1).copy(spotifyTrackId = ""), visit(2).copy(spotifyTrackId = ""))
        assertEquals(2, result.songs.size)
    }
    @Test fun changingRangeFiltersPlaysAndRecordedTimeTogether() {
        val visits = arrayOf(visit(), visit(2, 15 * day), visit(3, 60 * day))
        assertEquals(1, stats(*visits, days = 7).plays)
        assertEquals(60000L, stats(*visits, days = 30).recordedMs)
        assertEquals(3, stats(*visits, days = 0).plays)
    }
    @Test fun collaboratorsCountOncePerVisitIgnoringCase() {
        val result = stats(visit(singer = "ARTIST, artist feat. Guest"), visit(singer = "Artist"))
        assertEquals(2, result.artistCount); assertEquals(2, result.artists.first().plays)
    }
    @Test fun dailyActivityUsesDeviceCalendarInsteadOfUtcDays() {
        val result = ListeningStatsPolicy.summary(listOf(visit()), now, 7, ZoneId.of("America/New_York"))
        assertEquals("2026-10-13", result.activity.last().date.toString())
        assertEquals(1, result.activity.last().plays); assertEquals(7, result.activity.size)
    }
    @Test fun noListeningProducesEmptyRankingsAndSevenZeroDays() {
        val result = stats()
        assertEquals(0, result.plays); assertTrue(result.songs.isEmpty()); assertTrue(result.artists.isEmpty())
        assertTrue(result.activity.all { it.plays == 0 })
    }
    @Test fun laterIncompleteMetadataDoesNotMakeAPlayedSongUnplayable() {
        val result = stats(visit(age = day), visit().copy(url = ""))
        assertEquals("query:1", result.songs.single().song.url)
        assertEquals(2, result.songs.single().plays)
    }
    @Test fun artistArtworkFallsBackToEarlierAvailableTrackArt() {
        val result = stats(visit(age = day), visit(2).copy(image = ""))
        assertEquals("art1", result.artists.single().artwork)
    }
}
