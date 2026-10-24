package com.music.spotui.ui.notification

import com.music.spotui.data.entity.SongsModel
import org.json.JSONArray
import org.json.JSONObject

/** Small restart-safe catalog for dashboard items that the car may retain after service death. */
object CarSuggestionStore {
    const val LIMIT = 40
    fun encode(songs: List<SongsModel>): String = JSONArray().apply {
        songs.distinctBy { it.id }.take(LIMIT).forEach { s -> put(JSONObject()
            .put("id", s.id).put("title", s.title).put("album", s.album).put("artist", s.singer)
            .put("art", s.coverUri).put("query", s.url).put("spotify", s.spotifyTrackId)
            .put("explicit", s.explicit).put("duration", s.durationMs)) }
    }.toString()
    fun decode(raw: String): List<SongsModel> = runCatching {
        val a = JSONArray(raw)
        (0 until minOf(a.length(), LIMIT)).mapNotNull { i -> runCatching {
            val s = a.getJSONObject(i)
            SongsModel(s.getInt("id"), s.getString("title"), s.optString("album"),
                s.optString("artist"), s.optString("art"), s.getString("query"),
                s.optString("spotify"), s.optBoolean("explicit"), s.optInt("duration"))
                .takeIf { it.url.isNotBlank() && it.title.isNotBlank() }
        }.getOrNull() }
    }.getOrDefault(emptyList())
}
