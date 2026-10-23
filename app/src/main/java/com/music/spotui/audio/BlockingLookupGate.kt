package com.music.spotui.audio

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Isolate legacy blocking extraction. A stuck worker cannot queue up subsequent songs. */
class BlockingLookupGate {
    private val occupied = AtomicBoolean(false)

    suspend fun <T> run(timeoutMs: Long, block: () -> T): T? {
        return withTimeoutOrNull(timeoutMs) {
            if (!occupied.compareAndSet(false, true)) return@withTimeoutOrNull null
            suspendCancellableCoroutine { continuation ->
                val worker = Thread(worker@{
                    if (!continuation.isActive) { occupied.set(false); return@worker }
                    val result = runCatching(block)
                    // Release before resuming the next caller, without a late finally
                    // accidentally releasing a newer worker's permit.
                    occupied.set(false)
                    result.fold(continuation::resume, continuation::resumeWithException)
                }, "Guntify-legacy-stream-lookup").apply { isDaemon = true }
                continuation.invokeOnCancellation { worker.interrupt() }
                try { worker.start() }
                catch (error: Throwable) { occupied.set(false); continuation.resumeWithException(error) }
            }
        }
    }
}
