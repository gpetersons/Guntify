package com.music.spotui.data.recommendation

import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class CarNowPlayingMetadataTest {
    @Test fun titleAndArtistPopulateEveryAndroidAutoLabelField() {
        val metadata = MediaMetadata.Builder()
            .setTitle("Endless")
            .setDisplayTitle("Endless")
            .setArtist("Rainshow")
            .setSubtitle("Rainshow")
            .setDescription("Rainshow")
            .build()
        assertEquals("Endless", metadata.title.toString())
        assertEquals("Endless", metadata.displayTitle.toString())
        assertEquals("Rainshow", metadata.artist.toString())
        assertEquals("Rainshow", metadata.subtitle.toString())
        assertEquals("Rainshow", metadata.description.toString())
    }
}
