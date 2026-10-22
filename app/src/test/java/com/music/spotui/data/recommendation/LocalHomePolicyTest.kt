package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import org.junit.Assert.*
import org.junit.Test

class LocalHomePolicyTest {
    private val day = 86_400_000L
    private val now = 100 * day
    private fun sample(id: String, artist: String, age: Long = 0, duration: Long = 30_000, source: String = "spotui") =
        LocalTasteSample(id, artist, now - age * day, duration, source)
    private fun originals(title: String) = HomeSection(title, (1..5).map { HomeItem.Playlist("Mix $it", "", "", "$title:p$it") })
    private fun album(id: Int) = HomeItem.Album("Album $id", "", "Artist", "Artist")
    private fun radio(id: Int) = HomeItem.Radio(SongsModel(id, "Song $id", "", "Artist", "", "q", "s$id"))

    @Test fun excludesSkipsSpotifyImportsFutureAndOldPlays() {
        val samples = listOf(sample("valid", "A"), sample("skip", "B", duration = 10_000),
            sample("spotify", "C", source = "spotify"), sample("old", "D", age = 31), sample("future", "E", age = -1))
        assertEquals(listOf("valid"), LocalHomePolicy.topTracks(samples, now))
        assertEquals(listOf("A"), LocalHomePolicy.topArtists(samples, now))
    }
    @Test fun recentWeekHasExtraWeightWithFifteenSongsAndFiveArtists() {
        val samples = List(3) { sample("old", "Old", 20) } + sample("recent", "Recent") +
            (1..25).map { sample("other$it", "Other$it", 29) }
        assertEquals("recent", LocalHomePolicy.topTracks(samples, now).first())
        assertEquals("Recent", LocalHomePolicy.topArtists(samples, now).first())
        assertEquals(15, LocalHomePolicy.topTracks(samples, now).size)
        assertEquals(5, LocalHomePolicy.topArtists(samples, now).size)
    }
    @Test fun preservesAnyAccountNameWhileAddingAlbumsGradually() {
        val row = originals("Made For Someone Else")
        val discoveries = listOf(HomeSection(LocalHomePolicy.ALBUMS, (1..5).map(::album)))
        val result = LocalHomePolicy.shelves(listOf(row), discoveries, 0.2).first()
        assertEquals(row.title, result.title)
        assertEquals(1, result.items.count { it is HomeItem.Album })
        assertEquals(4, result.items.count { it is HomeItem.Playlist })
    }
    @Test fun replacesEveryCardAtFullPersonalization() {
        val result = LocalHomePolicy.shelves(listOf(originals("Made for Example User")),
            listOf(HomeSection(LocalHomePolicy.ALBUMS, (1..5).map(::album))), 1.0).first()
        assertEquals(5, result.items.count { it is HomeItem.Album })
        assertEquals(0, result.items.count { it is HomeItem.Playlist })
    }
    @Test fun partialPersonalizationStillKeepsTheRemainingSpotifyCards() {
        val result = LocalHomePolicy.shelves(listOf(originals("Made for Example User")),
            listOf(HomeSection(LocalHomePolicy.ALBUMS, (1..5).map(::album))), 0.6).first()
        assertEquals(3, result.items.count { it is HomeItem.Album })
        assertEquals(2, result.items.count { it is HomeItem.Playlist })
    }
    @Test fun fullPersonalizationBackfillsWhenOnlyTwoLocalAlbumsAreAvailable() {
        val result = LocalHomePolicy.shelves(listOf(originals("Made for Example User")),
            listOf(HomeSection(LocalHomePolicy.ALBUMS, (1..2).map(::album))), 1.0).first()
        assertEquals(2, result.items.count { it is HomeItem.Album })
        assertEquals(3, result.items.count { it is HomeItem.Playlist })
    }
    @Test fun newUserKeepsOriginalSpotifyCards() {
        val row = originals("Recommended Stations")
        val result = LocalHomePolicy.shelves(listOf(row), listOf(HomeSection(LocalHomePolicy.PLAYLISTS, listOf(radio(1)))), 0.0).first()
        assertEquals("Recommended playlists", result.title)
        assertEquals(row.items.map { it.name }, result.items.map { it.name })
        assertTrue(result.items.all { it is HomeItem.Playlist && it.subtitle == "Spotify playlist" })
    }
    @Test fun playlistShelfReceivesRealSpotifyIdsNotRadioSeeds() {
        val playlist = HomeItem.Playlist("A real playlist", "art", "Spotify playlist", "spotify-list-42")
        val result = LocalHomePolicy.shelves(listOf(originals("Recommended Stations")),
            listOf(HomeSection(LocalHomePolicy.PLAYLISTS, listOf(playlist, radio(42)))), 0.2).first()
        assertEquals(playlist.id, (result.items.first() as HomeItem.Playlist).id)
        assertTrue(result.items.all { it is HomeItem.Playlist })
    }
    @Test fun showsOnlyThreeShelvesAndRecentlyPlayed() {
        val rows = listOf(originals("Made For Example User"), originals("Recommended Stations"), originals("Popular radio"),
            originals("Unrelated editorial row"), originals("Recently played"))
        assertEquals(listOf("Recently played", "Made For Example User", "Recommended playlists", "Popular radio"),
            LocalHomePolicy.shelves(rows, emptyList(), 0.7).map { it.title })
    }
    @Test fun failedDiscoveryDoesNotRemoveSpotifyFallback() {
        val row = originals("Made For Example User")
        assertEquals(row, LocalHomePolicy.shelves(listOf(row), emptyList(), 0.7).first())
    }
    @Test fun existingHistoryWindowSettingStillWorks() {
        val samples = listOf(sample("old", "Artist", age = 60))
        assertTrue(LocalHomePolicy.topTracks(samples, now).isEmpty())
        assertEquals(listOf("old"), LocalHomePolicy.topTracks(samples, now, 90))
    }
    @Test fun oldRadioDiscoveriesAreNotRelabeledAsPlaylists() {
        val result = LocalHomePolicy.shelves(emptyList(), listOf(
            HomeSection("local-home-artist-radios", listOf(radio(1))),
            HomeSection(LocalHomePolicy.SONG_RADIOS, listOf(radio(1), radio(2))),
        ), 0.7)
        assertEquals(listOf("s1", "s2"), result.flatMap { it.items }.filterIsInstance<HomeItem.Radio>().map { it.seed.spotifyTrackId })
    }
}
