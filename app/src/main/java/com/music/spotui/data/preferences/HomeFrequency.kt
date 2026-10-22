package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.HomeItem
import org.json.JSONArray
import org.json.JSONObject

/** The first two Spotify home shortcuts seed the ranking once. Later local plays
 * accumulate and can displace them. No individual tracks are stored. */
object HomeFrequency {
    private const val STORE = "spotui_home_frequency_v1"
    private fun prefs(c: Context) = c.getSharedPreferences(STORE, Context.MODE_PRIVATE)
    // These queues have no catalog playlist page to reopen. Keep their song listens, not dead shortcuts.
    private fun isLocalMix(id: String) = id.startsWith("stats:") || id == "following:popular"
    private fun key(type: String, id: String) = "$type:$id"
    private fun records(c: Context): JSONArray = runCatching {
        JSONArray(prefs(c).getString("entries", "[]"))
    }.getOrDefault(JSONArray())
    private fun save(c: Context, list: JSONArray) = prefs(c).edit().putString("entries", list.toString()).apply()
    private fun encode(type: String, id: String, name: String, image: String, subtitle: String, score: Int, time: Long) =
        JSONObject().put("type",type).put("id",id).put("name",name).put("image",image)
            .put("subtitle",subtitle).put("score",score).put("time",time)
    @Synchronized
    fun seed(c: Context, items: List<HomeItem>) {
        if (prefs(c).getBoolean("seeded",false) || items.isEmpty()) return
        val list = records(c)
        val known = (0 until list.length()).map { index ->
            val entry = list.getJSONObject(index)
            key(entry.optString("type"), entry.optString("id"))
        }.toSet()
        items.filter { it is HomeItem.Playlist || it is HomeItem.Album }.take(2).forEachIndexed { i, item ->
            when(item) {
                is HomeItem.Playlist -> if (key("playlist", item.id) !in known) list.put(encode("playlist",item.id,item.name,item.imageUrl,item.subtitle,2,-i.toLong()))
                is HomeItem.Album -> if (key("album", item.name + "|" + item.artists) !in known) list.put(encode("album",item.name + "|" + item.artists,item.name,item.imageUrl,item.artists,2,-i.toLong()))
                else -> Unit
            }
        }
        if (list.length() > 0) { save(c,list); prefs(c).edit().putBoolean("seeded",true).apply() }
    }
    // Playback-start hooks no longer count as listening. The audio tracker calls record after 30s.
    fun played(c: Context, type: String, id: String, name: String, image: String, subtitle: String = "") = Unit
    @Synchronized
    fun record(c: Context, type: String, id: String, name: String, image: String, subtitle: String = "") {
        if (id.isBlank() || type !in setOf("playlist", "album", "radio") || (type == "playlist" && isLocalMix(id))) return
        val old = records(c)
        val out = JSONArray()
        var count = 0
        for (i in 0 until old.length()) {
            val entry = old.getJSONObject(i)
            if (key(entry.optString("type"),entry.optString("id")) == key(type,id)) count = entry.optInt("score")
            else out.put(entry)
        }
        out.put(encode(type,id,name,image,subtitle,count+1,System.currentTimeMillis()))
        save(c,out)
        HomeHiddenItems.qualifiedReplay(c, type, id)
    }
    fun top(c: Context, radios: List<HomeItem.Radio>, showPlaylists: Boolean, showRadios: Boolean): List<HomeItem> {
        val result = mutableListOf<Pair<HomeItem,Pair<Int,Long>>>()
        val arr = records(c)
        for(i in 0 until arr.length()) {
            val o=arr.getJSONObject(i)
            val item: HomeItem = when(o.optString("type")) {
                "playlist" -> if(showPlaylists && !isLocalMix(o.optString("id"))) HomeItem.Playlist(o.optString("name"),o.optString("image"),o.optString("subtitle"),o.optString("id")) else continue
                "album" -> HomeItem.Album(o.optString("name"),o.optString("image"),o.optString("subtitle"),o.optString("id").substringAfter('|',""))
                "radio" -> radios.firstOrNull { (it.seed.spotifyTrackId.ifBlank { it.seed.id.toString() }) == o.optString("id") } ?: continue
                else -> continue
            }
            result.add(item to (o.optInt("score") to o.optLong("time")))
        }
        // Seed-only radios can appear in recents but not displace frequently played tiles.
        return result.sortedWith(compareByDescending<Pair<HomeItem,Pair<Int,Long>>> { it.second.first }
            .thenByDescending { it.second.second }).map { it.first }.take(2)
    }
    /** Actual collection play activity, newest first. Spotify baseline seeds have negative timestamps. */
    fun recentlyPlayed(c: Context, radios: List<HomeItem.Radio>, showPlaylists: Boolean, showRadios: Boolean, limit: Int): List<HomeItem> {
        return recentActivity(c, radios, showPlaylists, showRadios).map { it.first }.take(limit)
    }
    /** Keep timestamps so Home can interleave collections and qualified song listens correctly. */
    fun recentActivity(c: Context, radios: List<HomeItem.Radio>, showPlaylists: Boolean, showRadios: Boolean): List<Pair<HomeItem, Long>> {
        val entries = records(c)
        val result = mutableListOf<Pair<HomeItem, Long>>()
        for (i in 0 until entries.length()) {
            val o = entries.optJSONObject(i) ?: continue
            val playedAt = o.optLong("time")
            if (playedAt <= 0L) continue
            val item: HomeItem = when (o.optString("type")) {
                "playlist" -> if (showPlaylists && !isLocalMix(o.optString("id"))) HomeItem.Playlist(o.optString("name"), o.optString("image"), "Playlist", o.optString("id")) else continue
                "album" -> HomeItem.Album(o.optString("name"), o.optString("image"), o.optString("subtitle"), o.optString("id").substringAfter('|', ""))
                "radio" -> if (showRadios) radios.firstOrNull { it.seed.spotifyTrackId.ifBlank { it.seed.id.toString() } == o.optString("id") } ?: continue else continue
                else -> continue
            }
            result.add(item to playedAt)
        }
        return result.sortedByDescending { it.second }
    }
    fun clear(c: Context) { prefs(c).edit().clear().apply() }
}
