package com.music.spotui.webui

import org.junit.Assert.*
import org.junit.Test

class WebUiProtocolTest {
    @Test fun closedOpenAndSuffixRanges() {
        assertEquals(WebUiProtocol.Window(10, 19), WebUiProtocol.range("bytes=10-19", 100))
        assertEquals(WebUiProtocol.Window(90, 99), WebUiProtocol.range("bytes=90-", 100))
        assertEquals(WebUiProtocol.Window(90, 99), WebUiProtocol.range("bytes=-10", 100))
        assertEquals(WebUiProtocol.Window(0, 99), WebUiProtocol.range("bytes=-200", 100))
        assertEquals(WebUiProtocol.Window(80, 99), WebUiProtocol.range("bytes=80-200", 100))
        assertEquals(10L, WebUiProtocol.range("bytes=10-19", 100)!!.length)
        assertNull(WebUiProtocol.range(null, 100))
    }
    @Test fun malformedAndOutOfBoundsRangesAreRejected() {
        listOf("bytes=100-", "bytes=20-10", "bytes=-0", "bytes=--1", "bytes=0-1,3-4",
            "bytes=a-b", "bytes=999999999999999999999-", "items=0-1", "bytes=", "bytes=0-1-2").forEach { header ->
            try { WebUiProtocol.range(header, 100); fail("Accepted $header") }
            catch (_: IllegalArgumentException) { }
        }
        try { WebUiProtocol.range("bytes=0-", 0); fail("Accepted an empty file") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun cookiesUseExactNameAndDoNotAcceptPrefixes() {
        assertEquals("abc", WebUiProtocol.cookie("other=x; uspot_web=abc; another=y"))
        assertEquals("", WebUiProtocol.cookie("not_uspot_web=abc"))
        assertEquals("", WebUiProtocol.cookie(null))
        assertTrue(WebUiProtocol.sameSecret("abc", "abc"))
        assertFalse(WebUiProtocol.sameSecret("abc", "abd"))
        assertFalse(WebUiProtocol.sameSecret("abc", "abc "))
    }
    @Test fun accessSecretsAreUniqueAndHighEntropy() {
        val a = WebUiProtocol.secret(); val b = WebUiProtocol.secret()
        assertTrue(a.matches(Regex("[0-9a-f]{64}")))
        assertNotEquals(a, b)
    }
    @Test fun foreignOriginsAndNullTextAreRejected() {
        assertTrue(WebUiProtocol.allowedOrigin(null, "192.168.1.9:8765"))
        assertTrue(WebUiProtocol.allowedOrigin("http://192.168.1.9:8765", "192.168.1.9:8765"))
        listOf("null", "https://evil.example", "http://192.168.1.9:8765.evil.example", "http://192.168.1.9:9000").forEach {
            assertFalse(WebUiProtocol.allowedOrigin(it, "192.168.1.9:8765"))
        }
    }
}
