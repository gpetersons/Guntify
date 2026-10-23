package com.music.spotui.data.recommendation

import android.content.Context
import com.music.spotui.data.entity.*
import com.music.spotui.data.preferences.*

/** Small render-ready snapshot: no preference reads or ranking are needed to display it. */
data class HomePresentation(val topTiles: List<HomeItem>, val sections: List<HomeSection>, val hasLocalActivity: Boolean)

object HomePresentationPolicy {
    fun prepare(feed: HomeFeedModel, discovered: List<HomeSection>, localItems: List<HomeItem>, frequent: List<HomeItem>,
                hidden: Set<String>, fraction: Double, accountName: String?, maxRecent: Int,
                recentActivity: List<HomeItem> = localItems): HomePresentation {
        fun visible(item: HomeItem) = HomeHiddenItems.key(item) !in hidden
        val local = localItems.filter(::visible)
        val sections = feed.sections.map { it.copy(items = it.items.filter(::visible).distinctBy(HomeHiddenItems::key)) }
        val shortcuts = HomeLayoutPolicy.spotifyCollections(feed).filter(::visible)
        val rotation = HomeLayoutPolicy.rotation((local.take(6) + frequent + local.drop(6)).filter(::visible), shortcuts)
        val carousels = if (sections.firstOrNull()?.title?.isBlank() == true) sections.drop(1) else sections
        val recentIndex = carousels.indexOfFirst { it.title.equals("Recently played", true) }
        val localTracks = recentActivity.filterIsInstance<HomeItem.RecentTrack>().filter(::visible)
        val fallbackTracks = carousels.getOrNull(recentIndex)?.items.orEmpty().filterIsInstance<HomeItem.RecentTrack>()
        val recent = localTracks.ifEmpty { fallbackTracks }.distinctBy(HomeHiddenItems::key).take(maxRecent)
        val withRecent = when {
            recent.isEmpty() -> carousels.filterNot { it.title.equals("Recently played", true) }
            recentIndex >= 0 -> carousels.mapIndexed { i, row -> if (i == recentIndex) row.copy(items = recent) else row }
            else -> listOf(HomeSection("Recently played", recent)) + carousels
        }
        val discoveries = discovered.map { it.copy(items = it.items.filter(::visible)) }
        val rows = LocalHomePolicy.shelves(withRecent, discoveries, fraction, accountName)
        return HomePresentation(rotation, HomePolishPolicy.refine(rows.map { it.copy(items = it.items.distinctBy(HomeHiddenItems::key)) }), local.isNotEmpty() || localTracks.isNotEmpty())
    }
}

/** Called on Dispatchers.IO by HomeViewModel, never during Compose rendering. */
object HomePresentationBuilder {
    fun build(context: Context, feed: HomeFeedModel, discovered: List<HomeSection>, accountName: String?): HomePresentation {
        val hidden = context.getSharedPreferences("home_hidden_items", Context.MODE_PRIVATE)
            .getStringSet("keys", emptySet()).orEmpty().toSet()
        val showPlaylists = HomeRecents.showPlaylists(context)
        val showRadios = HomeRecents.showRadios(context)
        val radios = if (showRadios) RadioSessions.get(context).map { HomeItem.Radio(it) } else emptyList()
        val collections = HomeFrequency.recentActivity(context, radios, showPlaylists, showRadios)
        val local = collections.map { it.first }.take(500)
        val history = getListeningHistory(context)
        val tracks = history.filter {
            it.source != "spotify" && com.music.spotui.audio.ListeningQualification.qualifies(it.listenedMs) &&
                it.title.isNotBlank() && it.url.isNotBlank()
        }.map { entry ->
            HomeItem.RecentTrack(SongsModel(entry.songId, entry.title, entry.album, entry.singer,
                entry.image, entry.url, entry.spotifyTrackId, durationMs = entry.durationMs)) to entry.ts
        }
        val recent = tracks.sortedByDescending { it.second }.map { it.first }
        val shortcuts = HomeLayoutPolicy.spotifyCollections(feed).filter { HomeHiddenItems.key(it) !in hidden }
        HomeFrequency.seed(context, shortcuts)
        val frequent = HomeFrequency.top(context, radios, showPlaylists, showRadios)
        val prefs = HomePersonalization
        val fraction = if (!prefs.enabled(context)) 0.0 else {
            val samples = history.map { LocalTasteSample(it.spotifyTrackId, it.singer, it.ts, it.listenedMs, it.source) }
            HomeMixPolicy.fraction(LocalHomePolicy.qualifying(samples, System.currentTimeMillis(), prefs.windowDays(context)).size, prefs.strength(context))
        }
        val ready=HomePresentationPolicy.prepare(feed, discovered, local, frequent, hidden, fraction, accountName, HomeRecents.maxItems(context), recent)
        val saved=radios.filter { HomeHiddenItems.key(it) !in hidden }.distinctBy { HomeHiddenItems.key(it) }.take(20)
        return if(saved.isEmpty())ready else ready.copy(sections=ready.sections+HomeSection("Your USpot radios",saved))
    }
}
