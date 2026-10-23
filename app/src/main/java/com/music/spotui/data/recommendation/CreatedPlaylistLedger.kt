package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.LibraryEntry
import org.json.JSONArray
import org.json.JSONObject

data class CreatedPlaylistRecord(val owner: String, val id: String, val name: String, val cover: String = "", val pending: Boolean = false)

/** Durable bookmarks are independent of a replaceable remote-library snapshot. */
object CreatedPlaylistLedger {
    fun encode(records: List<CreatedPlaylistRecord>): String = JSONArray().apply { records.forEach { r -> put(JSONObject()
        .put("owner",r.owner).put("id",r.id).put("name",r.name).put("cover",r.cover).put("pending",r.pending)) } }.toString()
    fun decode(raw: String): List<CreatedPlaylistRecord> {
        val array=JSONArray(raw)
        return (0 until array.length()).map { i -> array.getJSONObject(i).let { o -> CreatedPlaylistRecord(o.getString("owner"),o.getString("id"),o.getString("name"),o.optString("cover"),o.optBoolean("pending")) } }
            .filter { it.owner.isNotBlank() && Regex("[A-Za-z0-9]+").matches(it.id) }.distinctBy { it.owner to it.id }
    }
    fun merge(owner: String, records: List<CreatedPlaylistRecord>, remote: List<LibraryEntry>): List<LibraryEntry> {
        val keys=remote.filter { it.isPlaylist }.map { it.spotifyId }.toSet()
        return LibraryIdentityPolicy.unique(remote + records.filter { it.owner==owner && it.id !in keys }.map {
            LibraryEntry(it.id,it.name,"Playlist • Saved in Sprotify",it.cover,true)
        })
    }
    fun reconcile(owner: String, records: List<CreatedPlaylistRecord>, remote: List<LibraryEntry>): List<CreatedPlaylistRecord> = records.map { record ->
        if(record.owner!=owner) record else {
            val confirmed=remote.firstOrNull { it.isPlaylist && it.spotifyId==record.id }
            if(confirmed==null)record else record.copy(name=confirmed.name,cover=confirmed.coverUri.ifBlank{record.cover},pending=false)
        }
    }
}
