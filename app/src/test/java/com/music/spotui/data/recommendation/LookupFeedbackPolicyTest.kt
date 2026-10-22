package com.music.spotui.data.recommendation

import com.music.spotui.audio.*
import org.junit.Assert.*
import org.junit.Test

class LookupFeedbackPolicyTest {
    @Test fun firstPreparationIsImmediate() { assertTrue(PreparationBackoff().ready(100)) }
    @Test fun failedPreparationWaitsTenSecondsAfterCompletion() {
        val b=PreparationBackoff();b.failed(5_000)
        assertFalse(b.ready(14_999));assertTrue(b.ready(15_000))
    }
    @Test fun repeatedPreparationFailuresBackOffAndCap() {
        val b=PreparationBackoff();var now=1_000L
        for (delay in listOf(10_000L,20_000L,40_000L,60_000L,60_000L)) {
            b.failed(now);assertFalse(b.ready(now+delay-1));assertTrue(b.ready(now+delay));now+=delay
        }
    }
    @Test fun changedSelectionResetsPreparationWait() {
        val b=PreparationBackoff();b.failed(100_000);b.reset();assertTrue(b.ready(100_001))
        b.failed(100_001);assertTrue(b.ready(110_001))
    }
    @Test fun acceptedResponseDoesNotBecomeFailure() {
        val t=StreamFailureTrace();t.response("OK");assertEquals("No accepted stream",t.summary())
    }
    @Test fun rejectedStreamsShowHttpCode() {
        val t=StreamFailureTrace();t.rejectedHttp(403);assertEquals("Stream HTTP 403",t.summary())
    }
    @Test fun playabilityAndTransportFailuresAreDistinguished() {
        val t=StreamFailureTrace();t.response("LOGIN_REQUIRED");t.endpoint(java.net.SocketTimeoutException())
        assertEquals("Status LOGIN_REQUIRED, Timeout",t.summary())
    }
    @Test fun remoteMessagesCannotEnterDiagnostics() {
        val t=StreamFailureTrace();t.endpoint(java.io.IOException("https://secret.invalid?cookie=secret"))
        t.response("secret_user_id");t.rejectedHttp(-1)
        assertEquals("Endpoint failure, Status UNKNOWN",t.summary())
    }
    @Test fun summaryIsBoundedAndRetainsRecentRejections() {
        val t=StreamFailureTrace();t.missingFormat();t.missingUrl();t.wrongVideo();t.response("ERROR");t.rejectedHttp(429)
        assertTrue(t.summary().length<=80);assertTrue(t.summary().contains("HTTP 429"))
    }
    @Test fun dnsAndTlsFailuresHaveFixedLabels() {
        val t=StreamFailureTrace();t.endpoint(java.net.UnknownHostException("secret"));t.endpoint(javax.net.ssl.SSLException("secret"))
        assertEquals("DNS, TLS",t.summary())
    }
}
