package com.music.spotui.data.preferences

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local listening history: one entry per playback start, newest first, capped.
 * Backs the History & stats screen; individual entries can be deleted and the
 * whole log cleared.
 */
data class HistoryEntry(
    val ts: Long,
    val songId: Int,
    val title: String,
    val singer: String,
    val album: String = "",
    val image: String = "",
    val url: String = "",
    val spotifyTrackId: String = "",
    val source: String = "spotui",
    val listenedMs: Long = 0L,
    val durationMs: Int = 0,
    val visitId: String = "",
)

object ListeningHistorySignals { val revision = kotlinx.coroutines.flow.MutableStateFlow(0L) }
object ListeningStatsSignals { val revision = kotlinx.coroutines.flow.MutableStateFlow(0L) }

private const val PREF = "ListeningHistory"
private const val KEY = "entries"
private const val MAX_ENTRIES = 10000

private fun HistoryEntry.toJson() = JSONObject().apply {
    put("ts", ts); put("songId", songId); put("title", title)
    put("singer", singer); put("album", album); put("image", image)
    put("url", url); put("spotifyTrackId", spotifyTrackId); put("source", source); put("listenedMs", listenedMs)
    put("durationMs", durationMs); put("visitId", visitId)
}

private fun JSONObject.toEntry() = HistoryEntry(
    ts = optLong("ts"), songId = optInt("songId", -1), title = optString("title"),
    singer = optString("singer"), album = optString("album"), image = optString("image"),
    url = optString("url"), spotifyTrackId = optString("spotifyTrackId"),
    source = optString("source", "spotui"),
    listenedMs = optLong("listenedMs", 0L),
    durationMs = optInt("durationMs", 0), visitId = optString("visitId"),
)

fun getListeningHistory(context: Context): List<HistoryEntry> {
    val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
        ?: return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getJSONObject(it).toEntry() }
    }.getOrDefault(emptyList())
}

private fun save(context: Context, entries: List<HistoryEntry>) {
    val arr = JSONArray().also { a -> entries.forEach { a.put(it.toJson()) } }
    context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .edit().putString(KEY, arr.toString()).apply()
}

@Synchronized
fun addListeningHistory(context: Context, entry: HistoryEntry) {
    if (entry.title.isBlank()) return
    val existing = getListeningHistory(context)
    if (com.music.spotui.audio.ListeningQualification.qualifies(entry.listenedMs)) {
        // Qualify the most recent local start, instead of counting it twice in History.
        val sameVisit = entry.visitId.takeIf { it.isNotBlank() }?.let { id -> existing.indexOfFirst { it.visitId == id } } ?: -1
        val index = if (sameVisit >= 0) sameVisit else existing.indexOfFirst { it.songId == entry.songId && it.source != "spotify" && it.listenedMs == 0L }
        if (index >= 0) {
            val updated = existing.toMutableList()
            updated[index] = updated[index].copy(listenedMs = maxOf(updated[index].listenedMs, entry.listenedMs),
                visitId = entry.visitId, durationMs = entry.durationMs)
            save(context, updated)
        } else save(context, (listOf(entry) + existing).take(MAX_ENTRIES))
        if (sameVisit < 0) ListeningHistorySignals.revision.value += 1L
        ListeningStatsSignals.revision.value += 1L
        return
    }
    // Don't log the same track twice in a row (pause/resume, seek restarts).
    if (existing.firstOrNull()?.songId == entry.songId) return
    save(context, (listOf(entry) + existing).take(MAX_ENTRIES))
}

fun removeListeningHistory(context: Context, entry: HistoryEntry) {
    save(context, getListeningHistory(context).filterNot { it.ts == entry.ts && it.songId == entry.songId })
    ListeningHistorySignals.revision.value += 1L
}

fun clearListeningHistory(context: Context) {
    context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    ListeningHistorySignals.revision.value += 1L
}

/** Import Spotify plays without replacing local Spotui plays or duplicating synced entries. */
@Synchronized
fun mergeSpotifyHistory(context: Context, incoming: List<HistoryEntry>) {
    if (incoming.isEmpty()) return
    val existing = getListeningHistory(context)
    val keys = existing.map { it.ts to it.spotifyTrackId }.toHashSet()
    val fresh = incoming.filter { it.spotifyTrackId.isNotBlank() && keys.add(it.ts to it.spotifyTrackId) }
    if (fresh.isNotEmpty()) save(context, (existing + fresh).sortedByDescending { it.ts }.take(MAX_ENTRIES))
}
