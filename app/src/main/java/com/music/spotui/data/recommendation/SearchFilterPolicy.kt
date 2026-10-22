package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.SearchResults
object SearchFilterPolicy {
    fun filter(results: SearchResults, kind: String): SearchResults = when (kind) {
        "Songs" -> SearchResults(songs = results.songs)
        "Albums" -> SearchResults(albums = results.albums)
        "Artists" -> SearchResults(artists = results.artists)
        "Playlists" -> SearchResults(playlists = results.playlists)
        else -> results
    }
}
