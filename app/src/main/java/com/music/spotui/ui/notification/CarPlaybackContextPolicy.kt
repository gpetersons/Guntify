package com.music.spotui.ui.notification

import com.music.spotui.data.entity.PlaybackCollection

/** The playback-source label; never replaces the current song title/artist. */
object CarPlaybackContextPolicy {
    fun title(collection: PlaybackCollection?, songTitle: String): String {
        if (collection == null) return songTitle.ifBlank { "Sprotify" }
        if (collection.key == "playlist:liked-songs" ||
            (collection.type == "playlist" && collection.id == "liked-songs")) {
            return "Dziesmas, kas tev patīk"
        }
        val name = collection.name.trim()
        if (collection.type == "radio") {
            if (name.isBlank()) return songTitle.ifBlank { "Sprotify" } + " radio"
            return if (name.endsWith("radio", ignoreCase = true)) name else "$name radio"
        }
        return name.ifBlank { songTitle.ifBlank { "Sprotify" } }
    }
}
