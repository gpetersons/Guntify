package com.music.spotui.data.recommendation
import com.music.spotui.audio.SmartShufflePolicy
import com.music.spotui.data.entity.*
import com.music.spotui.data.preferences.HistoryEntry
import org.junit.Assert.*
import org.junit.Test

class ListeningFeaturesTest {
    private val now = 100L * 86_400_000
    private fun song(id: Int, artist: String = "Artist") = SongsModel(id, "Song $id", "Album", artist, "", "q$id", "s$id")
    private fun play(id: Int, age: Int = 0, source: String = "spotui", time: Long = 30_000) =
        HistoryEntry(now - age * 86_400_000L, id, "Song $id", "Artist", url = "q$id", spotifyTrackId = "s$id", source = source, listenedMs = time)
    @Test fun smartDownloadSelectionUsesQualifiedLocalHistoryOnly() {
        val history = listOf(play(1), play(2, source = "spotify"), play(3, time = 10_000), play(4, age = 31), play(5, age = -1))
        assertEquals(listOf(song(1).id), ListeningTastePolicy.topSongs(history, now, 20).map { it.id })
    }
    @Test fun smartDownloadDefaultCanFillTwentyDistinctSongsAndCountCanChange() {
        val history = (1..40).flatMap { listOf(play(it), play(it)) }
        assertEquals(20, ListeningTastePolicy.topSongs(history, now, 20).size)
        assertEquals(5, ListeningTastePolicy.topSongs(history, now, 5).size)
        assertTrue(ListeningTastePolicy.topSongs(history, now, 0).isEmpty())
    }
    @Test fun recentTasteAndLikedSongsInfluenceTheAutomaticSet() {
        val history = listOf(play(1, age = 20), play(1, age = 20), play(2))
        assertEquals(2, ListeningTastePolicy.topSongs(history, now, 1).single().id)
        assertEquals(2, ListeningTastePolicy.topSongs(listOf(play(1), play(2)), now, 1, setOf(2)).single().id)
    }
    @Test fun smartShuffleAddsRelatedCandidatesWithoutChangingSourceList() {
        val original = (1..9).map(::song)
        val (queue, added) = SmartShufflePolicy.inject(original, (10..20).map(::song), original.map { it.id }.toSet())
        assertEquals((1..9).map(::song), original)
        assertEquals(listOf(1,2,3,10,4,5,6,11,7,8,9,12), queue.map { it.id })
        assertEquals(setOf(10,11,12), added)
    }
    @Test fun smartShuffleExcludesExistingAndDuplicateSuggestions() {
        val (queue, added) = SmartShufflePolicy.inject(listOf(song(1)), listOf(song(1), song(2), song(2)), setOf(1))
        assertEquals(listOf(song(1), song(2)), queue)
        assertEquals(setOf(2), added)
    }
    @Test fun radioOffShufflePreservesReturnedOrderOnRotationAndRefresh() {
        val previous = (1..4).map(::song)
        val recommendations = (10..25).map(::song)
        assertEquals(listOf(song(1)) + (10..19).map(::song), EvolvingRadioPolicy.fill(song(1), previous, recommendations, true, shuffled = false))
        assertEquals(listOf(song(1)) + (10..19).map(::song), RadioQueuePolicy.refreshed(song(1), previous, recommendations, shuffled = false))
    }
    @Test fun radioTuningChangesSeedInfluenceWithoutDuplicates() {
        val batches = listOf((1..8).map(::song), (10..18).map(::song))
        assertEquals(listOf(1,2,3,4), RadioTastePolicy.blend(emptyList(), batches, "close").take(4).map { it.id })
        assertEquals(listOf(1,10,11,12), RadioTastePolicy.blend(emptyList(), batches, "explore").take(4).map { it.id })
    }
    @Test fun radioFeedbackFiltersExactArtistsAndDislikedTracks() {
        assertFalse(RadioTastePolicy.allowed(song(1), setOf("s1"), emptySet()))
        assertFalse(RadioTastePolicy.allowed(song(1, "Artist, Guest"), emptySet(), setOf("artist")))
        assertTrue(RadioTastePolicy.allowed(song(1, "Artist Two"), emptySet(), setOf("artist")))
    }
    @Test fun madeForAcceptsExistingRelevantPlaylistsAlongsideAlbums() {
        val original = HomeSection("Made For Another Account", (1..5).map { HomeItem.Playlist("Old $it", "", "", "$it") })
        val local = listOf(HomeItem.Playlist("Country favorites", "", "", "country"), HomeItem.Album("Country album", "", "", "Country Artist"))
        val sections = LocalHomePolicy.shelves(listOf(original), listOf(HomeSection(LocalHomePolicy.ALBUMS, local)), 1.0)
        val result = sections.single { it.title == original.title }
        assertEquals(original.title, result.title)
        assertEquals(local, result.items.take(2))
        assertEquals(5, result.items.size)
        val remaining = sections.single { it.title == "Recommended playlists" }
        assertEquals(listOf("4", "5"), remaining.items.filterIsInstance<HomeItem.Playlist>().map { it.id })
    }
    @Test fun smartSuggestionKeepsItsCollectionActiveWithoutLightingOtherCollections() {
        val queue = listOf(song(1), song(2), song(10))
        assertTrue(com.music.spotui.audio.CollectionPlaybackPolicy.active("playlist:p", "playlist:p", 10, queue.map { it.id }))
        assertFalse(com.music.spotui.audio.CollectionPlaybackPolicy.active("playlist:other", "playlist:p", 10, queue.map { it.id }))
    }
}
