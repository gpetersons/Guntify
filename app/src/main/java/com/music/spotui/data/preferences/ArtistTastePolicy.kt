package com.music.spotui.data.preferences

import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeSection
import java.util.Locale
import kotlin.math.exp

data class ArtistListeningSample(val timestamp: Long, val artists: String, val listenedMs: Long, val source: String)

object ArtistTastePolicy {
    private const val DAY = 86_400_000L
    fun topThree(samples: List<ArtistListeningSample>, now: Long): List<String> {
        data class Weight(val name: String, var score: Double = 0.0, var plays: Int = 0, val days: MutableSet<Long> = mutableSetOf())
        val weights = linkedMapOf<String, Weight>()
        samples.filter { it.source != "spotify" && com.music.spotui.audio.ListeningQualification.qualifies(it.listenedMs) && it.timestamp <= now && now - it.timestamp <= 30L * DAY }.forEach { sample ->
            val age = (now - sample.timestamp) / DAY.toDouble()
            val score = exp(-age / 14.0) * if (age <= 7.0) 3.0 else 1.0
            sample.artists.split(Regex(",| feat\\. | ft\\. ", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.ROOT) }.forEach { artist ->
                val weight = weights.getOrPut(artist.lowercase(Locale.ROOT)) { Weight(artist) }
                weight.score += score; weight.plays++; weight.days += sample.timestamp / DAY
            }
        }
        // A one-day testing spree must not immediately relabel an artist-based section.
        return weights.values.filter { it.plays >= 3 && it.days.size >= 2 }
            .sortedWith(compareByDescending<Weight> { it.score }.thenBy { it.name.lowercase(Locale.ROOT) })
            .take(3).map { it.name }
    }
    fun isMoreLike(title: String) = title.trim().startsWith("More like ", ignoreCase = true) && title.trim().length > 10

    /** Keep each original seeded row unless a complete validated playlist row is available. */
    fun rotateRows(feed: HomeFeedModel, discoveries: List<HomeSection>, day: Long): HomeFeedModel {
        val candidates = discoveries.filter { isMoreLike(it.title) && it.items.size >= 2 }
        if (candidates.isEmpty()) return feed
        var index = 0
        val rows = feed.sections.map { original ->
            if (isMoreLike(original.title) && index < candidates.size) candidates[Math.floorMod(day + index++, candidates.size.toLong()).toInt()] else original
        }
        return feed.copy(sections = if (index == 0) rows + candidates[Math.floorMod(day, candidates.size.toLong()).toInt()] else rows)
    }
    /** A playlist needs meaningful sampled overlap with the seed/related artists, not just a search keyword. */
    fun relevantPlaylist(matches: Int, total: Int, titleMentionsSeed: Boolean): Boolean = total > 0 &&
        matches.toDouble() / total >= 0.15 && (matches >= 2 || (titleMentionsSeed && matches >= 1))
}
