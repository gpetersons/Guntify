package com.metrolist.spotify

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.*

/** Web Player playlist changes. One request only: an ambiguous write must not be replayed. */
internal class PlaylistChanges(private val client: HttpClient) {
    suspend fun add(id: String, uris: List<String>, token: String) = change(id, token, buildJsonObject {
        require(uris.isNotEmpty() && uris.size <= 100 && uris.all { Regex("spotify:track:[A-Za-z0-9]+").matches(it) })
        put("kind", "ADD")
        putJsonObject("add") { put("addLast", true); putJsonArray("items") { uris.forEach { uri -> add(buildJsonObject { put("uri", uri) }) } } }
    })
    suspend fun remove(id: String, items: List<Spotify.PlaylistItemRef>, token: String) = change(id, token, buildJsonObject {
        require(items.isNotEmpty() && items.size <= 100 && items.all { it.uid.isNotBlank() && Regex("spotify:track:[A-Za-z0-9]+").matches(it.uri) })
        put("kind", "REM")
        putJsonObject("rem") { put("itemsAsKey", true); putJsonArray("items") { items.forEach { item -> add(buildJsonObject { put("uri", item.uri); put("uid", item.uid) }) } } }
    })
    private suspend fun change(id: String, token: String, operation: JsonObject) {
        require(Regex("[A-Za-z0-9]+").matches(id)) { "Invalid playlist" }
        val body = buildJsonObject { putJsonArray("deltas") { add(buildJsonObject {
            putJsonArray("ops") { add(operation) }
            putJsonObject("info") { putJsonObject("source") { put("client", "WEBPLAYER") } }
        }) } }
        val response = client.post("https://spclient.wg.spotify.com/playlist/v2/playlist/$id/changes") {
            header("Authorization", "Bearer $token")
            header("app-platform", "WebPlayer")
            SpotifyWebSession.current()?.let { session -> header("client-token", session.clientToken); session.userAgent?.let { header("User-Agent", it) } }
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw Spotify.SpotifyException(response.status.value,
            "Spotify could not change this playlist", response.headers["Retry-After"]?.toLongOrNull() ?: 0L, "Playlist songs")
        if (raw.isNotBlank()) {
            val result = Json.parseToJsonElement(raw).jsonObject
            if (result["error"] != null || (result["errors"] as? JsonArray)?.isNotEmpty() == true)
                throw Spotify.SpotifyException(400, "Spotify did not accept the playlist change", endpoint = "Playlist songs")
        }
    }
}
