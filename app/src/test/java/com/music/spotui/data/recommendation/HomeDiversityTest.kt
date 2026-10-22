package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import com.music.spotui.data.preferences.HomeSnapshotCodec
import org.junit.Assert.*
import org.junit.Test

class HomeDiversityTest {
    private fun playlist(id: String, artist: String, mixed: Boolean = false) = HomeItem.Playlist(id, "art", "Playlist", id, artist, mixed)
    @Test fun visitsEveryAvailableArtistBeforeAddingSeconds() {
        val candidates = (1..5).map { playlist("a$it", "A") } + listOf(playlist("b1", "B"), playlist("c1", "C"))
        assertEquals(listOf("a1", "b1", "c1", "a2", "a3"), HomeDiversityPolicy.spread(candidates, 5).map { it.name })
    }
    @Test fun fiveArtistsEachGetAPlaceDespiteOneArtistHavingMoreMatches() {
        val candidates = (1..10).map { playlist("a$it", "A") } + ('B'..'E').map { playlist("$it", "$it") }
        assertEquals(listOf("a1", "B", "C", "D", "E"), HomeDiversityPolicy.spread(candidates, 5).map { it.name })
    }
    @Test fun genuineMixedPlaylistIsPreferredWithinItsArtistGroup() {
        val items = listOf(playlist("focused", "A"), playlist("mixed", "A", true), playlist("b", "B"))
        assertEquals(listOf("mixed", "b", "focused"), HomeDiversityPolicy.spread(items).map { it.name })
    }
    @Test fun albumsAndPlaylistsShareTheSameArtistTurn() {
        val items = listOf(HomeItem.Album("A album", "", "", "Artist A"), playlist("A mix", "artist a"), playlist("B mix", "Artist B"))
        assertEquals(listOf("A album", "B mix", "A mix"), HomeDiversityPolicy.spread(items).map { it.name })
    }
    @Test fun olderSavedThisIsAndMixCardsStillGroupWithoutNewMetadata() {
        val items = listOf(HomeItem.Playlist("This Is Warren Zeiders", "", "", "a"), HomeItem.Playlist("Warren Zeiders Mix", "", "", "b"), HomeItem.Playlist("This Is Tyler Nance", "", "", "c"))
        assertEquals(listOf("a", "c", "b"), HomeDiversityPolicy.spread(items).filterIsInstance<HomeItem.Playlist>().map { it.id })
    }
    @Test fun spotifyBackfillPrefersArtistsNotAlreadyShown() {
        val result = HomeDiversityPolicy.spread(listOf(playlist("a2", "A"), playlist("b", "B"), playlist("c", "C")), 2, alreadyShown = listOf(playlist("a1", "A")))
        assertEquals(listOf("b", "c"), result.map { it.name })
    }
    @Test fun collaborativeCreditsDoNotFalselyMakeASingleArtistPlaylistMixed() {
        val profile = HomeDiversityPolicy.profile("A", List(6) { listOf("A", "Guest $it") })
        assertEquals("a", profile.artistKey); assertFalse(profile.mixed)
    }
    @Test fun mixedClassificationRequiresAnActualVarietyOfSampledArtists() {
        val profile = HomeDiversityPolicy.profile("Seed", listOf(listOf("A"),listOf("B"),listOf("C"),listOf("D")))
        assertEquals("seed", profile.artistKey); assertTrue(profile.mixed)
        assertFalse(HomeDiversityPolicy.profile("A", List(6) { listOf("A") }).mixed)
    }
    @Test fun newUserPreservesSpotifysOriginalCardOrder() {
        val originals = (1..5).map { playlist("a$it", "A") }
        val row = LocalHomePolicy.shelves(listOf(HomeSection("Recommended Stations", originals)), emptyList(), 0.0).single()
        assertEquals(originals.map { it.name }, row.items.map { it.name })
    }
    @Test fun cachedArtistMetadataSurvivesRoundTripAndOlderFormatStillLoads() {
        val item = playlist("mix", "artist", true)
        val feed = HomeFeedModel(sections = listOf(HomeSection("row", listOf(item))))
        val raw = HomeSnapshotCodec.encode("account", "30", feed)
        assertEquals(feed, HomeSnapshotCodec.decode(raw, "account", "30"))
        val legacy = org.json.JSONObject(raw).apply {
            getJSONArray("sections").getJSONObject(0).getJSONArray("items").getJSONObject(0).apply {
                remove("artistKey"); remove("mixedArtists")
            }
        }.toString()
        val restored = HomeSnapshotCodec.decode(legacy, "account", "30")!!.sections.single().items.single() as HomeItem.Playlist
        assertEquals("", restored.artistKey); assertFalse(restored.mixedArtists)
    }
}
