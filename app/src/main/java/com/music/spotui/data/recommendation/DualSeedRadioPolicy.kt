package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SongsModel

/** Keep the original station anchor alongside the latest fifth qualified track. */
object DualSeedRadioPolicy {
    fun seeds(original: String, evolving: String): List<String> =
        listOf(original, evolving).filter { it.isNotBlank() }.distinct()

    fun blend(seeds: List<String>, batches: List<List<SongsModel>>): List<SongsModel> {
        val seen = seeds.toMutableSet()
        val result = mutableListOf<SongsModel>()
        for (index in 0 until (batches.maxOfOrNull { it.size } ?: 0)) {
            for (batch in batches) {
                val song = batch.getOrNull(index) ?: continue
                val key = song.spotifyTrackId.ifBlank { "local:${song.id}" }
                if (seen.add(key)) result += song
            }
        }
        return result
    }
}
