package com.music.spotui.webui

import java.security.MessageDigest
import java.security.SecureRandom

internal object WebUiProtocol {
    fun secret(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    fun sameSecret(expected: String, supplied: String): Boolean = supplied.length == expected.length &&
        MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), supplied.toByteArray(Charsets.UTF_8))
    fun cookie(header: String?): String = header.orEmpty().split(';').map(String::trim)
        .firstOrNull { it.startsWith("guntify_web=") }?.substringAfter('=').orEmpty()
    fun allowedOrigin(origin: String?, authority: String) = origin == null || origin == "http://$authority"
    fun sameOrigin(origin: String?, authority: String) = origin == "http://$authority"

    data class Window(val start: Long, val end: Long) { val length: Long get() = end - start + 1 }
    fun range(header: String?, total: Long): Window? {
        if (header == null) return null
        require(total > 0 && header.startsWith("bytes=") && ',' !in header) { "Unsupported range" }
        val parts = header.removePrefix("bytes=").split('-')
        require(parts.size == 2)
        if (parts[0].isEmpty()) {
            val suffix = parts[1].toLongOrNull() ?: throw IllegalArgumentException("Invalid suffix")
            require(suffix > 0)
            return Window((total - suffix).coerceAtLeast(0), total - 1)
        }
        val start = parts[0].toLongOrNull() ?: throw IllegalArgumentException("Invalid offset")
        val end = if (parts[1].isEmpty()) total - 1 else parts[1].toLongOrNull() ?: throw IllegalArgumentException("Invalid end")
        require(start >= 0 && start < total && end >= start)
        return Window(start, minOf(end, total - 1))
    }
}
