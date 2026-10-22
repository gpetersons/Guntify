package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.PlaybackCollection
import java.util.Locale

object PlaybackHighlightPolicy {
    private fun same(a: String, b: String) = a.trim().lowercase(Locale.ROOT) == b.trim().lowercase(Locale.ROOT)
    fun active(item: HomeItem, collection: PlaybackCollection?, currentSongId: Int): Boolean = when (item) {
        is HomeItem.RecentTrack -> item.song.id == currentSongId
        is HomeItem.Playlist -> collection?.type == "playlist" && item.id.isNotBlank() && collection.id == item.id
        is HomeItem.Radio -> collection?.key == "radio:${item.seed.spotifyTrackId.ifBlank { item.seed.id.toString() }}"
        is HomeItem.Album -> album(item.name, item.artists, collection)
        is HomeItem.Artist -> collection?.type == "artist" &&
            (if (item.id.isNotBlank() && collection.id.isNotBlank() && !same(collection.id, collection.name))
                collection.id == item.id else same(item.name, collection.name))
    }
    fun album(name: String, artist: String, collection: PlaybackCollection?): Boolean {
        if (collection?.type != "album" || !same(name, collection.name)) return false
        val actualArtist = collection.id.substringAfter('|', "").ifBlank { collection.subtitle }
        return artist.isBlank() || same(artist, actualArtist)
    }
    fun followingSong(songId: Int, sourceKey: String, currentSongId: Int, collection: PlaybackCollection?): Boolean =
        songId == currentSongId && sourceKey == collection?.key
}
