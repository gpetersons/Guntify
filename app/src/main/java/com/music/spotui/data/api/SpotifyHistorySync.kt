package com.music.spotui.data.api

import android.content.Context
import android.util.Log
import com.metrolist.spotify.Spotify
import com.music.spotui.data.preferences.HistoryEntry
import com.music.spotui.data.preferences.mergeSpotifyHistory
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Import authorized Spotify recently-played tracks; never submits external plays to Spotify. */
object SpotifyHistorySync {
    suspend fun refresh(context: Context): Boolean {
        if (!SpotifyTokenProvider.ensureToken(context.applicationContext)) return false
        val token = Spotify.accessToken ?: return false
        return runCatching {
            val conn = (URL("https://api.spotify.com/v1/me/player/recently-played?limit=50")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
            }
            val body = try {
                if (conn.responseCode != 200) {
                    Log.w("SpotifyHistorySync", "Recently played unavailable: HTTP ${conn.responseCode}")
                    return false
                }
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally { conn.disconnect() }
            val items = JSONObject(body).optJSONArray("items") ?: return false
            val plays = (0 until items.length()).mapNotNull { i ->
                val item = items.optJSONObject(i) ?: return@mapNotNull null
                val track = item.optJSONObject("track") ?: return@mapNotNull null
                val id = track.optString("id")
                if (id.isBlank()) return@mapNotNull null
                val album = track.optJSONObject("album")
                val artists = track.optJSONArray("artists")
                val singer = (0 until (artists?.length() ?: 0)).mapNotNull { j ->
                    artists?.optJSONObject(j)?.optString("name")?.takeIf { it.isNotBlank() }
                }.joinToString(", ")
                val images = album?.optJSONArray("images")
                val image = images?.optJSONObject(0)?.optString("url").orEmpty()
                val timestamp = runCatching { java.time.Instant.parse(item.optString("played_at")).toEpochMilli() }.getOrNull()
                    ?: return@mapNotNull null
                HistoryEntry(ts = timestamp, songId = id.hashCode(), title = track.optString("name"),
                    singer = singer, album = album?.optString("name").orEmpty(), image = image,
                    spotifyTrackId = id, source = "spotify")
            }
            mergeSpotifyHistory(context.applicationContext, plays)
            true
        }.onFailure { Log.w("SpotifyHistorySync", "Spotify history import failed", it) }.getOrDefault(false)
    }
}
