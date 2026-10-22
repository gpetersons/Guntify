package com.music.spotui.data.preferences

import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeSection
import java.util.Locale

/** Unknown/named Spotify sections keep their exact original content and order. */
object HomeSectionPolicy {
    private val genericTitles = setOf(
        "albums", "playlists", "artists", "songs", "discover", "best of artists",
        "your favorite artists", "your favourite artists", "favorite artists", "favourite artists",
        "your top mixes", "top mixes", "made for you", "recommended for you",
        "recommended albums", "recommended playlists", "recommended artists", "recommended songs",
        "albums for you", "playlists for you", "artists for you", "songs for you",
        "discover songs for you", "discover artists for you",
    )
    fun canPersonalize(title: String): Boolean = title.trim().lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ") in genericTitles

    fun personalize(feed: HomeFeedModel, transform: (HomeSection) -> HomeSection): HomeFeedModel =
        feed.copy(sections = feed.sections.map { section ->
            if (canPersonalize(section.title)) transform(section).copy(title = section.title) else section
        })
}
