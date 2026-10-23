package com.metrolist.spotify

import java.net.URI

/** Reuses the current Web Player session. Memory only; never logged or exported. */
object SpotifyWebSession {
    data class Identity(val clientToken: String, val userAgent: String?, val capturedAt: Long)
    @Volatile private var identity: Identity? = null
    fun clear() { identity = null }
    fun current(now: Long = System.currentTimeMillis()): Identity? = identity?.takeIf {
        now >= it.capturedAt && now - it.capturedAt < 60 * 60_000L
    }
    private fun isSessionHost(host: String?): Boolean = host in setOf("api-partner.spotify.com", "spclient.wg.spotify.com") ||
        (host != null && Regex("[a-z0-9]+-spclient\\.spotify\\.com").matches(host))

    /** Let the existing signed-in WebView finish loading before the first write. */
    suspend fun awaitCurrent(): Identity? = kotlinx.coroutines.withTimeoutOrNull(4_000L) {
        while (current() == null) kotlinx.coroutines.delay(100L)
        current()
    }
    fun capture(url: String, headers: Map<String, String>, now: Long = System.currentTimeMillis()) {
        val uri = runCatching { URI(url) }.getOrNull() ?: return
        if (uri.scheme != "https" || !isSessionHost(uri.host) || uri.userInfo != null) return
        val token = headers.entries.firstOrNull { it.key.equals("client-token", true) }?.value ?: return
        if (token.length !in 20..16384 || token.any { it.code < 33 || it.code > 126 }) return
        val ua = headers.entries.firstOrNull { it.key.equals("user-agent", true) }?.value
            ?.takeIf { it.length in 1..512 && it.all { c -> c.code in 32..126 } }
        identity = Identity(token, ua, now)
    }
}
