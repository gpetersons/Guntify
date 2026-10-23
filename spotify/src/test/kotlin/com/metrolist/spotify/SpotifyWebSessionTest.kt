package com.metrolist.spotify

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SpotifyWebSessionTest {
    @After fun cleanup() = SpotifyWebSession.clear()
    private val headers = mapOf("Client-Token" to "a".repeat(40), "User-Agent" to "Session browser")
    @Test fun ownHttpsSessionHeadersAreReused() {
        SpotifyWebSession.capture("https://api-partner.spotify.com/pathfinder/v2/query",headers,1000)
        assertEquals("a".repeat(40),SpotifyWebSession.current(1001)?.clientToken)
        assertEquals("Session browser",SpotifyWebSession.current(1001)?.userAgent)
    }
    @Test fun regionalSpotifyEndpointsCanProvideTheSession() {
        SpotifyWebSession.capture("https://gew4-spclient.spotify.com/a",headers,1000)
        assertNotNull(SpotifyWebSession.current(1001))
    }
    @Test fun regionalLookalikesCannotProvideTheSession() {
        SpotifyWebSession.capture("https://gew4-spclient.spotify.com.evil.test/a",headers,1000)
        assertNull(SpotifyWebSession.current(1001))
    }
    @Test fun arbitraryHostsAndPlainHttpCannotSetIdentity() {
        for (url in listOf("http://spclient.wg.spotify.com/a","https://spclient.wg.spotify.com.evil.test/a","https://evil.test/a")) {
            SpotifyWebSession.capture(url,headers,1000)
            assertNull(SpotifyWebSession.current(1001))
        }
    }
    @Test fun expiredOrClockReversedIdentityIsNotSent() {
        SpotifyWebSession.capture("https://spclient.wg.spotify.com/a",headers,1000)
        assertNull(SpotifyWebSession.current(999))
        assertNull(SpotifyWebSession.current(3_601_000))
    }
    @Test fun controlCharactersCannotBecomeHeaders() {
        SpotifyWebSession.capture("https://spclient.wg.spotify.com/a",headers + ("Client-Token" to ("a".repeat(40)+"\r\nInjected: yes")),1000)
        assertNull(SpotifyWebSession.current(1001))
    }
    @Test fun accountChangeClearsIdentity() {
        SpotifyWebSession.capture("https://spclient.wg.spotify.com/a",headers,1000)
        SpotifyWebSession.clear(); assertNull(SpotifyWebSession.current(1001))
    }
}
