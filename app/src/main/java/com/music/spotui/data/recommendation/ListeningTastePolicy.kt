package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.HistoryEntry
import kotlin.math.exp

object ListeningTastePolicy {
    private const val DAY = 86_400_000L
    fun qualified(history: List<HistoryEntry>, now: Long, days: Int = 30) = history.filter {
        it.source != "spotify" && com.music.spotui.audio.ListeningQualification.qualifies(it.listenedMs) && it.ts <= now && (days <= 0 || now - it.ts <= days * DAY)
    }
    fun topSongs(history: List<HistoryEntry>, now: Long, count: Int, liked: Set<Int> = emptySet(), days: Int = 30): List<SongsModel> =
        qualified(history, now, days).filter { it.url.isNotBlank() }.groupBy { it.spotifyTrackId.ifBlank { "local:${it.songId}" } }
            .entries.sortedWith(compareByDescending<Map.Entry<String, List<HistoryEntry>>> { (_, plays) ->
                plays.sumOf { val age = (now - it.ts) / DAY.toDouble(); exp(-age / 14) * (if (age <= 7) 3 else 1) * (if (it.songId in liked) 1.25 else 1.0) }
            }.thenBy { it.key }).take(count.coerceIn(0, 100)).map { (_, plays) ->
                val p = plays.maxBy { it.ts }
                SongsModel(p.songId, p.title, p.album, p.singer, p.image, p.url, p.spotifyTrackId, durationMs = p.durationMs)
            }
    fun artistCounts(history: List<HistoryEntry>, now: Long, days: Int): List<Pair<String, Int>> =
        qualified(history, now, days).flatMap { it.singer.split(Regex(",| feat\\. | ft\\. ", RegexOption.IGNORE_CASE)).map(String::trim).filter(String::isNotBlank).distinct() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key to it.value }
}
