package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SearchResults

object SearchPagePolicy {
    fun append(old: SearchResults, page: SearchResults): SearchResults = SearchResults(
        songs = (old.songs + page.songs).distinctBy { it.spotifyTrackId.ifBlank { it.id.toString() } },
        albums = (old.albums + page.albums).distinctBy { it.name to it.artists },
        artists = (old.artists + page.artists).distinctBy { it.id.ifBlank { it.name } },
        shows = old.shows,
        episodes = old.episodes,
        playlists = LibraryIdentityPolicy.unique(old.playlists + page.playlists),
        nextOffset = page.nextOffset,
    )
}
