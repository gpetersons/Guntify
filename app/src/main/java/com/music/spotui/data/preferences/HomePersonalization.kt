package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeItem
import java.util.Locale
import kotlin.math.exp

/** On-device, deterministic ranking of Spotify's existing Home catalog. */
object HomePersonalization {
    val revision = kotlinx.coroutines.flow.MutableStateFlow(0L)
    private const val PREF = "home_personalization"
    fun enabled(context: Context) = context.getSharedPreferences(PREF, 0).getBoolean("enabled", true)
    fun setEnabled(context: Context, value: Boolean) { context.getSharedPreferences(PREF, 0).edit().putBoolean("enabled", value).apply(); revision.value++ }
    fun strength(context: Context) = context.getSharedPreferences(PREF, 0).getInt("strength", 100).coerceIn(0, 100)
    fun setStrength(context: Context, value: Int) { context.getSharedPreferences(PREF, 0).edit().putInt("strength", value.coerceIn(0, 100)).apply(); revision.value++ }
    fun windowDays(context: Context) = context.getSharedPreferences(PREF, 0).getInt("window", 30)
    fun setWindowDays(context: Context, value: Int) { context.getSharedPreferences(PREF, 0).edit().putInt("window", value).apply(); revision.value++ }

    fun rank(context: Context, feed: HomeFeedModel): HomeFeedModel {
        if (!enabled(context) || strength(context) == 0) return feed
        val now = System.currentTimeMillis()
        val days = windowDays(context)
        val entries = getListeningHistory(context).filter { entry ->
            entry.source != "spotify" && com.music.spotui.audio.ListeningQualification.qualifies(entry.listenedMs) && (days <= 0 || now - entry.ts <= days * 86_400_000L)
        }
        if (entries.isEmpty()) return feed // Preserve Spotify's original order until Spotui learns preferences.
        val artists = mutableMapOf<String, Double>()
        val albums = mutableMapOf<String, Double>()
        fun key(s: String) = s.trim().lowercase(Locale.ROOT)
        for (entry in entries) {
            val ageDays = ((now - entry.ts).coerceAtLeast(0L) / 86_400_000.0)
            val weight = exp(-ageDays / 45.0)
            entry.singer.split(",", "&", " feat. ", " ft. ").forEach { artist ->
                if (artist.isNotBlank()) artists[key(artist)] = (artists[key(artist)] ?: 0.0) + weight
            }
            if (entry.album.isNotBlank()) albums[key(entry.album)] = (albums[key(entry.album)] ?: 0.0) + weight
        }
        val factor = strength(context) / 100.0
        fun score(item: HomeItem): Double {
            val name = key(item.name)
            val searchable = when (item) {
                is HomeItem.Album -> key(item.artists + " " + item.subtitle + " " + item.name)
                is HomeItem.Playlist -> key(item.name + " " + item.subtitle)
                else -> name
            }
            val artistScore = artists.entries.sumOf { (artist, weight) ->
                if (artist.length > 2 && searchable.contains(artist)) weight else 0.0
            }
            val albumScore = albums.entries.sumOf { (album, weight) ->
                if (album.length > 2 && searchable.contains(album)) weight else 0.0
            }
            return factor * (artistScore + 1.4 * albumScore)
        }
        return HomeSectionPolicy.personalize(feed) { section ->
            // Do not rearrange the top shortcut grid; it has its own frequency rules.
            if (section.title.isBlank()) section
            else section.copy(items = section.items.withIndex().sortedWith(
                compareByDescending<IndexedValue<HomeItem>> { score(it.value) }
                    .thenBy { it.index }
            ).map { it.value })
        }
    }
}
