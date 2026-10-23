package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.api.SpotifySession
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.recommendation.*
import java.util.UUID

/** Account-scoped metadata only. Never stores or retries the playlist-creation request. */
object CreatedPlaylists {
    private fun prefs(c:Context)=c.getSharedPreferences("created_playlists",Context.MODE_PRIVATE)
    @Synchronized fun session(c:Context):String {
        val p=c.getSharedPreferences("spotify_session",Context.MODE_PRIVATE)
        return p.getString("created_scope",null) ?: UUID.randomUUID().toString().also { check(p.edit().putString("created_scope",it).commit()) }
    }
    @Synchronized fun newSession(c:Context) {
        check(c.getSharedPreferences("spotify_session",Context.MODE_PRIVATE).edit().putString("created_scope",UUID.randomUUID().toString()).commit())
    }
    private fun owner(c:Context,s:String=session(c))=prefs(c).getString("binding:$s",null) ?: "session:$s"
    private fun records(c:Context)=CreatedPlaylistLedger.decode(prefs(c).getString("records","[]") ?: "[]")
    private fun save(c:Context,r:List<CreatedPlaylistRecord>) { check(prefs(c).edit().putString("records",CreatedPlaylistLedger.encode(r)).commit()) { "Could not save this playlist on your phone" } }
    @Synchronized fun bind(c:Context,id:String) {
        if(id.isBlank())return
        val s=session(c);val previous=owner(c,s);val next="account:$id"
        val changed=records(c).map { if(it.owner==previous)it.copy(owner=next)else it }.distinctBy { it.owner to it.id }
        check(prefs(c).edit().putString("binding:$s",next).putString("records",CreatedPlaylistLedger.encode(changed)).commit())
    }
    @Synchronized fun record(c:Context,session:String,id:String,name:String,cover:String="",pending:Boolean=false) {
        val account=owner(c,session)
        val rows=records(c).filterNot { it.owner==account && it.id==id }
        save(c,rows+CreatedPlaylistRecord(account,id,name,cover,pending))
    }
    @Synchronized fun find(c:Context,id:String)=records(c).firstOrNull { it.owner==owner(c) && it.id==id }
    @Synchronized fun markSaved(c:Context,id:String) { val account=owner(c);save(c,records(c).map{if(it.owner==account&&it.id==id)it.copy(pending=false)else it}) }
    @Synchronized fun remove(c:Context,id:String) { val account=owner(c);save(c,records(c).filterNot{it.owner==account&&it.id==id}) }
    @Synchronized fun merge(c:Context,remote:List<LibraryEntry>,authoritative:Boolean=false):List<LibraryEntry> {
        val account=owner(c);var rows=records(c)
        if(authoritative){rows=CreatedPlaylistLedger.reconcile(account,rows,remote);save(c,rows)}
        return CreatedPlaylistLedger.merge(account,rows,remote)
    }
    private val recoveredScopes = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    /** Recover old app-created cover IDs by reading the existing playlist; never create replacements. */
    suspend fun recoverLegacy(c:Context):Boolean {
        val cookie=SpotifySession.spDc(c);val scope=session(c)
        if(cookie.isBlank())return false
        val candidates=c.getSharedPreferences("playlist_covers",0).all.keys.filter { Regex("[A-Za-z0-9]{22}").matches(it) }
        if(candidates.isEmpty())return false
        if(!recoveredScopes.add(scope))return false
        val me=kotlinx.coroutines.withTimeoutOrNull(5_000L){com.metrolist.spotify.Spotify.me().getOrNull()} ?: return false
        if(cookie!=SpotifySession.spDc(c))return false
        bind(c,me.id)
        var changed=false
        for(id in candidates.filter { find(c,it)==null }.take(10)) {
            val playlist=kotlinx.coroutines.withTimeoutOrNull(4_000L){com.metrolist.spotify.Spotify.playlist(id).getOrNull()} ?: continue
            if(cookie!=SpotifySession.spDc(c))break
            if(playlist.owner?.id==me.id) { record(c,scope,id,playlist.name,com.music.spotui.data.api.PlaylistCover.localPath(c,id).orEmpty(),true);changed=true }
        }
        return changed
    }
}
