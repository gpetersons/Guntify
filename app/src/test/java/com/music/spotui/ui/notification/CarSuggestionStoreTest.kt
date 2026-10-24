package com.music.spotui.ui.notification

import com.music.spotui.data.entity.SongsModel
import org.junit.Assert.*
import org.junit.Test

class CarSuggestionStoreTest {
    @Test fun restartCatalogRetainsPlayableIdentityAndArtwork() {
        val song = SongsModel(7, "A/B", "Album", "Artist", "https://image", "q|id", "sp", true, 1000)
        assertEquals(listOf(song), CarSuggestionStore.decode(CarSuggestionStore.encode(listOf(song))))
    }
    @Test fun corruptCatalogCannotCrashBrowsingAndSizeIsBounded() {
        assertTrue(CarSuggestionStore.decode("bad json").isEmpty())
        val songs = (1..100).map { SongsModel(it, "Song", "", "", "", "q$it") }
        assertEquals(CarSuggestionStore.LIMIT, CarSuggestionStore.decode(CarSuggestionStore.encode(songs)).size)
    }
}
