package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.*
import org.junit.Assert.*
import org.junit.Test
class EvolvingRadioAndSearchTest {
    private fun song(id: Int) = SongsModel(id, "Song $id", "", "", "", "q$id", "s$id")
    @Test fun rotatesRecommendationSeedAfterExactlyFiveQualifiedVisits() {
        var progress = EvolvingRadioPolicy.Progress("original")
        for (i in 1..4) { progress = EvolvingRadioPolicy.qualify(progress, "s$i", "v$i"); assertEquals("original", progress.seed) }
        progress = EvolvingRadioPolicy.qualify(progress, "s5", "v5")
        assertEquals("s5", progress.seed); assertEquals(0, progress.count)
    }
    @Test fun queueRefreshDoesNotCountTheSamePlaybackVisitTwice() {
        val progress = EvolvingRadioPolicy.qualify(EvolvingRadioPolicy.Progress("original"), "song", "same-visit")
        repeat(10) { assertEquals(progress, EvolvingRadioPolicy.qualify(progress, "song", "same-visit")) }
    }
    @Test fun persistedProgressContinuesAfterReopening() {
        val restored = EvolvingRadioPolicy.Progress("original", 4, "last-visit")
        assertEquals("new-track", EvolvingRadioPolicy.qualify(restored, "new-track", "new-process-visit").seed)
    }
    @Test fun refillKeepsCurrentSongAndExactlyTenUpcomingWithNoDuplicates() {
        val result = EvolvingRadioPolicy.fill(song(3), (1..5).map(::song), (1..30).map(::song), false)
        assertEquals((1..3).map(::song), result.take(3)); assertEquals(13, result.size)
        assertEquals(listOf(song(4),song(5)), result.drop(3).take(2))
        assertEquals(13, result.map { it.id }.distinct().size)
    }
    @Test fun rotatingSeedPrioritizesFreshSongsOverTheOldUpcomingBatch() {
        val result = EvolvingRadioPolicy.fill(song(3), (1..15).map(::song), (1..40).map(::song), true)
        assertEquals((1..3).map(::song), result.take(3))
        assertTrue(result.drop(3).all { it.id > 15 })
    }
    @Test fun failedRefillPreservesAvailableUpcomingSongs() {
        assertEquals((1..4).map(::song), EvolvingRadioPolicy.fill(song(1), (1..4).map(::song), emptyList(), false))
    }
    @Test fun searchTypeFiltersHideOtherTypesWithoutChangingInputResults() {
        val original = SearchResults(listOf(song(1)), listOf(AlbumsModel(1,"Artist","","Album","")), listOf(ArtistsModel("Artist","","id")), listOf(PodcastModel("id","Show","","")), listOf(song(2)))
        assertEquals(original.songs, SearchFilterPolicy.filter(original,"Songs").songs)
        assertTrue(SearchFilterPolicy.filter(original,"Songs").albums.isEmpty())
        assertTrue(SearchFilterPolicy.filter(original,"Albums").songs.isEmpty())
        assertEquals(original.artists, SearchFilterPolicy.filter(original,"Artists").artists)
        assertEquals(original, SearchFilterPolicy.filter(original,"All"))
    }
    @Test fun reopeningLargeOldRadioQueueKeepsLastTrackAndTenUpcoming() {
        assertEquals((1..52).map(::song), EvolvingRadioPolicy.window((1..99).map(::song),42))
    }
    @Test fun localTracksWithoutSpotifyIdentityDoNotAdvanceRecommendationSeed() {
        val original = EvolvingRadioPolicy.Progress("original",4,"visit")
        assertEquals(original, EvolvingRadioPolicy.qualify(original,"","new-visit"))
    }
}
