package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import java.util.Locale
import kotlin.math.exp

data class LocalTasteSample(val trackId: String, val artists: String, val timestamp: Long, val listenedMs: Long, val source: String)

object LocalHomePolicy {
    const val ALBUMS = "local-home-albums"
    const val PLAYLISTS = "local-home-playlists"
    const val SONG_RADIOS = "local-home-song-radios"
    private const val DAY = 86_400_000L
    fun qualifying(samples: List<LocalTasteSample>, now: Long, windowDays: Int = 30) = samples.filter {
        it.source != "spotify" && com.music.spotui.audio.ListeningQualification.qualifies(it.listenedMs) && it.timestamp <= now && (windowDays <= 0 || now - it.timestamp <= windowDays.toLong() * DAY)
    }
    private fun weight(sample: LocalTasteSample, now: Long): Double {
        val age = (now - sample.timestamp) / DAY.toDouble()
        return exp(-age / 14.0) * if (age <= 7.0) 3.0 else 1.0
    }
    fun topTracks(samples: List<LocalTasteSample>, now: Long, windowDays: Int = 30): List<String> = qualifying(samples, now, windowDays)
        .filter { it.trackId.isNotBlank() }.groupBy { it.trackId }.entries
        .sortedWith(compareByDescending<Map.Entry<String, List<LocalTasteSample>>> { it.value.sumOf { sample -> weight(sample, now) } }.thenBy { it.key })
        .take(15).map { it.key }

    fun topArtists(samples: List<LocalTasteSample>, now: Long, windowDays: Int = 30): List<String> {
        val names = linkedMapOf<String, String>()
        val scores = linkedMapOf<String, Double>()
        qualifying(samples, now, windowDays).forEach { sample ->
            sample.artists.split(Regex(",| feat\\. | ft\\. ", RegexOption.IGNORE_CASE)).map { it.trim() }
                .filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.ROOT) }.forEach { name ->
                    val key = name.lowercase(Locale.ROOT)
                    names.putIfAbsent(key, name)
                    scores[key] = (scores[key] ?: 0.0) + weight(sample, now)
                }
        }
        return scores.entries.sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .take(5).map { names.getValue(it.key) }
    }

    private fun kind(title: String): String? = when {
        title.trim().startsWith("Made for ", true) -> ALBUMS
        title.trim().lowercase(Locale.ROOT) in setOf("recommended stations", "recommended radios", "recommended playlists") -> PLAYLISTS
        title.trim().equals("Popular radio", true) -> SONG_RADIOS
        else -> null
    }
    private fun id(item: HomeItem): String = when (item) {
        is HomeItem.Album -> "album:${item.name.lowercase(Locale.ROOT)}|${item.artists.lowercase(Locale.ROOT)}"
        is HomeItem.Radio -> "radio:${item.seed.spotifyTrackId}"
        is HomeItem.Playlist -> "playlist:${item.id}"
        is HomeItem.Artist -> "artist:${item.id}"
        is HomeItem.RecentTrack -> "track:${item.song.spotifyTrackId}"
    }
    /** Three concise shelves plus the separately required Recently played row. */
    fun shelves(original: List<HomeSection>, discoveries: List<HomeSection>, fraction: Double, accountName: String? = null): List<HomeSection> {
        val madeForTitle = accountName?.trim()?.takeIf { it.isNotBlank() }?.let { "Made For $it" } ?: "Made for you"
        val defaults = listOf(madeForTitle to ALBUMS, "Recommended playlists" to PLAYLISTS, "Popular radio" to SONG_RADIOS)
        val used = mutableSetOf<String>()
        val knownArtists = HomeDiversityPolicy.knownArtists(discoveries.flatMap { it.items })
        fun allowed(item: HomeItem, category: String) = when (category) {
            ALBUMS -> item is HomeItem.Album || (item is HomeItem.Playlist && item.id.isNotBlank())
            PLAYLISTS -> item is HomeItem.Playlist && item.id.isNotBlank()
            else -> item is HomeItem.Radio || (item is HomeItem.Playlist && item.id.isNotBlank())
        }
        val result = defaults.mapNotNull { (fallbackTitle, category) ->
            val row = original.firstOrNull { kind(it.title) == category }
            val local = HomeDiversityPolicy.spread(discoveries.firstOrNull { it.title == category }?.items.orEmpty()
                .filter { allowed(it, category) && id(it) !in used }.distinctBy(::id), known = knownArtists).take(5)
            // New users can use Spotify's existing feed playlists even when its station row is absent.
            val fallback = when {
                category == PLAYLISTS -> original.flatMap { it.items }.filterIsInstance<HomeItem.Playlist>()
                category == ALBUMS && row == null -> original.filter { kind(it.title) !in setOf(PLAYLISTS, SONG_RADIOS) }
                    .flatMap { it.items }.filter { it is HomeItem.Album || it is HomeItem.Playlist }
                else -> emptyList()
            }
            val originals = row?.items.orEmpty() + fallback
            val spotify = originals.filter { allowed(it, category) && id(it) !in used }.map { item ->
                if (item is HomeItem.Playlist) when (category) {
                    PLAYLISTS -> item.copy(subtitle = "Spotify playlist")
                    SONG_RADIOS -> item.copy(subtitle = "Spotify Radio playlist")
                    else -> item
                } else item
            }.distinctBy(::id)
            val share = fraction.coerceIn(0.0, 1.0)
            val count = com.music.spotui.data.preferences.HomeMixPolicy.replacementCount(5, share, local.size)
            val additions = if (spotify.isEmpty() && share > 0.0) local else local.take(count)
            val remaining = spotify.filter { old -> additions.none { id(it) == id(old) } }
            val backfill = if (additions.isEmpty()) remaining.take(5) else HomeDiversityPolicy.spread(remaining, 5 - additions.size, knownArtists, additions)
            val selected = (additions + backfill).distinctBy(::id).take(5)
            val merged = if (additions.isEmpty()) selected else HomeDiversityPolicy.spread(selected, known = knownArtists)
            merged.forEach { used.add(id(it)) }
            val title = if (category == PLAYLISTS) fallbackTitle else row?.title ?: fallbackTitle
            if (merged.isEmpty()) null else HomeSection(title, merged)
        }
        val recentlyPlayed = original.firstOrNull { it.title.equals("Recently played", true) && it.items.isNotEmpty() }
        return listOfNotNull(recentlyPlayed) + result
    }
}
