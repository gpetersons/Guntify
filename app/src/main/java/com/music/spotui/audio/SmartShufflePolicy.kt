package com.music.spotui.audio
import com.music.spotui.data.entity.SongsModel

object SmartShufflePolicy {
    /** One suggested track after each three collection tracks, capped at five additions. */
    fun inject(upcoming: List<SongsModel>, suggestions: List<SongsModel>, existingIds: Set<Int>): Pair<List<SongsModel>, Set<Int>> {
        val additions = suggestions.filter { it.id !in existingIds }.distinctBy { it.spotifyTrackId.ifBlank { "local:${it.id}" } }.take(5)
        val result = mutableListOf<SongsModel>()
        val inserted = mutableSetOf<Int>()
        var next = 0
        upcoming.forEachIndexed { i, song ->
            result += song
            if ((i % 3 == 2 || i == upcoming.lastIndex) && next < additions.size) {
                val suggestion = additions[next++]; result += suggestion; inserted += suggestion.id
            }
        }
        return result to inserted
    }
}
