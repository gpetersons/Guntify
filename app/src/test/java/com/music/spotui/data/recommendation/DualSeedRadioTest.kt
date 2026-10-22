package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SongsModel
import org.junit.Assert.*
import org.junit.Test

class DualSeedRadioTest {
    private fun song(id: Int) = SongsModel(id, "Song", "", "", "", "q", "s$id")

    @Test fun originalRemainsWhenFifthSongChanges() {
        assertEquals(listOf("original", "fifth"), DualSeedRadioPolicy.seeds("original", "fifth"))
        assertEquals(listOf("original", "tenth"), DualSeedRadioPolicy.seeds("original", "tenth"))
    }

    @Test fun initialIdenticalSeedsNeedOnlyOneRequest() {
        assertEquals(listOf("original"), DualSeedRadioPolicy.seeds("original", "original"))
    }

    @Test fun combinesBothSourcesInAlternatingOrder() {
        val result = DualSeedRadioPolicy.blend(emptyList(), listOf(listOf(song(1), song(2)), listOf(song(3), song(4))))
        assertEquals(listOf(1, 3, 2, 4), result.map { it.id })
    }

    @Test fun excludesSeedsAndDuplicatesAcrossSources() {
        val result = DualSeedRadioPolicy.blend(listOf("s1", "s2"), listOf(listOf(song(1), song(3)), listOf(song(2), song(3), song(4))))
        assertEquals(listOf(3, 4), result.map { it.id })
    }

    @Test fun oneUnavailableSourceDoesNotBlankTheQueue() {
        assertEquals(listOf(song(3)), DualSeedRadioPolicy.blend(emptyList(), listOf(emptyList(), listOf(song(3)))))
    }

    @Test fun rotatedTenSongWindowIncludesBothInfluences() {
        val a = (1..20).map(::song)
        val b = (21..40).map(::song)
        val current = song(99)
        val result = EvolvingRadioPolicy.fill(current, listOf(current), DualSeedRadioPolicy.blend(emptyList(), listOf(a, b)), true)
        assertEquals(current, result.first())
        assertEquals(11, result.size)
        assertEquals(5, result.drop(1).count { it.id in 1..20 })
        assertEquals(5, result.drop(1).count { it.id in 21..40 })
    }
}
