package com.music.spotui.data.entity

/** Account-followed artists only; this is a local playable mix, not a saved Spotify playlist. */
data class FollowingFeed(
    val artists: List<FollowingArtist> = emptyList(),
    val releases: List<FollowingRelease> = emptyList(),
    val popularSongs: List<SongsModel> = emptyList(),
    val failedArtists: Int = 0,
    val followedCount: Int = 0,
    val loadedArtists: Int = 0,
)
data class FollowingArtist(val id: String, val name: String, val image: String, val songs: List<ArtistTrackUi>)
data class FollowingRelease(val id: String, val name: String, val artist: String, val image: String, val date: String)
