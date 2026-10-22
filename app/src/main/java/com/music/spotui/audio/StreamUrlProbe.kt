package com.music.spotui.audio

import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import kotlin.coroutines.resume

/** A probe is optional. Slow networks may let the player try the URL, but never hold lookup. */
object StreamUrlProbe {
    suspend fun check(call: Call, contentLength: Long?, timeoutMs: Long = 3_000L, onRejected: (Int) -> Unit = {}): Boolean =
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resume(true)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val accepted = try {
                            response.use {
                                val mediaType = it.header("Content-Type").orEmpty()
                                (it.isSuccessful || it.code == 416) &&
                                    (mediaType.startsWith("audio/") || mediaType.startsWith("video/")) &&
                                    (contentLength != null || it.body?.source()?.request(16 * 1024L) == true)
                            }
                        } catch (_: IOException) { true }
                        if (!accepted) onRejected(response.code)
                        if (continuation.isActive) continuation.resume(accepted)
                    }
                })
            }
        } ?: true
}
