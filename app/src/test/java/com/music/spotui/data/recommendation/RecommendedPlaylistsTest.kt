package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.*
import org.junit.Assert.*
import org.junit.Test

class RecommendedPlaylistsTest {
    private fun playlist(id: String) = HomeItem.Playlist(id, "art:$id", "Playlist", id)
    private val spotify = HomeSection("Recommended Stations", (1..5).map { playlist("spotify$it") })
    private val discovered = HomeSection(LocalHomePolicy.PLAYLISTS, (1..5).map { playlist("local$it") })
    private fun recommended(rows: List<HomeSection>) = rows.first { it.title == "Recommended playlists" }
    @Test fun newUserStartsWithSpotifyEvenIfLocalCandidatesExist() {
        val row = recommended(LocalHomePolicy.shelves(listOf(spotify), listOf(discovered), 0.0))
        assertEquals(spotify.items.map { it.name }, row.items.map { it.name })
    }
    @Test fun gradualTransitionUsesTheConfiguredLocalShare() {
        for ((share, expected) in listOf(0.2 to 1, 0.4 to 2, 0.6 to 3, 0.8 to 4, 1.0 to 5)) {
            val row = recommended(LocalHomePolicy.shelves(listOf(spotify), listOf(discovered), share))
            assertEquals(expected, row.items.filterIsInstance<HomeItem.Playlist>().count { it.id.startsWith("local") })
            assertEquals(5, row.items.size)
        }
    }
    @Test fun rejectsWrongTypesAndDeadPlaylistIds() {
        val invalid = HomeSection(LocalHomePolicy.PLAYLISTS, listOf(HomeItem.Radio(SongsModel()),
            HomeItem.Album("Album", "", ""), playlist("")))
        val row = recommended(LocalHomePolicy.shelves(listOf(spotify), listOf(invalid), 1.0))
        assertEquals(spotify.items.map { it.name }, row.items.map { it.name })
        assertTrue(row.items.all { it is HomeItem.Playlist && it.id.isNotBlank() })
    }
    @Test fun coldStartCanFillMissingShelfFromOtherSpotifyRows() {
        val made = HomeSection("Made For Any Account", (1..5).map { playlist("made$it") })
        val extra = HomeSection("Spotify suggestions", (1..5).map { playlist("extra$it") })
        val rows = LocalHomePolicy.shelves(listOf(made, extra), emptyList(), 0.0)
        assertEquals(made.title, rows.first().title)
        assertEquals(extra.items.map { it.name }, recommended(rows).items.map { it.name })
    }
    @Test fun avoidsRepeatingPlaylistsAcrossMadeForAndRecommended() {
        val made = HomeSection("Made For Any Account", listOf(playlist("same")))
        val station = HomeSection("Recommended Stations", listOf(playlist("same"), playlist("other")))
        val rows = LocalHomePolicy.shelves(listOf(made, station), emptyList(), 0.0)
        assertEquals(listOf("other"), recommended(rows).items.filterIsInstance<HomeItem.Playlist>().map { it.id })
    }
    @Test fun discoveryFailureKeepsSpotifyCardsAtAnyPersonalizationLevel() {
        val row = recommended(LocalHomePolicy.shelves(listOf(spotify), emptyList(), 1.0))
        assertEquals(spotify.items.map { it.name }, row.items.map { it.name })
    }
    @Test fun fallbackAlbumsPopulateMadeForWithTheSignedInAccountName() {
        val albums = HomeSection("Albums", listOf(HomeItem.Album("Spotify album", "art", "Artist", "Artist")))
        val rows = LocalHomePolicy.shelves(listOf(albums), emptyList(), 0.0, accountName = "Another account")
        assertEquals("Made For Another account", rows.first().title)
        assertEquals(albums.items, rows.first().items)
    }
}
