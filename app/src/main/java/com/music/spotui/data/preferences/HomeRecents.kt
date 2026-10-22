package com.music.spotui.data.preferences

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Recently started playlists; recorded only when playback begins in a playlist screen. */
object HomeRecents {
    private const val PREF = "spotui_modified_home_recents"
    data class PlaylistEntry(val id: String, val name: String, val image: String)
    private fun prefs(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    fun showPlaylists(context: Context) = prefs(context).getBoolean("playlists", true)
    fun showRadios(context: Context) = prefs(context).getBoolean("radios", true)
    fun maxItems(context: Context) = prefs(context).getInt("max", 8).coerceIn(2, 20)
    fun setPlaylists(context: Context, value: Boolean) { prefs(context).edit().putBoolean("playlists", value).apply() }
    fun setRadios(context: Context, value: Boolean) { prefs(context).edit().putBoolean("radios", value).apply() }
    fun setMaxItems(context: Context, value: Int) { prefs(context).edit().putInt("max", value.coerceIn(2, 20)).apply() }
    fun playlists(context: Context): List<PlaylistEntry> = runCatching {
        val array = JSONArray(prefs(context).getString("recent_playlists", "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            PlaylistEntry(obj.optString("id"), obj.optString("name"), obj.optString("image"))
        }.filter { it.id.isNotBlank() }
    }.getOrDefault(emptyList())
    fun rememberPlaylist(context: Context, id: String, name: String, image: String) {
        if (id.isBlank()) return
        HomeFrequency.played(context, "playlist", id, name, image, "Playlist")
        val items = (listOf(PlaylistEntry(id, name, image)) + playlists(context).filterNot { it.id == id }).take(20)
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("image", it.image)) }
        prefs(context).edit().putString("recent_playlists", array.toString()).apply()
    }
    fun clear(context: Context) {
        HomeFrequency.clear(context)
        prefs(context).edit().remove("recent_playlists").apply()
        // Keep radio seeds and resume positions; clearing shortcuts must not lose session identity.
    }
}
