package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.SongsModel
import org.json.JSONObject

// Persists the last playing track + position so a fresh app launch can restore
// the session (mini player shows the track, play resumes where it stopped).

private const val PREF = "PlaybackState"
private const val KEY_SONG = "song"
private const val KEY_POSITION = "positionMs"
fun savePlaybackActive(context: Context, playing: Boolean) {
    context.getSharedPreferences(PREF, 0).edit().putBoolean("playing", playing).apply()
}
fun wasPlaybackActive(context: Context) = context.getSharedPreferences(PREF, 0).getBoolean("playing", false)

/** Saves the track (resets the stored position when the track changed). */
fun saveLastPlayback(context: Context, song: SongsModel) {
    if (song.title.isBlank() || song.url.isBlank()) return
    val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    val prevId = runCatching { p.getString(KEY_SONG, null)?.let { JSONObject(it).optInt("id", -1) } }
        .getOrNull() ?: -1
    val json = JSONObject().apply {
        put("id", song.id); put("title", song.title); put("album", song.album)
        put("singer", song.singer); put("coverUri", song.coverUri)
        put("url", song.url); put("spotifyTrackId", song.spotifyTrackId)
        put("explicit", song.explicit); put("durationMs", song.durationMs)
    }
    p.edit().apply {
        putString(KEY_SONG, json.toString())
        if (prevId != song.id) putLong(KEY_POSITION, 0L)
    }.apply()
}

/** Updates just the position of the stored track (called periodically). */
fun saveLastPosition(context: Context, positionMs: Long) {
    if (positionMs <= 0) return
    context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .edit().putLong(KEY_POSITION, positionMs).apply()
}

/** The last track and position, or null if nothing was ever played. */
fun loadLastPlayback(context: Context): Pair<SongsModel, Long>? {
    val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    val raw = p.getString(KEY_SONG, null) ?: return null
    return runCatching {
        val o = JSONObject(raw)
        val song = SongsModel(
            id = o.optInt("id", -1),
            title = o.optString("title"),
            album = o.optString("album"),
            singer = o.optString("singer"),
            coverUri = o.optString("coverUri"),
            url = o.optString("url"),
            spotifyTrackId = o.optString("spotifyTrackId"),
            explicit = o.optBoolean("explicit", false),
            durationMs = o.optInt("durationMs", 0),
        )
        if (song.title.isBlank() || song.url.isBlank()) null
        else song to com.music.spotui.audio.PlaybackClockPolicy.position(p.getLong(KEY_POSITION, 0L), song.durationMs.toLong())
    }.getOrNull()
}


data class PlaybackQueueSnapshot(val songs: List<SongsModel>, val collection: com.music.spotui.data.entity.PlaybackCollection?,
    val radioSeed: String?, val shuffled: Boolean, val original: List<SongsModel>?, val playedThroughId: Int? = null,
    val smartShuffle: Boolean = false, val smartBase: List<SongsModel>? = null, val smartAdded: Set<Int> = emptySet(), val manualQueued: Set<Int> = emptySet())
private fun songJson(song: SongsModel) = JSONObject().put("id", song.id).put("title", song.title).put("album", song.album)
    .put("singer", song.singer).put("cover", song.coverUri).put("url", song.url).put("spotify", song.spotifyTrackId)
    .put("duration", song.durationMs).put("explicit", song.explicit)
private fun readQueue(raw: org.json.JSONArray?): List<SongsModel> = (0 until (raw?.length() ?: 0)).mapNotNull { i ->
    val o = raw?.optJSONObject(i) ?: return@mapNotNull null
    SongsModel(o.optInt("id"), o.optString("title"), o.optString("album"), o.optString("singer"), o.optString("cover"),
        o.optString("url"), o.optString("spotify"), o.optBoolean("explicit"), o.optInt("duration"))
}
fun savePlaybackQueue(context: Context, snapshot: PlaybackQueueSnapshot) {
    val json = JSONObject().put("songs", org.json.JSONArray().apply { snapshot.songs.forEach { put(songJson(it)) } })
        .put("radio", snapshot.radioSeed.orEmpty()).put("shuffle", snapshot.shuffled)
    snapshot.playedThroughId?.let { json.put("playedThrough", it) }
    json.put("manualQueued", org.json.JSONArray().also { a -> snapshot.manualQueued.forEach { a.put(it) } })
    json.put("smartShuffle", snapshot.smartShuffle)
    snapshot.smartBase?.let { tracks -> json.put("smartBase", org.json.JSONArray().apply { tracks.forEach { put(songJson(it)) } }) }
    json.put("smartAdded", org.json.JSONArray().apply { snapshot.smartAdded.forEach { put(it) } })
    snapshot.original?.let { tracks -> json.put("original", org.json.JSONArray().apply { tracks.forEach { put(songJson(it)) } }) }
    snapshot.collection?.let { c -> json.put("collection", JSONObject().put("key", c.key).put("type", c.type).put("id", c.id)
        .put("name", c.name).put("image", c.image).put("subtitle", c.subtitle)) }
    context.getSharedPreferences(PREF, 0).edit().putString("queue", json.toString()).apply()
}
fun loadPlaybackQueue(context: Context): PlaybackQueueSnapshot? = runCatching {
    val raw = context.getSharedPreferences(PREF, 0).getString("queue", null) ?: return@runCatching null
    val o = JSONObject(raw)
    val c = o.optJSONObject("collection")?.let { com.music.spotui.data.entity.PlaybackCollection(it.optString("key"), it.optString("type"),
        it.optString("id"), it.optString("name"), it.optString("image"), it.optString("subtitle")) }
    PlaybackQueueSnapshot(readQueue(o.optJSONArray("songs")), c, o.optString("radio").takeIf { it.isNotBlank() },
        o.optBoolean("shuffle"), o.optJSONArray("original")?.let(::readQueue),
        if (o.has("playedThrough") && !o.isNull("playedThrough")) o.optInt("playedThrough") else null,
        o.optBoolean("smartShuffle", false), o.optJSONArray("smartBase")?.let(::readQueue),
        o.optJSONArray("smartAdded")?.let { a -> (0 until a.length()).map { a.optInt(it) }.toSet() }.orEmpty(),
        o.optJSONArray("manualQueued")?.let { a -> (0 until a.length()).map { a.optInt(it) }.toSet() }.orEmpty())
}.getOrNull()
