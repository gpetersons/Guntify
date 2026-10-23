package com.music.spotui.ui.notification

import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.SongsModel
import java.net.URLDecoder
import java.net.URLEncoder

/** Stable car browse IDs; collection history is independent of Home visibility preferences. */
object CarBrowsePolicy {
    const val RECENT = "recent"
    const val RECENT_RADIOS = "recent-radios"
    const val LIBRARY = "library"
    data class Entry(val id: String, val title: String, val artwork: String = "", val subtitle: String = "", val playable: Boolean = false)
    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    fun radioId(station: String) = "radio/" + encode(station)
    fun radioStation(id: String): String? = if (!id.startsWith("radio/")) null else
        runCatching { URLDecoder.decode(id.removePrefix("radio/"), "UTF-8").takeIf { it.isNotBlank() } }.getOrNull()
    fun trackId(parent: String, song: SongsModel) = "song/${encode(parent)}/${song.id}"
    fun root() = listOf(Entry(RECENT, "Recent"), Entry(RECENT_RADIOS, "Recent radios"), Entry("liked", "Liked Songs"), Entry(LIBRARY, "Library"))
    fun library() = listOf(Entry("playlists", "Playlists"), Entry("albums", "Albums"), Entry("downloads", "Downloads"))
    /** Spotify supplies these as playlists, without a reliable Radio type flag.
     * Only radio-named playlist titles qualify; this does not assert Spotify ownership. */
    fun recentRadios(items: List<HomeItem>, limit: Int = 40): List<Entry> = recent(items.filter {
        it is HomeItem.Radio || (it is HomeItem.Playlist && isRadioPlaylist(it.name))
    }, limit).map { if (it.id.startsWith("playlist/")) it.copy(subtitle = "Spotify playlist") else it }
    fun isRadioPlaylist(name: String): Boolean = name.trim().let {
        it.equals("Radio", ignoreCase = true) || it.endsWith(" Radio", ignoreCase = true)
    }
    fun recent(items: List<HomeItem>, limit: Int = 40): List<Entry> = items.mapNotNull { item ->
        when (item) {
            is HomeItem.Radio -> Entry(radioId(item.seed.spotifyTrackId.ifBlank { item.seed.id.toString() }), item.name, item.imageUrl, "Guntify Radio", true)
            is HomeItem.Playlist -> if (item.id.isBlank()) null else Entry(when (item.id) {
                "liked-songs" -> "liked"
                "downloads", "downloaded-offline" -> "downloads"
                else -> "playlist/${item.id}"
            }, item.name, item.imageUrl, "Playlist")
            is HomeItem.Album -> Entry("album/${encode(item.name)}/${encode(item.artists)}", item.name, item.imageUrl, "Album")
            else -> null
        }
    }.distinctBy { it.id }.take(limit.coerceIn(0, 40))

    /** Saved current + next ten first; otherwise start from last track or an original mix anchor. */
    fun radioSongs(seed: SongsModel, last: SongsModel?, anchors: List<SongsModel>, saved: List<SongsModel>): List<SongsModel> {
        val cached = saved.filter { it.url.isNotBlank() }.distinctBy { it.id }.take(11)
        if (cached.isNotEmpty()) return cached
        return listOfNotNull(last, anchors.firstOrNull(), seed).firstOrNull { it.url.isNotBlank() }?.let { listOf(it) }.orEmpty()
    }
}
