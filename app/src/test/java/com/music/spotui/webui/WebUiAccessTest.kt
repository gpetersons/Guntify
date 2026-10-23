package com.music.spotui.webui

import org.junit.Assert.*
import org.junit.Test

class WebUiAccessTest {
    @Test fun anonymousRequestsNeverCreateSessions() {
        val access = WebUiAccess()
        assertFalse(access.authorized(null))
        assertFalse(access.authorized("guntify_web=made-up"))
        assertTrue(access.currentPin().matches(Regex("[0-9]{8}")))
        assertEquals(WebUiAccess.Result.INVALID, access.pair("000").result)
        assertFalse(access.authorized(null))
    }

    @Test fun correctPinCreatesSeparateCookieAndConsumesTheCode() {
        var changes = 0
        val access = WebUiAccess(onPinChanged = { changes++ })
        val pin = access.currentPin()
        val paired = access.pair(pin)
        assertEquals(WebUiAccess.Result.PAIRED, paired.result)
        val bearer = requireNotNull(paired.session)
        assertTrue(bearer.matches(Regex("[0-9a-f]{64}")))
        assertNotEquals(pin, bearer)
        assertTrue(access.authorized("other=y; guntify_web=$bearer; z=2"))
        assertEquals(WebUiAccess.Result.INVALID, access.pair(pin).result)
        assertEquals(1, changes)
        assertFalse(access.authorized("uspot_web=$bearer"))
    }

    @Test fun wrongPinIsRateLimitedWithoutMintingACookie() {
        var clock = 0L
        val access = WebUiAccess(now = { clock })
        val good = access.currentPin()
        val bad = if (good != "99999999") "99999999" else "88888888"
        repeat(WebUiAccess.MAX_ATTEMPTS) {
            assertEquals(WebUiAccess.Result.INVALID, access.pair(bad).result)
        }
        assertEquals(WebUiAccess.Result.RATE_LIMITED, access.pair(good).result)
        clock += WebUiAccess.LOCK_MS
        assertEquals(WebUiAccess.Result.PAIRED, access.pair(good).result)
    }

    @Test fun pinExpiresAndCanBeRotatedOnlyByLocalController() {
        var clock = 10_000L
        val access = WebUiAccess(now = { clock })
        val original = access.currentPin()
        clock += WebUiAccess.PIN_TTL_MS
        assertEquals(WebUiAccess.Result.EXPIRED, access.pair(original).result)
        val current = access.currentPin()
        assertTrue(current.matches(Regex("[0-9]{8}")))
        assertEquals(WebUiAccess.Result.PAIRED, access.pair(current).result)
    }

    @Test fun sessionsExpireAndStopRevokesEveryCookie() {
        var clock = 0L
        val access = WebUiAccess(now = { clock })
        val bearer = requireNotNull(access.pair(access.currentPin()).session)
        assertTrue(access.authorized("guntify_web=$bearer"))
        clock += WebUiAccess.SESSION_TTL_MS
        assertFalse(access.authorized("guntify_web=$bearer"))
        val current = access.currentPin()
        val other = requireNotNull(access.pair(current).session)
        assertTrue(access.authorized("guntify_web=$other"))
        access.invalidate()
        assertFalse(access.authorized("guntify_web=$other"))
        assertEquals(WebUiAccess.Result.CLOSED, access.pair(current).result)
        assertEquals("", access.currentPin())
    }

    @Test fun noMoreThanFourSessionsStayActive() {
        val access = WebUiAccess()
        val cookies = (1..WebUiAccess.MAX_SESSIONS + 1).map {
            requireNotNull(access.pair(access.currentPin()).session)
        }
        assertFalse(access.authorized("guntify_web=${cookies.first()}"))
        cookies.drop(1).forEach { assertTrue(access.authorized("guntify_web=$it")) }
    }
}
