package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.LibraryEntry
import org.junit.Assert.*
import org.junit.Test

class LibraryIdentityPolicyTest {
    private fun entry(id: String, playlist: Boolean = true, name: String = id) =
        LibraryEntry(id, name, "", "", playlist)

    @Test fun overlappingPagesProduceUniqueLazyKeys() {
        val first = (1..50).map { entry("$it") }
        val second = (45..90).map { entry("$it") }
        val result = LibraryIdentityPolicy.unique(first + second)
        assertEquals(90, result.size)
        assertEquals(result.size, result.map(LibraryIdentityPolicy::key).toSet().size)
        assertEquals(first.first(), result.first())
    }

    @Test fun albumsAndPlaylistsWithSameIdRemainSeparate() {
        val result = LibraryIdentityPolicy.unique(listOf(entry("same"), entry("same", false)))
        assertEquals(2, result.size)
        assertNotEquals(LibraryIdentityPolicy.key(result[0]), LibraryIdentityPolicy.key(result[1]))
    }

    @Test fun duplicateCacheRowsKeepFirstOccurrenceAndOrder() {
        val original = entry("liked-songs", name = "Liked Songs")
        assertEquals(listOf(original, entry("downloads")), LibraryIdentityPolicy.unique(
            listOf(original, entry("downloads"), entry("liked-songs", name = "Duplicate"))))
    }

    @Test fun invalidIdsAreNotRenderedAsBrokenNavigationTargets() {
        assertEquals(listOf(entry("ok")), LibraryIdentityPolicy.unique(listOf(entry(""), entry(" "), entry("ok"))))
    }
}
