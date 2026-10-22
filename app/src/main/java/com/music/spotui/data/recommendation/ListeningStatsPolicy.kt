package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.HistoryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class RankedListeningSong(val song: SongsModel, val plays: Int)
data class RankedListeningArtist(val name: String, val plays: Int, val artwork: String)
data class ListeningDay(val date: LocalDate, val plays: Int)
data class ListeningSummary(
    val plays: Int, val recordedMs: Long, val artistCount: Int,
    val songs: List<RankedListeningSong>, val artists: List<RankedListeningArtist>, val activity: List<ListeningDay>,
)

/** Stats use actual qualified play counts, rather than recommendation weighting. */
object ListeningStatsPolicy {
    fun summary(history: List<HistoryEntry>, now: Long, days: Int, zone: ZoneId = ZoneId.systemDefault()): ListeningSummary {
        val entries = ListeningTastePolicy.qualified(history, now, days)
        val songs = entries.groupBy { it.spotifyTrackId.ifBlank { "local:${it.songId}" } }
            .map { (_, visits) ->
                val entry = visits.filter { it.url.isNotBlank() }.maxByOrNull { it.ts } ?: visits.maxBy { it.ts }
                RankedListeningSong(SongsModel(entry.songId, entry.title, entry.album, entry.singer,
                    entry.image, entry.url, entry.spotifyTrackId, durationMs = entry.durationMs), visits.size)
            }.sortedWith(compareByDescending<RankedListeningSong> { it.plays }.thenBy { it.song.title.lowercase(Locale.ROOT) })
        val artistVisits = linkedMapOf<String, MutableList<Pair<String, HistoryEntry>>>()
        entries.forEach { entry ->
            entry.singer.split(Regex(",| feat\\. | ft\\. ", RegexOption.IGNORE_CASE)).map { it.trim() }
                .filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.ROOT) }.forEach { name ->
                    artistVisits.getOrPut(name.lowercase(Locale.ROOT)) { mutableListOf() }.add(name to entry)
                }
        }
        val artists = artistVisits.values.map { visits ->
            val latest = visits.maxBy { it.second.ts }
            val artwork = visits.sortedByDescending { it.second.ts }.firstOrNull { it.second.image.isNotBlank() }?.second?.image.orEmpty()
            RankedListeningArtist(latest.first, visits.size, artwork)
        }.sortedWith(compareByDescending<RankedListeningArtist> { it.plays }.thenBy { it.name.lowercase(Locale.ROOT) })
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val perDay = entries.groupingBy { Instant.ofEpochMilli(it.ts).atZone(zone).toLocalDate() }.eachCount()
        val activity = (6L downTo 0L).map { offset -> today.minusDays(offset).let { ListeningDay(it, perDay[it] ?: 0) } }
        return ListeningSummary(entries.size, entries.sumOf { it.listenedMs }, artists.size, songs, artists, activity)
    }
}
