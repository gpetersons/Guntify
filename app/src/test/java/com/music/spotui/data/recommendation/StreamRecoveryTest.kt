package com.music.spotui.data.recommendation

import com.music.spotui.audio.StreamRecoveryPolicy
import org.junit.Assert.*
import org.junit.Test

class StreamRecoveryTest {
    private val retry = StreamRecoveryPolicy.Action.RETRY
    @Test fun staleFailuresDoNotSkipTheCurrentSong() {
        assertEquals(StreamRecoveryPolicy.Action.IGNORE, StreamRecoveryPolicy().failure("old", "new"))
    }
    @Test fun firstSongGetsTwoRetriesThenOneSkip() {
        val policy = StreamRecoveryPolicy()
        assertEquals(retry, policy.failure("A", "A"))
        assertEquals(retry, policy.failure("A", "A"))
        assertEquals(StreamRecoveryPolicy.Action.SKIP, policy.failure("A", "A"))
    }
    @Test fun secondSongKeepsRetryingWithoutSkippingAgain() {
        val policy = StreamRecoveryPolicy()
        repeat(3) { policy.failure("A", "A") }
        repeat(20) { assertEquals(retry, policy.failure("B", "B")) }
    }
    @Test fun audibleRecoveryStartsANewFailureBudget() {
        val policy = StreamRecoveryPolicy()
        repeat(3) { policy.failure("A", "A") }
        policy.recovered()
        assertEquals(retry, policy.failure("A", "A"))
        assertEquals(retry, policy.failure("A", "A"))
        assertEquals(StreamRecoveryPolicy.Action.SKIP, policy.failure("A", "A"))
    }
    @Test fun ignoredFailureDoesNotConsumeRetryBudget() {
        val policy = StreamRecoveryPolicy()
        policy.failure("old", "A"); policy.failure("", "")
        assertEquals(retry, policy.failure("A", "A"))
        assertEquals(retry, policy.failure("A", "A"))
    }
}
