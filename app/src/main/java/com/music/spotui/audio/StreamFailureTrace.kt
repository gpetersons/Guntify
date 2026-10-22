package com.music.spotui.audio

import java.io.IOException

/** Fixed codes only: never include remote reason text, URLs, IDs or exception messages. */
class StreamFailureTrace {
    private val codes = linkedSetOf<String>()
    @Synchronized private fun add(code: String) { if (code !in codes) { if (codes.size >= 4) codes.remove(codes.first()); codes.add(code) } }
    fun endpoint(error: Throwable) {
        val code = when (error) {
            is java.net.UnknownHostException -> "DNS"
            is java.net.SocketTimeoutException -> "Timeout"
            is javax.net.ssl.SSLException -> "TLS"
            is java.net.ConnectException -> "Connection"
            else -> "Endpoint failure"
        }
        add(code)
    }
    fun response(status: String?) {
        if (status == "OK") return
        val code = status?.takeIf { it in setOf("LOGIN_REQUIRED", "UNPLAYABLE", "ERROR", "AGE_CHECK_REQUIRED", "AGE_VERIFICATION_REQUIRED", "CONTENT_CHECK_REQUIRED", "LIVE_STREAM_OFFLINE") } ?: "UNKNOWN"
        add("Status $code")
    }
    fun missingFormat() = add("No audio format")
    fun missingUrl() = add("No audio URL")
    fun wrongVideo() = add("Wrong video rejected")
    fun rejectedHttp(code: Int) { if (code in 100..599) add("Stream HTTP $code") }
    @Synchronized fun summary(): String = codes.joinToString(", ").ifBlank { "No accepted stream" }.take(80)
}
class StreamCandidateException(val summary: String) : IOException("No client supplied an accepted audio stream")
