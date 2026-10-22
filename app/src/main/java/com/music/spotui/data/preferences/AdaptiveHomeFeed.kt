package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import java.util.Locale
import kotlin.math.exp

/** Mix Spotify's existing recommendations with new Spotify catalog discoveries.
 *  Keeps Spotify collection IDs and original navigation; never invents playlists.
 *  Recent plays are weighted more heavily and newly discovered cards are introduced
 *  gradually without overwriting the special history or top shortcut sections.
 */
object AdaptiveHomeFeed {
    private fun key(s: String) = s.trim().lowercase(Locale.ROOT)
    private fun id(item: HomeItem): String = when (item) {
        is HomeItem.Playlist -> "playlist:" + item.id
        is HomeItem.Album -> "album:" + key(item.name) + ":" + key(item.artists)
        is HomeItem.Artist -> "artist:" + item.id
        is HomeItem.Radio -> "radio:" + item.seed.id
        is HomeItem.RecentTrack -> "track:" + item.song.id
    }
    fun blend(context: Context, original: HomeFeedModel, discoveries: List<HomeSection>): HomeFeedModel {
        val prefs = HomePersonalization
        val ranked = prefs.rank(context, original)
        if (!prefs.enabled(context) || prefs.strength(context) == 0) return ranked
        val now = System.currentTimeMillis()
        val window = prefs.windowDays(context)
        val plays = getListeningHistory(context).filter {
            it.source != "spotify" && com.music.spotui.audio.ListeningQualification.qualifies(it.listenedMs) && (window <= 0 || now - it.ts <= window.toLong() * 86_400_000L)
        }
        if (plays.size < 3 || discoveries.isEmpty()) return ranked
        val artistWeights = mutableMapOf<String, Double>()
        val albumWeights = mutableMapOf<String, Double>()
        plays.forEach { play ->
            val weight = exp(-((now - play.ts).coerceAtLeast(0L) / 86_400_000.0) / 45.0)
            play.singer.split(",", " & ", " feat. ", " ft. ").map(::key).filter { it.length > 2 }.forEach {
                artistWeights[it] = (artistWeights[it] ?: 0.0) + weight
            }
            key(play.album).takeIf { it.length > 2 }?.let { album ->
                albumWeights[album] = (albumWeights[album] ?: 0.0) + weight
            }
        }
        fun score(item: HomeItem): Double {
            val words = when(item) {
                is HomeItem.Album -> key(item.name + " " + item.artists)
                is HomeItem.Playlist -> key(item.name + " " + item.subtitle)
                is HomeItem.RecentTrack -> key(item.song.singer + " " + item.song.album + " " + item.name)
                else -> key(item.name)
            }
            return artistWeights.entries.sumOf { (name, weight) -> if (words.contains(name)) weight else 0.0 } +
                albumWeights.entries.sumOf { (name, weight) -> if (words.contains(name)) weight * 1.5 else 0.0 }
        }
        val pool = discoveries.filterNot { ArtistTastePolicy.isMoreLike(it.title) }.flatMap { it.items }.distinctBy(::id)
        val used = ranked.sections.flatMap { it.items }.map(::id).toMutableSet()
        // The Spotify section title is authoritative. Never rename, remove, or reorder sections.
        // Increase local discovery toward the share selected by the user.
        val localFraction = HomeMixPolicy.fraction(plays.size, prefs.strength(context))
        val sections = ranked.sections.map { section ->
            // Only positively identified generic sections accept history-based discoveries.
            // Titles such as "More like Eli" carry a seed we cannot safely infer from the text.
            if (!HomeSectionPolicy.canPersonalize(section.title) || section.items.size < 2) return@map section
            val originalKinds = section.items
            if (originalKinds.isEmpty()) return@map section
            fun kind(item: HomeItem) = when (item) {
                is HomeItem.Album -> "album"
                is HomeItem.Playlist -> "playlist"
                is HomeItem.Artist -> "artist"
                is HomeItem.RecentTrack -> "track"
                is HomeItem.Radio -> "radio"
            }
            val kinds = originalKinds.map(::kind).toSet()
            val candidates = pool.filter { candidate ->
                id(candidate) !in used && kind(candidate) in kinds
            }.sortedWith(compareByDescending<HomeItem>(::score).thenBy { id(it) })
            val count = HomeMixPolicy.replacementCount(section.items.size, localFraction, candidates.size)
            if (count == 0) return@map section
            val additions = candidates.take(count)
            additions.forEach { used.add(id(it)) }
            val kept = section.items.take(section.items.size - count)
            val merged = (kept + additions).distinctBy(::id)
            section.copy(items = merged.withIndex().sortedWith(
                compareByDescending<IndexedValue<HomeItem>> { score(it.value) }
                    .thenBy { it.index }
            ).map { it.value })
        }
        // Keep unused discoveries available as dedicated sections, too.
        val extras = discoveries.filterNot { ArtistTastePolicy.isMoreLike(it.title) }.mapNotNull { section ->
            section.copy(items = section.items.filter { id(it) !in used }.take(12))
                .takeIf { it.items.isNotEmpty() }
        }
        return ArtistTastePolicy.rotateRows(ranked.copy(sections = sections + extras), discoveries, java.time.LocalDate.now().toEpochDay())
    }
}
