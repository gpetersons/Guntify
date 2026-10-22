package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.HomeSection
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeItem
import java.util.Locale

/** Short initial feed; additional Spotify rows stay available without retitling them. */
object HomeLayoutPolicy {
    const val INITIAL_ROWS = 4
    const val PAGE_ROWS = 4
    const val CARDS_PER_ROW = 6
    fun visible(sections: List<HomeSection>, requested: Int): List<HomeSection> =
        sections.filter { it.items.isNotEmpty() }.take(requested.coerceAtLeast(INITIAL_ROWS))
    private fun identity(item: HomeItem): String = when (item) {
        is HomeItem.Playlist -> "playlist:" + item.id.ifBlank { item.name.trim().lowercase(Locale.ROOT) }
        is HomeItem.Album -> "album:" + item.name.trim().lowercase(Locale.ROOT) + "|" + item.artists.trim().lowercase(Locale.ROOT)
        is HomeItem.Radio -> "radio:" + item.seed.spotifyTrackId.ifBlank { item.seed.id.toString() }
        is HomeItem.Artist -> "artist:" + item.id
        is HomeItem.RecentTrack -> "track:" + item.song.id
    }
    private fun collections(items: List<HomeItem>): List<HomeItem> = items.filter {
        it is HomeItem.Playlist || it is HomeItem.Album || it is HomeItem.Radio
    }.distinctBy(::identity)

    /** Spotify fallback stays live; first-section artists cannot leave the rotation empty. */
    fun spotifyCollections(feed: HomeFeedModel): List<HomeItem> {
        val ordered = feed.sections.withIndex().sortedWith(compareBy<IndexedValue<HomeSection>> {
            when {
                it.value.title.isBlank() -> 0
                it.value.title.equals("Recently played", ignoreCase = true) -> 1
                else -> 2
            }
        }.thenBy { it.index })
        return collections(ordered.flatMap { it.value.items })
    }
    fun rotation(local: List<HomeItem>, spotify: List<HomeItem>): List<HomeItem> = collections(local + spotify).take(8)
}

