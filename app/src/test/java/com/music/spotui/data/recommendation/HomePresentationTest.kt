package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import com.music.spotui.data.preferences.HomeHiddenItems
import org.junit.Assert.*
import org.junit.Test

class HomePresentationTest {
    private fun playlist(id: Int) = HomeItem.Playlist("Mix $id", "art", "Playlist", "id$id")
    private fun songCard(id: Int) = HomeItem.RecentTrack(SongsModel(id,"Song $id","Album","Artist","art","q$id"))
    private fun feed(items: List<HomeItem>) = HomeFeedModel(sections = listOf(HomeSection("", items), HomeSection("Made For listener", items)))
    private fun prepare(feed: HomeFeedModel, local: List<HomeItem> = emptyList(), frequent: List<HomeItem> = emptyList(), hidden: Set<String> = emptySet(), max: Int = 8) =
        HomePresentationPolicy.prepare(feed, emptyList(), local, frequent, hidden, 0.0, "listener", max)

    @Test fun hiddenRotationCardsBackfillFromOlderCollectionsBeforeLimit() {
        val items = (1..12).map(::playlist)
        val ready = prepare(feed(items), items, items.take(2), setOf(HomeHiddenItems.key(items.first())))
        assertEquals((2..9).map(::playlist), ready.topTiles)
        assertFalse(ready.sections.flatMap { it.items }.contains(items.first()))
    }
    @Test fun individualSongsNeverDisplaceRotationCollections() {
        val track = HomeItem.RecentTrack(SongsModel(1,"Song","Album","Artist","art","url"))
        val ready = prepare(feed(listOf(track, playlist(1))), listOf(track))
        assertEquals(listOf(playlist(1)), ready.topTiles)
    }
    @Test fun newUserRetainsSpotifyFallbackAndHasNoLocalActivity() {
        val items = (1..10).map(::playlist)
        val ready = prepare(feed(items))
        assertEquals(items.take(8), ready.topTiles)
        assertFalse(ready.hasLocalActivity)
        assertEquals(items.take(5), ready.sections.first().items)
    }
    @Test fun collectionHistoryDoesNotCreateLargeRecentCards() {
        val base = feed((1..5).map(::playlist)).copy(sections = listOf(HomeSection("Recently played", listOf(playlist(99)))) + feed((1..5).map(::playlist)).sections)
        val ready = prepare(base, (10..15).map(::playlist), max = 2)
        assertTrue(ready.sections.none { it.title == "Recently played" })
        assertEquals(playlist(10), ready.topTiles.first())
        assertTrue(ready.hasLocalActivity)
    }
    @Test fun hiddenRecentSongsCannotReappearOrConsumeRecentLimit() {
        val songs = (10..15).map(::songCard)
        val ready = HomePresentationPolicy.prepare(feed((1..5).map(::playlist)), emptyList(), emptyList(), emptyList(),
            setOf(HomeHiddenItems.key(songs.first())), 0.0, "listener", 2, songs)
        assertEquals(listOf(songCard(11), songCard(12)), ready.sections.first().items)
    }
    @Test fun duplicateCatalogItemsCannotCreateDuplicateLazyRowKeys() {
        val p = playlist(1)
        val ready = prepare(feed(listOf(p,p)), listOf(p,p))
        ready.sections.forEach { row -> assertEquals(row.items.size, row.items.map(HomeHiddenItems::key).distinct().size) }
    }
    @Test fun localRecommendationsStillReplaceSpotifyAtFullInfluence() {
        val album = HomeItem.Album("Local album","art","Artist","Artist")
        val ready = HomePresentationPolicy.prepare(HomeFeedModel(), listOf(HomeSection(LocalHomePolicy.ALBUMS,listOf(album))), emptyList(), emptyList(), emptySet(), 1.0, "account",8)
        assertEquals("Made For account", ready.sections.single().title)
        assertEquals(listOf(album), ready.sections.single().items)
    }
    @Test fun hiddenDiscoveryDoesNotReappearThroughPersonalization() {
        val p = playlist(1)
        val ready = HomePresentationPolicy.prepare(HomeFeedModel(), listOf(HomeSection(LocalHomePolicy.PLAYLISTS,listOf(p))), emptyList(), emptyList(), setOf(HomeHiddenItems.key(p)),1.0,"account",8)
        assertTrue(ready.sections.isEmpty())
    }
    @Test fun newCollectionAppearsBeforeOldLifetimeFavorites() {
        val fresh = playlist(20)
        val ready = prepare(feed((1..10).map(::playlist)), listOf(fresh), listOf(playlist(1), playlist(2)))
        assertEquals(fresh, ready.topTiles.first())
    }
    @Test fun qualifiedSongsOnlyAppearInSeparateRecentRow() {
        val track = HomeItem.RecentTrack(SongsModel(1,"Song","Album","Artist","art","url"))
        val ready = HomePresentationPolicy.prepare(feed(listOf(playlist(1))), emptyList(), listOf(playlist(20)),
            emptyList(), emptySet(), 0.0, "listener", 8, listOf(track, playlist(20)))
        assertTrue(ready.topTiles.none { it is HomeItem.RecentTrack })
        assertEquals(listOf(track), ready.sections.first().items)
    }
    @Test fun localSongsRenderWithoutAnySpotifyFeed() {
        val track = HomeItem.RecentTrack(SongsModel(1,"Song","Album","Artist","art","url"))
        val ready = HomePresentationPolicy.prepare(HomeFeedModel(), emptyList(), emptyList(), emptyList(),
            emptySet(), 0.0, "listener", 8, listOf(track))
        assertTrue(ready.topTiles.isEmpty())
        assertTrue(ready.hasLocalActivity)
        assertEquals(listOf(track), ready.sections.single().items)
    }
    @Test fun hiddenAndDuplicateSongsDoNotUseUpTheRecentLimit() {
        val track = HomeItem.RecentTrack(SongsModel(1,"Song","Album","Artist","art","url"))
        val visible = track.copy(song = track.song.copy(id = 2))
        val ready = HomePresentationPolicy.prepare(HomeFeedModel(), emptyList(), emptyList(), emptyList(),
            setOf(HomeHiddenItems.key(track)), 0.0, "listener", 2, listOf(track, visible, visible, playlist(20), songCard(3)))
        assertEquals(listOf(visible, songCard(3)), ready.sections.single { it.title == "Recently played" }.items)
    }
    @Test fun spotifyFallbackRecentRowAlsoExcludesCollections() {
        val base = feed((1..8).map(::playlist)).copy(sections = listOf(HomeSection("Recently played", listOf(playlist(20), songCard(1), HomeItem.Album("Album","art","Artist","Artist")))) + feed((1..8).map(::playlist)).sections)
        val ready = prepare(base)
        assertEquals(listOf(songCard(1)), ready.sections.first { it.title == "Recently played" }.items)
        assertEquals(8, ready.topTiles.size)
        assertFalse(ready.hasLocalActivity)
    }
    @Test fun radioAndAlbumStayInRotationButNeverInLargeRecentRow() {
        val radio = HomeItem.Radio(songCard(1).song)
        val album = HomeItem.Album("Album","art","Artist","Artist")
        val ready = HomePresentationPolicy.prepare(HomeFeedModel(), emptyList(), listOf(radio, album, playlist(1)), emptyList(), emptySet(), 0.0, "listener", 8,
            listOf(radio, album, playlist(1), songCard(2)))
        assertEquals(listOf(radio, album, playlist(1)), ready.topTiles)
        assertEquals(listOf(songCard(2)), ready.sections.single { it.title == "Recently played" }.items)
    }

}
