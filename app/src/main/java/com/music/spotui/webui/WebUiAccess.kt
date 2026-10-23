package com.music.spotui.webui

import java.security.SecureRandom

/**
 * Per-start, memory-only LAN pairing state. The PIN is shown only on the phone,
 * never in a GET response. A successful pairing consumes the PIN and creates a
 * separate bearer cookie; restarting/stopping desktop mode revokes every cookie.
 *
 * NOTE: LAN HTTP is not encrypted. Use only on a trusted private network.
 */
internal class WebUiAccess(
    private val now: () -> Long = System::currentTimeMillis,
    private val onPinChanged: (String) -> Unit = {},
) {
    companion object {
        const val PIN_TTL_MS = 10 * 60_000L
        const val SESSION_TTL_MS = 8 * 60 * 60_000L
        const val MAX_ATTEMPTS = 5
        const val LOCK_MS = 60_000L
        const val MAX_SESSIONS = 4
    }

    internal enum class Result { PAIRED, INVALID, EXPIRED, RATE_LIMITED, CLOSED }
    internal data class PairResult(val result: Result, val session: String? = null)

    private val random = SecureRandom()
    private val sessions = LinkedHashMap<String, Long>()
    private var pin = newPin()
    private var pinExpiresAt = now() + PIN_TTL_MS
    private var failed = 0
    private var blockedUntil = 0L
    private var closed = false

    private fun newPin(): String = random.nextInt(100_000_000).toString().padStart(8, '0')

    @Synchronized fun currentPin(): String {
        if (!closed && now() >= pinExpiresAt) rotatePin()
        return if (closed) "" else pin
    }

    /** Called from the Android phone UI, never from an unauthenticated HTTP request. */
    @Synchronized fun rotatePin(): String {
        if (closed) return ""
        val oldPin = pin
        do { pin = newPin() } while (pin == oldPin)
        pinExpiresAt = now() + PIN_TTL_MS
        failed = 0
        blockedUntil = 0
        onPinChanged(pin)
        return pin
    }

    @Synchronized fun pair(input: String): PairResult {
        if (closed) return PairResult(Result.CLOSED)
        val time = now()
        if (time >= pinExpiresAt) {
            rotatePin()
            return PairResult(Result.EXPIRED)
        }
        if (time < blockedUntil) return PairResult(Result.RATE_LIMITED)
        if (!input.matches(Regex("[0-9]{8}")) || !WebUiProtocol.sameSecret(pin, input)) {
            failed += 1
            if (failed >= MAX_ATTEMPTS) {
                blockedUntil = time + LOCK_MS
                failed = 0
            }
            return PairResult(Result.INVALID)
        }
        sessions.entries.removeAll { it.value <= time }
        if (sessions.size >= MAX_SESSIONS) {
            sessions.remove(sessions.entries.first().key)
        }
        val token = WebUiProtocol.secret()
        sessions[token] = time + SESSION_TTL_MS
        rotatePin() // one-time code: the same PIN cannot authorize another browser
        return PairResult(Result.PAIRED, token)
    }

    @Synchronized fun authorized(cookieHeader: String?): Boolean {
        if (closed) return false
        val session = WebUiProtocol.cookie(cookieHeader)
        if (session.isBlank()) return false
        val validUntil = sessions[session] ?: return false
        if (now() >= validUntil) {
            sessions.remove(session)
            return false
        }
        return true
    }

    @Synchronized fun invalidate() {
        closed = true
        sessions.clear()
        pin = ""
        failed = 0
    }
}
