package com.music.spotui.webui

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.music.spotui.di.SongPlayer
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException

/** One explicitly started Wi-Fi session; never accepts arbitrary URLs or file paths. */
internal class WebUiServer(private val context: Context, private val address: String, port: Int, private val access: WebUiAccess) : NanoHTTPD(address, port) {
    private val authority = "$address:$port"
    private val backend = WebUiBackend(context)
    private val lookups = Semaphore(2)
    private val mediaSlots = Semaphore(4)
    private val active = ConcurrentHashMap.newKeySet<DataSource>()
    @Volatile private var closed = false

    init {
        setAsyncRunner(object : AsyncRunner {
            private val clients = ConcurrentHashMap.newKeySet<ClientHandler>()
            private val workers = ThreadPoolExecutor(12, 12, 30, TimeUnit.SECONDS,
                ArrayBlockingQueue<Runnable>(12), { task -> Thread(task, "Guntify-LAN").apply { isDaemon = true } })
            override fun exec(client: ClientHandler) {
                clients.add(client)
                try { workers.execute(client) }
                catch (_: RejectedExecutionException) { clients.remove(client); client.close() }
            }
            override fun closed(client: ClientHandler) { clients.remove(client) }
            override fun closeAll() {
                clients.forEach { it.close() }; clients.clear(); workers.shutdownNow()
            }
        })
    }
    fun currentPin(): String = access.currentPin()

    override fun stop() {
        closed = true
        access.invalidate()
        backend.close()
        active.forEach { runCatching { it.close() } }
        active.clear()
        super.stop()
    }
    private fun response(status: Response.Status, mime: String, text: String): Response =
        newFixedLengthResponse(status, mime, text).also(::headers)
    private fun json(status: Response.Status, value: JSONObject) = response(status, "application/json; charset=utf-8", value.toString())
    private fun error(status: Response.Status, message: String) = json(status, JSONObject().put("error", message))
    private fun headers(r: Response) {
        r.closeConnection(true)
        r.addHeader("Cache-Control", "no-store")
        r.addHeader("Referrer-Policy", "no-referrer")
        r.addHeader("X-Content-Type-Options", "nosniff")
        r.addHeader("X-Frame-Options", "DENY")
        r.addHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' https: data:; media-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'")
    }
    override fun serve(session: IHTTPSession): Response {
        if (closed) return error(Response.Status.SERVICE_UNAVAILABLE, "Web UI stopped.")
        if (session.headers["host"] != authority || !WebUiProtocol.allowedOrigin(session.headers["origin"], authority) ||
            session.headers["sec-fetch-site"] == "cross-site") return error(Response.Status.FORBIDDEN, "Open the address shown on your phone.")
        val path = session.uri
        if (session.method == Method.GET && path in setOf("/", "/app.js", "/style.css", "/logo.svg")) {
            val asset = when(path) { "/" -> "webui/index.html"; "/logo.svg" -> "webui/logo.svg"; else -> "webui/${path.drop(1)}" }
            val mime = when(path) { "/app.js" -> "text/javascript"; "/style.css" -> "text/css"; "/logo.svg" -> "image/svg+xml"; else -> "text/html; charset=utf-8" }
            return newChunkedResponse(Response.Status.OK, mime, context.assets.open(asset)).also(::headers)
        }
        // Never mint a browser session with GET. Every new browser needs the PIN
        // currently visible on the Android device.
        if (path == "/session") return error(Response.Status.NOT_FOUND, "Not found")
        if (path == "/pair") {
            if (session.method != Method.POST) return error(Response.Status.METHOD_NOT_ALLOWED, "Use POST.")
            if (!WebUiProtocol.sameOrigin(session.headers["origin"], authority))
                return error(Response.Status.FORBIDDEN, "This browser origin is not allowed.")
            val type = session.headers["content-type"]?.substringBefore(';')?.trim()?.lowercase()
            val size = session.headers["content-length"]?.toLongOrNull()
            if (type != "application/json" || size == null || size !in 1..128 || session.headers.containsKey("transfer-encoding"))
                return error(Response.Status.BAD_REQUEST, "Invalid pairing request.")
            return try {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val pin = JSONObject(files["postData"] ?: "{}").optString("pin")
                val result = access.pair(pin)
                when (result.result) {
                    WebUiAccess.Result.PAIRED -> json(Response.Status.OK, JSONObject().put("ok", true)).also {
                        it.addHeader("Set-Cookie", "guntify_web=${result.session}; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800")
                    }
                    WebUiAccess.Result.RATE_LIMITED -> error(Response.Status.TOO_MANY_REQUESTS, "Too many attempts. Try again in one minute or generate a new PIN on the phone.")
                    WebUiAccess.Result.EXPIRED -> error(Response.Status.UNAUTHORIZED, "PIN expired. Use the new PIN shown on the phone.")
                    WebUiAccess.Result.CLOSED -> error(Response.Status.SERVICE_UNAVAILABLE, "Desktop mode stopped.")
                    WebUiAccess.Result.INVALID -> error(Response.Status.UNAUTHORIZED, "Incorrect PIN. Check the code on the phone.")
                }
            } catch (_: Exception) { error(Response.Status.BAD_REQUEST, "Invalid pairing request.") }
        }
        val authenticated = access.authorized(session.headers["cookie"])
        // Browsers include Origin on same-origin writes. Reject missing/foreign
        // origins as an extra CSRF barrier for all state-changing endpoints.
        if ((path == "/api" || path == "/restore") &&
            !WebUiProtocol.sameOrigin(session.headers["origin"], authority))
            return error(Response.Status.FORBIDDEN, "This browser origin is not allowed.")
        if (path == "/ping" && session.method == Method.GET) {
            return if (authenticated) json(Response.Status.OK, JSONObject().put("ok", true))
            else error(Response.Status.UNAUTHORIZED, "Reconnect this browser.")
        }
        if (path == "/api") {
            if (session.method != Method.POST) return error(Response.Status.METHOD_NOT_ALLOWED, "Use POST.")
            val size = session.headers["content-length"]?.toLongOrNull()
            if (size == null || size !in 1..1_048_576 || session.headers.containsKey("transfer-encoding"))
                return error(Response.Status.BAD_REQUEST, "Invalid request size.")
            if (!authenticated) return error(Response.Status.UNAUTHORIZED, "Reconnect this browser.")
            var acquired = false
            return try {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val body = JSONObject(files["postData"] ?: "{}")
                if (path == "/api" && body.optString("action") !in setOf("started", "listened", "ping", "adopt", "saveRadioQueue", "songState")) {
                    if (!lookups.tryAcquire()) return error(Response.Status.SERVICE_UNAVAILABLE, "Another request is loading. Try again shortly.")
                    acquired = true
                }
                json(Response.Status.OK, runBlocking(Dispatchers.IO) { backend.command(body) })
            } catch (failure: Exception) {
                // No provider URLs, cookies, stack traces or tokens in LAN responses.
                error(Response.Status.SERVICE_UNAVAILABLE, when(failure) {
                    is WebUiActionFailure -> failure.message.orEmpty()
                    is IllegalArgumentException -> "This request is unavailable. Refresh and choose the item again."
                    else -> "Couldn't load this right now. Check your phone's connection and try again."
                })
            } finally { if (acquired) lookups.release() }
        }
        if (path.startsWith("/image/") && session.method == Method.GET) {
            if (!authenticated) return error(Response.Status.UNAUTHORIZED, "Reconnect this browser.")
            val uri = backend.imageUri(path.removePrefix("/image/")) ?: return error(Response.Status.NOT_FOUND, "Image unavailable")
            return try {
                val parsed = if(uri.startsWith("/")) Uri.fromFile(java.io.File(uri)) else Uri.parse(uri)
                val input = context.contentResolver.openInputStream(parsed) ?: return error(Response.Status.NOT_FOUND, "Image unavailable")
                newChunkedResponse(Response.Status.OK, context.contentResolver.getType(parsed) ?: "image/jpeg", input).also(::headers)
            } catch (_: Exception) { error(Response.Status.NOT_FOUND, "Image unavailable") }
        }
        if (path == "/backup" && session.method == Method.GET) {
            if (!authenticated) return error(Response.Status.UNAUTHORIZED, "Reconnect this browser.")
            val file = java.io.File.createTempFile("web-backup-", ".zip", context.cacheDir)
            return try {
                check(com.music.spotui.di.Downloads.downloadingSnapshot().isEmpty())
                com.music.spotui.data.backup.UserBackup.saveBackup(context, Uri.fromFile(file), false)
                val input = object : java.io.FilterInputStream(file.inputStream()) {
                    override fun close() { try { super.close() } finally { file.delete() } }
                }
                newFixedLengthResponse(Response.Status.OK, "application/zip", input, file.length()).also {
                    headers(it); it.addHeader("Content-Disposition", "attachment; filename=Guntify-backup.zip")
                }
            } catch (_: Exception) { file.delete(); error(Response.Status.SERVICE_UNAVAILABLE, "Couldn't create a backup. Try exporting from your phone.") }
        }
        if (path == "/restore" && session.method == Method.PUT) {
            if (!authenticated) return error(Response.Status.UNAUTHORIZED, "Reconnect this browser.")
            val size = session.headers["content-length"]?.toLongOrNull()
            if (size == null || size !in 1..33_554_432 || session.headers.containsKey("transfer-encoding"))
                return error(Response.Status.BAD_REQUEST, "Choose a backup under 32 MB. Import larger audio backups on your phone.")
            if (!lookups.tryAcquire()) return error(Response.Status.SERVICE_UNAVAILABLE, "Wait for the current request to finish.")
            return try {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val file = java.io.File(files["content"] ?: error("Missing backup"))
                check(com.music.spotui.di.Downloads.downloadingSnapshot().isEmpty())
                val player = dagger.hilt.android.EntryPointAccessors.fromApplication(context, WebUiPlayerEntryPoint::class.java).currentSongState()
                runBlocking(Dispatchers.Main) { SongPlayer.pause(); SongPlayer.stop() }
                com.music.spotui.data.backup.UserBackup.restore(context, Uri.fromFile(file))
                runBlocking(Dispatchers.Main) { com.music.spotui.data.backup.BackupRestoreEffects.apply(context, player) }
                backend.restored()
                json(Response.Status.OK, JSONObject().put("ok", true))
            } catch (_: Exception) {
                error(Response.Status.BAD_REQUEST, "Couldn't import this backup. It must be a complete Guntify data export with valid files. Your existing data was retained.")
            } finally { lookups.release() }
        }
        if (path.startsWith("/audio/") && session.method in setOf(Method.GET, Method.HEAD)) {
            if (!authenticated) return error(Response.Status.UNAUTHORIZED, "Reconnect this browser.")
            val ticket = backend.ticket(path.removePrefix("/audio/")) ?: return error(Response.Status.NOT_FOUND, "Play the song again to refresh its stream.")
            return audio(session, ticket)
        }
        return error(Response.Status.NOT_FOUND, "Not found")
    }
    private fun audio(session: IHTTPSession, ticket: WebUiBackend.Ticket): Response {
        if (!mediaSlots.tryAcquire(2, TimeUnit.SECONDS)) return error(Response.Status.SERVICE_UNAVAILABLE, "Too many audio connections. Pause another browser.")
        var ds: DataSource? = null
        try {
            val range = session.headers["range"]
            var offset = if (range != null) {
                require(range.startsWith("bytes=") && ',' !in range)
                range.removePrefix("bytes=").substringBefore('-').toLongOrNull() ?: 0L
            } else 0L
            require(offset >= 0)
            fun open(position: Long): Long {
                ds?.let { active.remove(it); runCatching { it.close() } }
                val source = SongPlayer.browserDataSource(context)
                ds = source
                active.add(source)
                return source.open(DataSpec.Builder().setUri(Uri.parse(ticket.uri)).setPosition(position).build())
            }
            var remaining = open(offset)
            var total = if (remaining >= 0) Math.addExact(offset, remaining) else -1L
            val window = if (range != null && total > 0) WebUiProtocol.range(range, total) else null
            // Browsers commonly send bytes=0- on the initial play request. A source
            // with no known length can still stream from the beginning as HTTP 200.
            if (range != null && total <= 0 && range != "bytes=0-") throw IllegalArgumentException("Unknown range")
            if (window != null && window.start != offset) { offset = window.start; remaining = open(offset) }
            val count = window?.length ?: remaining
            val source = requireNotNull(ds)
            val mime = ticket.mime ?: source.responseHeaders.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value?.firstOrNull()
                ?: "audio/mpeg"
            val input = object : InputStream() {
                private var left = count
                private var ended = false
                override fun read(): Int { val b = ByteArray(1); return if(read(b,0,1)<0)-1 else b[0].toInt() and 255 }
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (len == 0) return 0
                    if (closed || ended || left == 0L) return -1
                    val n = source.read(b, off, if(left < 0)len else minOf(left, len.toLong()).toInt())
                    if (n == C.RESULT_END_OF_INPUT) return -1
                    if (left >= 0) left -= n
                    return n
                }
                override fun close() { if(!ended) { ended=true; active.remove(source); runCatching { source.close() }; mediaSlots.release() } }
            }
            val status = if (window == null) Response.Status.OK else Response.Status.PARTIAL_CONTENT
            val reply = if(count >= 0)newFixedLengthResponse(status, mime.substringBefore(';'), input, count) else newChunkedResponse(status,mime.substringBefore(';'),input)
            headers(reply)
            if (total > 0) reply.addHeader("Accept-Ranges", "bytes")
            if (window != null) reply.addHeader("Content-Range", "bytes ${window.start}-${window.end}/$total")
            return reply
        } catch (e: Exception) {
            ds?.let { active.remove(it); runCatching { it.close() } }
            mediaSlots.release()
            return error(if(e is IllegalArgumentException)Response.Status.RANGE_NOT_SATISFIABLE else Response.Status.SERVICE_UNAVAILABLE,
                "Audio could not be opened. Press Retry or select the song again.")
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
internal interface WebUiPlayerEntryPoint {
    fun currentSongState(): com.music.spotui.di.CurrentSongState
}
