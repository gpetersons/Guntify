package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.HomeItem
import java.util.Locale

data class PlaylistArtistProfile(val artistKey: String, val mixed: Boolean)

/** Stable variety: retain taste order for ties, but give each artist a turn first. */
object HomeDiversityPolicy {
    fun normalize(name: String) = name.trim().lowercase(Locale.ROOT)
    private fun primary(name: String) = normalize(name.split(Regex(",| feat\\. | ft\\. ", RegexOption.IGNORE_CASE)).first())
    fun profile(seed: String, artistsPerTrack: List<List<String>>): PlaylistArtistProfile {
        val tracks = artistsPerTrack.map { names -> names.map(::normalize).filter { it.isNotBlank() }.distinct() }.filter { it.isNotEmpty() }
        val counts = tracks.flatten().groupingBy { it }.eachCount()
        val dominant = counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).firstOrNull()
        val mixed = counts.size >= 3 && dominant != null && dominant.value * 2 < tracks.size
        return PlaylistArtistProfile(if (mixed || dominant == null) normalize(seed) else dominant.key, mixed)
    }
    fun knownArtists(items: List<HomeItem>): Set<String> = items.mapNotNull { item ->
        when (item) {
            is HomeItem.Playlist -> item.artistKey.takeIf { it.isNotBlank() }
            is HomeItem.Album -> primary(item.artists).takeIf { it.isNotBlank() }
            is HomeItem.Radio -> primary(item.seed.singer).takeIf { it.isNotBlank() }
            is HomeItem.Artist -> normalize(item.name).takeIf { it.isNotBlank() }
            is HomeItem.RecentTrack -> primary(item.song.singer).takeIf { it.isNotBlank() }
        }
    }.map(::normalize).toSet()
    private fun group(item: HomeItem, known: Set<String>): String = when (item) {
        is HomeItem.Playlist -> item.artistKey.takeIf { it.isNotBlank() }?.let(::normalize)
            ?: known.sortedByDescending { it.length }.firstOrNull { name ->
                Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(name) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE).containsMatchIn(item.name)
            }
            ?: Regex("^(?:this is (.+)|(.+) (?:mix|radio))$", RegexOption.IGNORE_CASE).matchEntire(item.name.trim())
                ?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.let(::normalize)
            ?: "playlist:${item.id.ifBlank { item.name }}"
        is HomeItem.Album -> primary(item.artists).ifBlank { "album:${item.name}" }
        is HomeItem.Radio -> primary(item.seed.singer).ifBlank { "radio:${item.seed.id}" }
        is HomeItem.Artist -> normalize(item.name)
        is HomeItem.RecentTrack -> primary(item.song.singer)
    }
    fun spread(items: List<HomeItem>, limit: Int = items.size, known: Set<String> = knownArtists(items), alreadyShown: List<HomeItem> = emptyList()): List<HomeItem> {
        val normalized = known.map(::normalize).toSet()
        val groups = items.distinct().groupBy { group(it, normalized) }.mapValues { (_, group) ->
            group.sortedBy { if (it is HomeItem.Playlist && it.mixedArtists) 0 else 1 }.toMutableList()
        }
        val used = alreadyShown.groupingBy { group(it, normalized) }.eachCount().toMutableMap()
        val result = mutableListOf<HomeItem>()
        repeat(limit.coerceAtLeast(0).coerceAtMost(items.size)) {
            val next = groups.entries.filter { it.value.isNotEmpty() }.minByOrNull { used[it.key] ?: 0 } ?: return result
            result += next.value.removeAt(0)
            used[next.key] = (used[next.key] ?: 0) + 1
        }
        return result
    }
}
