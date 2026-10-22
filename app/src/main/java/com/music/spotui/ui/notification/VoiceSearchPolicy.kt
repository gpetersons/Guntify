package com.music.spotui.ui.notification

import com.music.spotui.data.entity.SongsModel
import java.text.Normalizer

object VoiceSearchPolicy {
    const val PLAY_ACTION = "com.music.spotui.action.VOICE_PLAY"
    data class Request(val raw: String, val title: String = "", val artist: String = "", val album: String = "") {
        val query: String get() {
            val content = title.ifBlank { raw }.trim()
            val extraArtist = artist.takeUnless { it.isBlank() || content.contains(it, ignoreCase = true) }.orEmpty()
            return listOf(content, extraArtist).filter { it.isNotBlank() }.joinToString(" ").trim().take(200)
        }
    }
    private fun normalized(value: String) = Normalizer.normalize(value.lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    fun rank(request: Request, songs: List<SongsModel>): List<SongsModel> {
        fun score(song: SongsModel): Int {
            val title = normalized(song.title)
            val artist = normalized(song.singer)
            val wantedTitle = normalized(request.title)
            val wantedArtist = normalized(request.artist)
            val raw = normalized(request.raw)
            return (if (wantedTitle.isNotEmpty() && title == wantedTitle) 100 else 0) +
                (if (wantedArtist.isNotEmpty() && (" $artist ").contains(" $wantedArtist ")) 100 else 0) +
                (if (raw.isNotEmpty() && title == raw) 50 else 0) +
                (if (request.album.isNotBlank() && normalized(song.album) == normalized(request.album)) 20 else 0)
        }
        return songs.filter { it.url.isNotBlank() }.distinctBy { it.id }.sortedByDescending(::score).take(30)
    }
    fun page(songs: List<SongsModel>, page: Int, size: Int): List<SongsModel> {
        if (page < 0 || size <= 0) return emptyList()
        val start = page.toLong() * size
        if (start >= songs.size) return emptyList()
        return songs.drop(start.toInt()).take(size)
    }
}
