package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.SongsModel
import org.json.JSONArray
import org.json.JSONObject

/** Local radio seeds, ordered newest first. Never sent to Spotify. */
object RadioSessions {
    private const val PREF = "spotui_radio_sessions"
    private const val KEY = "seeds"
    private fun encode(song: SongsModel) = JSONObject().apply {
        put("id", song.id); put("title", song.title); put("album", song.album)
        put("singer", song.singer); put("cover", song.coverUri); put("url", song.url)
        put("spotify", song.spotifyTrackId); put("explicit", song.explicit)
        put("duration", song.durationMs)
    }
    fun get(context: Context): List<SongsModel> = runCatching {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]")
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            SongsModel(o.optInt("id"), o.optString("title"), o.optString("album"),
                o.optString("singer"), o.optString("cover"), o.optString("url"),
                o.optString("spotify"), o.optBoolean("explicit"), o.optInt("duration"))
        }
    }.getOrDefault(emptyList())
    @Synchronized fun remember(context: Context, song: SongsModel) {
        HomeFrequency.played(context, "radio", song.spotifyTrackId.ifBlank { song.id.toString() }, song.title + " Radio", song.coverUri, song.singer)
        val existing=get(context)
        val incoming=if(isMix(song.spotifyTrackId))existing.firstOrNull{it.spotifyTrackId==song.spotifyTrackId} ?: song else song
        val items = (listOf(incoming) + existing.filterNot {
            if (song.spotifyTrackId.isNotBlank()) it.spotifyTrackId == song.spotifyTrackId else it.id == song.id
        })
        val arr = JSONArray().apply { items.forEach { put(encode(it)) } }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
    private const val MIX_PREFIX = "uspot-mix:"
    fun isMix(id: String) = id.startsWith(MIX_PREFIX)
    fun stationName(marker: SongsModel) = if (isMix(marker.spotifyTrackId)) marker.title else "${marker.title} Radio"
    private fun decode(o: JSONObject) = SongsModel(o.optInt("id"),o.optString("title"),o.optString("album"),
        o.optString("singer"),o.optString("cover"),o.optString("url"),o.optString("spotify"),o.optBoolean("explicit"),o.optInt("duration"))
    private fun store(context: Context) = context.getSharedPreferences(PREF,0)
    fun anchors(context: Context, stationId: String): List<SongsModel> {
        if (!isMix(stationId)) return get(context).firstOrNull { it.spotifyTrackId == stationId }?.let { listOf(it) }.orEmpty()
        return runCatching {
            val array = JSONArray(store(context).getString("mix:$stationId", "[]"))
            (0 until array.length()).map { decode(array.getJSONObject(it)) }.take(5)
        }.getOrDefault(emptyList())
    }
    @Synchronized fun create(context: Context, input: List<SongsModel>, name: String, mix: Boolean): SongsModel {
        val songs = com.music.spotui.data.recommendation.RadioMixPolicy.selected(input)
        require(songs.isNotEmpty() && input.size <= 5 && (mix || songs.size == 1)) { "Choose one to five catalog songs." }
        if (!mix) { remember(context,songs.first()); return songs.first() }
        val id = MIX_PREFIX + java.util.UUID.randomUUID().toString()
        val marker = songs.first().copy(id=id.hashCode(),title=name.trim().take(80).ifBlank { "Sprotify Radio Mix" },
            album="Sprotify Radio Mix",singer="${songs.size} songs",url="",spotifyTrackId=id)
        val array=JSONArray().also { a -> songs.forEach { a.put(encode(it)) } }
        store(context).edit().putString("mix:$id",array.toString()).apply()
        remember(context,marker)
        return marker
    }
    @Synchronized fun updateArtwork(context: Context,stationId: String,path: String) {
        val current=get(context)
        if(current.none{it.spotifyTrackId==stationId})return
        val array=JSONArray().also { a -> current.forEach { a.put(encode(if(it.spotifyTrackId==stationId)it.copy(coverUri=path)else it)) } }
        store(context).edit().putString(KEY,array.toString()).apply()
    }
    fun repairAllowed(context: Context,id: String,now: Long=System.currentTimeMillis()) = now-store(context).getLong("checked:$id",0)>600_000L
    fun resolvedAnchor(context: Context, original: SongsModel): SongsModel = runCatching {
        val raw=store(context).getString("replacement:${original.spotifyTrackId}",null) ?: return@runCatching original
        decode(JSONObject(raw))
    }.getOrDefault(original)
    fun knownIsrc(context: Context, id: String)=store(context).getString("isrc:$id", "").orEmpty()
    fun saveIsrc(context: Context,id: String,isrc: String) { if(isrc.isNotBlank())store(context).edit().putString("isrc:$id",isrc).apply() }
    fun repairDue(context: Context,id: String,now: Long=System.currentTimeMillis()) = now-store(context).getLong("checked:$id",0)>86_400_000L
    fun checked(context: Context,id: String){store(context).edit().putLong("checked:$id",System.currentTimeMillis()).apply()}
    @Synchronized fun repaired(context: Context,original: SongsModel,replacement: SongsModel,oldIsrc: String,newIsrc: String): Boolean {
        if(!com.music.spotui.data.recommendation.RadioSeedRepairPolicy.sameRecording(original,replacement,oldIsrc,newIsrc))return false
        store(context).edit().putString("replacement:${original.spotifyTrackId}",encode(replacement).toString()).apply()
        saveIsrc(context,original.spotifyTrackId,newIsrc)
        return true
    }

    private const val PROGRESS = "radio_progress"
    private fun key(seedId: String) = "last_" + seedId
    fun lastTrack(context: Context, seedId: String): SongsModel? = runCatching {
        val raw = context.getSharedPreferences(PROGRESS, Context.MODE_PRIVATE).getString(key(seedId), null)
            ?: return@runCatching null
        val o = JSONObject(raw)
        SongsModel(o.optInt("id"), o.optString("title"), o.optString("album"),
            o.optString("singer"), o.optString("cover"), o.optString("url"),
            o.optString("spotify"), o.optBoolean("explicit"), o.optInt("duration"))
    }.getOrNull()
    fun saveLastTrack(context: Context, seedId: String, track: SongsModel) {
        if (seedId.isBlank()) return
        context.getSharedPreferences(PROGRESS, Context.MODE_PRIVATE).edit()
            .putString(key(seedId), encode(track).toString()).apply()
    }

    fun recommendationSeed(context: Context, stationId: String): String =
        context.getSharedPreferences(PROGRESS, 0).getString("recommendation_" + stationId, stationId) ?: stationId
    @Synchronized
    fun qualified(context: Context, stationId: String, track: SongsModel, visit: String): Boolean {
        if (track.spotifyTrackId.isBlank()) return false
        val prefs = context.getSharedPreferences(PROGRESS, 0)
        val previous = com.music.spotui.data.recommendation.EvolvingRadioPolicy.Progress(
            recommendationSeed(context, stationId), prefs.getInt("count_" + stationId, 0), prefs.getString("visit_" + stationId, "").orEmpty())
        val next = com.music.spotui.data.recommendation.EvolvingRadioPolicy.qualify(previous, track.spotifyTrackId, visit)
        if (next == previous) return false
        RadioMemory.heard(context,stationId,track)
        prefs.edit().putString("recommendation_" + stationId, next.seed).putInt("count_" + stationId, next.count)
            .putString("visit_" + stationId, next.visit).apply()
        return next.seed != previous.seed || (next.count == 0 && previous.count == 4)
    }

}
