package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import org.junit.Assert.*
import org.junit.Test

class SearchPlaylistPagingTest {
    private fun playlist(id: String) = LibraryEntry(id, "Mix $id", "Playlist", "", true)
    private fun song(id: Int) = SongsModel(id, "Song", "", "", "", "q", "s$id")

    @Test fun playlistsFilterKeepsPlaylistResultsAndHidesSongs() {
        val all = SearchResults(songs = listOf(song(1)), playlists = listOf(playlist("a")))
        val filtered = SearchFilterPolicy.filter(all, "Playlists")
        assertEquals(all.playlists, filtered.playlists)
        assertTrue(filtered.songs.isEmpty())
        assertEquals(all, SearchFilterPolicy.filter(all, "All"))
    }

    @Test fun overlappingPagesPreserveSpotifyOrderAndRemoveDuplicates() {
        val old = SearchResults(songs = listOf(song(1)), playlists = listOf(playlist("a"), playlist("b")), nextOffset = 50)
        val page = SearchResults(songs = listOf(song(1), song(2)), playlists = listOf(playlist("b"), playlist("c")), nextOffset = 100)
        val merged = SearchPagePolicy.append(old, page)
        assertEquals(listOf("a", "b", "c"), merged.playlists.map { it.spotifyId })
        assertEquals(listOf(1, 2), merged.songs.map { it.id })
        assertEquals(100, merged.nextOffset)
    }

    @Test fun lastPageClearsLoadMoreAndPreservesPodcastResults() {
        val old = SearchResults(shows = listOf(PodcastModel("p", "Podcast", "", "")), nextOffset = 50)
        val merged = SearchPagePolicy.append(old, SearchResults(playlists = listOf(playlist("a"))))
        assertNull(merged.nextOffset)
        assertEquals(old.shows, merged.shows)
    }

    @Test fun songsFilterDoesNotLeakPlaylists() {
        val result = SearchFilterPolicy.filter(SearchResults(playlists = listOf(playlist("a"))), "Songs")
        assertTrue(result.playlists.isEmpty())
    }
}
