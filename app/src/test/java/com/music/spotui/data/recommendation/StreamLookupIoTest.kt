package com.music.spotui.data.recommendation

import com.music.spotui.audio.BlockingLookupGate
import com.music.spotui.audio.StreamUrlProbe
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test

class StreamLookupIoTest {
    @Test fun stuckLegacyWorkerDoesNotQueueOtherSongs() = runBlocking {
        val gate = BlockingLookupGate()
        val release = CountDownLatch(1)
        val entered = CountDownLatch(1)
        try {
            assertNull(gate.run(100) {
                entered.countDown()
                while (release.count > 0) { try { release.await() } catch (_: InterruptedException) {} }
                "late"
            })
            assertTrue(entered.await(1, TimeUnit.SECONDS))
            assertNull(withTimeout(200) { gate.run(5_000) { fail("Must not queue a second worker") } })
        } finally { release.countDown() }
    }
    @Test fun cancellationInterruptsWorkerAndNextSongCanUseGate() = runBlocking {
        val gate = BlockingLookupGate(); val entered = CountDownLatch(1); val exited = CountDownLatch(1)
        val task = launch { gate.run(5_000) { entered.countDown(); try { CountDownLatch(1).await() } finally { exited.countDown() } } }
        yield(); assertTrue(entered.await(1, TimeUnit.SECONDS)); task.cancelAndJoin()
        assertTrue(exited.await(1, TimeUnit.SECONDS))
        withTimeout(1_000) { while (gate.run(200) { "fresh" } == null) yield() }
    }
    @Test fun legacyExceptionDoesNotPoisonTheGate() = runBlocking {
        val gate = BlockingLookupGate()
        try { gate.run(1_000) { throw IllegalStateException("test") }; fail("Expected failure") }
        catch (_: IllegalStateException) {}
        assertEquals("fresh", gate.run(1_000) { "fresh" })
    }
    @Test fun zeroBudgetDoesNotReserveWorker() = runBlocking {
        val gate = BlockingLookupGate(); assertNull(gate.run(0) { "unused" }); assertEquals("fresh",gate.run(1_000){"fresh"})
    }
    @Test fun cancelledParentDoesNotReturnAFalseMiss() = runBlocking {
        val gate=BlockingLookupGate();var returned=false
        val task=launch { gate.run(5_000) { Thread.sleep(5_000) };returned=true }
        yield();task.cancelAndJoin();assertFalse(returned)
    }

    /** Standard Java sockets are visible to Android's unit-test compiler; jdk.httpserver is not. */
    private class LocalServer(handler: (Socket) -> Unit) : Closeable {
        private val listener = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        private val active = AtomicReference<Socket?>()
        val url = "http://127.0.0.1:${listener.localPort}/"
        private val worker = kotlin.concurrent.thread(isDaemon = true, name = "USpot-probe-test") {
            try {
                listener.accept().use { socket ->
                    active.set(socket)
                    socket.soTimeout = 2_000
                    val input = socket.getInputStream().bufferedReader(Charsets.US_ASCII)
                    while (true) {
                        val line = input.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    handler(socket)
                }
            } catch (_: java.io.IOException) {
                // Closing a cancelled probe/fixture intentionally interrupts socket IO.
            }
        }
        override fun close() {
            listener.close()
            active.get()?.close()
            worker.join(1_000)
        }
    }
    private suspend fun serverTest(code: Int, mime: String, block: suspend (String) -> Unit) {
        LocalServer { socket ->
            val output = socket.getOutputStream()
            output.write(("HTTP/1.1 $code Test\r\nContent-Type: $mime\r\n" +
                "Content-Length: 20000\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
            output.write(ByteArray(20_000))
            output.flush()
        }.use { server -> block(server.url) }
    }
    private val httpClient = OkHttpClient()
    @org.junit.After fun shutdownNetwork() { httpClient.dispatcher.executorService.shutdownNow(); httpClient.connectionPool.evictAll() }
    private fun call(url:String)=httpClient.newCall(Request.Builder().url(url).build())
    @Test fun acceptsWorkingAudio()=runBlocking { serverTest(206,"audio/webm") { assertTrue(StreamUrlProbe.check(call(it),20_000)) } }
    @Test fun refusesForbiddenAudioInsteadOfPassingItToThePlayer()=runBlocking { serverTest(403,"audio/webm") { assertFalse(StreamUrlProbe.check(call(it),20_000)) } }
    @Test fun refusesHtmlSuccessPage()=runBlocking { serverTest(200,"text/html") { assertFalse(StreamUrlProbe.check(call(it),20_000)) } }
    @Test fun probesBodyWhenLengthIsUnknown()=runBlocking { serverTest(200,"audio/webm") { assertTrue(StreamUrlProbe.check(call(it),null)) } }
    private suspend fun stalledProbe(parentCancellation:Boolean) {
        val release=CountDownLatch(1);val entered=CountDownLatch(1)
        val server=LocalServer {
            entered.countDown()
            release.await(2,TimeUnit.SECONDS)
        }
        val request=call(server.url)
        try {
            if(parentCancellation) {
                val returned=AtomicBoolean(false)
                coroutineScope {
                    val task=launch { StreamUrlProbe.check(request,20_000,5_000);returned.set(true) }
                    yield();assertTrue(entered.await(1,TimeUnit.SECONDS));task.cancelAndJoin();assertFalse(returned.get())
                }
            } else assertTrue(withTimeout(1_000) { StreamUrlProbe.check(request,20_000,150) })
            assertTrue(request.isCanceled())
        } finally { release.countDown();server.close() }
    }
    @Test fun probeTimeoutCancelsNetworkCallAndAllowsPlayerAttempt()=runBlocking { stalledProbe(false) }
    @Test fun parentCancellationCancelsNetworkCallWithoutReportingSuccess()=runBlocking { stalledProbe(true) }
    @Test fun rejectedProbeReportsOnlyStatusCode()=runBlocking {
        var rejected=0
        serverTest(403,"audio/webm") { assertFalse(StreamUrlProbe.check(call(it),20_000,onRejected={code->rejected=code})) }
        assertEquals(403,rejected)
    }
    @Test fun acceptedProbeDoesNotReportRejection()=runBlocking {
        var rejected=0
        serverTest(206,"audio/webm") { assertTrue(StreamUrlProbe.check(call(it),20_000,onRejected={code->rejected=code})) }
        assertEquals(0,rejected)
    }

}
