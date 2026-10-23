package com.music.spotui.data.recommendation

import com.metrolist.music.utils.potoken.PoTokenResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Provider contract: a visitor/session proof is not interchangeable with a video proof. */
class PoTokenBindingTest {
    @Test fun changingSongsMintsNewPlayerProofAndRetainsSessionProof() = runBlocking {
        val requested = mutableListOf<String>()
        suspend fun mint(id: String): String { requested += id; return "proof-for-$id" }
        val first = PoTokenResult.forVideo("session-proof", "video-A", ::mint)
        val second = PoTokenResult.forVideo("session-proof", "video-B", ::mint)
        assertEquals(listOf("video-A", "video-B"), requested)
        assertEquals("proof-for-video-A", first.playerRequestPoToken)
        assertEquals("proof-for-video-B", second.playerRequestPoToken)
        assertEquals("session-proof", first.streamingDataPoToken)
        assertEquals(first.streamingDataPoToken, second.streamingDataPoToken)
        assertNotEquals(first.playerRequestPoToken, first.streamingDataPoToken)
        assertEquals("proof-for-video-A", first.videoBoundStreamingDataPoToken)
        assertEquals("proof-for-video-B", second.videoBoundStreamingDataPoToken)
        assertNotEquals(first.videoBoundStreamingDataPoToken, second.videoBoundStreamingDataPoToken)
    }
    @Test fun failedVideoMintMustNotSubstituteTheSessionProof() = runBlocking {
        var returned = false
        try {
            PoTokenResult.forVideo("session-proof", "video-A") { throw java.io.IOException("fixture") }
            returned = true
        } catch (_: java.io.IOException) { }
        assertFalse(returned)
    }
}
