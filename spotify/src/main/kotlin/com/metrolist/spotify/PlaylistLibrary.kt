package com.metrolist.spotify

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/** Changes library references only, never playlist contents. No ambiguous-write retries.
 * Rootlist endpoint/revision envelope: mirrorfm/spotify-private-api.
 * ADD/REM operations: Spotify playlist4_external.proto, as extracted by librespot.
 */
internal class PlaylistLibrary(private val client: HttpClient) {
    private val lock = Mutex()
    private val retryAt = mutableMapOf<String, Long>()
    private data class Snapshot(val revision: String, val uris: Set<String>)

    suspend fun setSaved(user: String, uris: List<String>, saved: Boolean, token: String) = lock.withLock {
        require(user.isNotBlank())
        require(uris.isNotEmpty() && uris.size <= 100 && uris.all { Regex("spotify:playlist:[A-Za-z0-9]+").matches(it) })
        val now = System.currentTimeMillis()
        retryAt[user]?.takeIf { it > now }?.let { throw Spotify.SpotifyException(429, "Wait before retrying the playlist library change",
            (it - now + 999) / 1000, "Playlist library") }
        val endpoint = "https://spclient.wg.spotify.com/playlist/v2/user/${user.encodeURLPathPart()}/rootlist"
        val session = SpotifyWebSession.awaitCurrent()
        fun HttpRequestBuilder.authorize() {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("app-platform", "WebPlayer")
            header(HttpHeaders.Accept, "application/json")
            session?.let { header("client-token", it.clientToken); it.userAgent?.let { ua -> header(HttpHeaders.UserAgent, ua) } }
        }
        suspend fun parse(response: HttpResponse): JsonObject {
            if (response.status.value !in 200..299) {
                val wait = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.coerceIn(1, 86400) ?: 30L
                if (response.status.value == 429) retryAt[user] = System.currentTimeMillis() + wait * 1000
                throw Spotify.SpotifyException(response.status.value, "Spotify could not change your saved playlists",
                    if (response.status.value == 429) wait else 0L, "Playlist library")
            }
            val result = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            if (result["error"] != null || (result["errors"] as? JsonArray)?.isNotEmpty() == true)
                throw Spotify.SpotifyException(400, "Spotify rejected the library change", endpoint = "Playlist library")
            return result
        }
        suspend fun snapshot(): Snapshot {
            var revision: String? = null
            val found = mutableSetOf<String>()
            var offset = 0
            while (offset <= 12000) {
                val page = parse(client.get(endpoint) {
                    authorize(); parameter("decorate", "revision,length"); parameter("from", offset); parameter("length", 500)
                })
                val rev = page["revision"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: throw Spotify.SpotifyException(502, "Spotify did not return a library revision", endpoint = "Playlist library")
                if (revision != null && revision != rev) throw Spotify.SpotifyException(409, "Your library changed. Please try again.", endpoint = "Playlist library")
                revision = rev
                val contents = page["contents"]?.jsonObject
                    ?: throw Spotify.SpotifyException(502, "Spotify did not return library contents", endpoint = "Playlist library")
                val items = (contents["items"] as? JsonArray).orEmpty()
                items.mapNotNullTo(found) { it.jsonObject["uri"]?.jsonPrimitive?.contentOrNull }
                val pos = contents["pos"]?.jsonPrimitive?.intOrNull ?: offset
                require(pos == offset) { "Spotify returned an unexpected library page" }
                offset += items.size
                val truncated = contents["truncated"]?.jsonPrimitive?.booleanOrNull ?: false
                val total = page["length"]?.jsonPrimitive?.intOrNull
                if (!truncated && (total == null || offset >= total)) return Snapshot(rev, found)
                require(items.isNotEmpty()) { "Spotify returned an incomplete library" }
            }
            throw Spotify.SpotifyException(502, "Library too large to confirm safely", endpoint = "Playlist library")
        }
        val before = snapshot()
        val changes = uris.distinct().filter { (it in before.uris) != saved }
        if (changes.isEmpty()) return@withLock
        val body = buildJsonObject {
            put("baseRevision", before.revision)
            putJsonArray("deltas") { add(buildJsonObject {
                putJsonArray("ops") { add(buildJsonObject {
                    put("kind", if (saved) "ADD" else "REM")
                    putJsonObject(if (saved) "add" else "rem") {
                        put(if (saved) "addFirst" else "itemsAsKey", true)
                        putJsonArray("items") { changes.forEach { uri -> add(buildJsonObject { put("uri", uri) }) } }
                    }
                }) }
                putJsonObject("info") { putJsonObject("source") { put("client", "WEBPLAYER") } }
            }) }
        }
        parse(client.post("$endpoint/changes") { authorize(); setBody(TextContent(body.toString(), ContentType.Application.Json)) })
        val after = snapshot()
        if (!uris.all { (it in after.uris) == saved }) throw Spotify.SpotifyException(502,
            "Spotify has not confirmed this library change. Keep the playlist and retry saving it.", endpoint = "Playlist library")
    }
}
