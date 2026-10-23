package com.music.spotui.data.recommendation

import com.music.spotui.audio.ManualQueuePolicy
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.ui.notification.CarSearchChoicePolicy
import org.junit.Assert.*
import org.junit.Test

class CarSearchChoicePolicyTest {
    private fun song(id: Int) = SongsModel(id, "Song $id", "", "Artist", "", "q$id")

    @Test fun searchTapOpensChoiceAndPreservesOriginalTrackIdentity() {
        val track = "song/voice%2Ftest%20query/314"
        val folder = CarSearchChoicePolicy.folderId(track)
        assertEquals(track, CarSearchChoicePolicy.folderTarget(folder))
        assertEquals(track, CarSearchChoicePolicy.playTarget(CarSearchChoicePolicy.playId(track)))
        assertEquals(track, CarSearchChoicePolicy.addTarget(CarSearchChoicePolicy.addId(track)))
        assertNull(CarSearchChoicePolicy.addTarget(CarSearchChoicePolicy.playId(track)))
    }

    @Test fun addToQueueInsertsAfterCurrentWithoutReplacingPlayingSong() {
        val existing = listOf(song(1), song(2), song(3))
        val result = ManualQueuePolicy.add(existing, 1, emptySet(), song(4))!!
        assertEquals(listOf(1, 4, 2, 3), result.map { it.id })
        assertEquals(1, result.first().id)
    }

    @Test fun repeatedAddDoesNotDuplicatePendingSong() {
        val existing = listOf(song(1), song(4), song(2))
        assertNull(ManualQueuePolicy.add(existing, 1, setOf(4), song(4)))
    }
}
