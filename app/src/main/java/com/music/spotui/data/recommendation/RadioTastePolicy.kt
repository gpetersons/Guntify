package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.SongsModel
import java.util.Locale

object RadioTastePolicy {
    fun allowed(song: SongsModel, dislikes: Set<String>, blocked: Set<String>): Boolean {
        if (song.spotifyTrackId.ifBlank { "local:${song.id}" } in dislikes) return false
        return song.singer.lowercase(Locale.ROOT).split(Regex(",| feat\\. | ft\\. ")).map { it.trim() }.none { it in blocked }
    }
    fun blend(seeds: List<String>, batches: List<List<SongsModel>>, mode: String): List<SongsModel> =
        RadioMixPolicy.blend(seeds,batches,minOf(1,batches.size),mode)
}
