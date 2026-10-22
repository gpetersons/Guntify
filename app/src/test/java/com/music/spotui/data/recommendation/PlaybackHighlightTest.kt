package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackHighlightTest {
    private val song = SongsModel(42, "Song", "Album", "Artist", "", "query", "seed")
    @Test fun playlistGlowUsesIdentityRatherThanSharedNameOrTrack() {
        val collection = PlaybackCollection("playlist:a", "playlist", "a", "Mix", "")
        assertTrue(PlaybackHighlightPolicy.active(HomeItem.Playlist("Mix", "", "", "a"), collection, 42))
        assertFalse(PlaybackHighlightPolicy.active(HomeItem.Playlist("Mix", "", "", "b"), collection, 42))
        assertFalse(PlaybackHighlightPolicy.active(HomeItem.Playlist("Mix", "", "", ""), collection, 42))
    }
    @Test fun albumGlowDistinguishesSameTitleFromDifferentArtists() {
        val collection = PlaybackCollection("album:Hits|Artist A", "album", "Hits|Artist A", "Hits", "")
        assertTrue(PlaybackHighlightPolicy.album("Hits", "Artist A", collection))
        assertFalse(PlaybackHighlightPolicy.album("Hits", "Artist B", collection))
        assertFalse(PlaybackHighlightPolicy.album("Other", "Artist A", collection))
    }
    @Test fun radioGlowStaysWithOriginalSeedWhenItAdvances() {
        val collection = PlaybackCollection("radio:seed", "radio", "seed", "Song Radio", "")
        assertTrue(PlaybackHighlightPolicy.active(HomeItem.Radio(song), collection, 99))
        assertFalse(PlaybackHighlightPolicy.active(HomeItem.Radio(song.copy(spotifyTrackId = "other")), collection, 99))
    }
    @Test fun songCardOnlyHighlightsTheCurrentSong() {
        assertTrue(PlaybackHighlightPolicy.active(HomeItem.RecentTrack(song), null, 42))
        assertFalse(PlaybackHighlightPolicy.active(HomeItem.RecentTrack(song), null, 99))
    }
    @Test fun followingDuplicateSongOnlyHighlightsItsActualSourceMix() {
        val collection = PlaybackCollection("following:popular", "playlist", "following:popular", "Following", "")
        assertTrue(PlaybackHighlightPolicy.followingSong(42, "following:popular", 42, collection))
        assertFalse(PlaybackHighlightPolicy.followingSong(42, "artist:a", 42, collection))
        assertFalse(PlaybackHighlightPolicy.followingSong(99, "following:popular", 42, collection))
    }
    @Test fun artistIdentityWinsOverNamesWhenSpotifyIdsAreKnown() {
        val collection = PlaybackCollection("artist:a", "artist", "a", "Artist", "")
        assertTrue(PlaybackHighlightPolicy.active(HomeItem.Artist("Artist", "", "a"), collection, 42))
        assertFalse(PlaybackHighlightPolicy.active(HomeItem.Artist("Artist", "", "b"), collection, 42))
        val legacy = PlaybackCollection("artist:Artist", "artist", "Artist", "Artist", "")
        assertTrue(PlaybackHighlightPolicy.active(HomeItem.Artist("Artist", "", "a"), legacy, 42))
    }
}
