package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.HomeItem

/** Home-only exclusions. No Spotify mutations or removal of radio seed/progress. */
object HomeHiddenItems {
    fun key(item: HomeItem): String = when (item) {
        is HomeItem.Playlist -> "playlist:" + item.id
        is HomeItem.Album -> "album:" + item.name + "|" + item.artists
        is HomeItem.Radio -> "radio:" + item.seed.spotifyTrackId.ifBlank { item.seed.id.toString() }
        is HomeItem.RecentTrack -> "track:" + item.song.id
        is HomeItem.Artist -> "artist:" + item.id
    }
    private fun prefs(context: Context) = context.getSharedPreferences("home_hidden_items", 0)
    fun hidden(context: Context, item: HomeItem) = key(item) in prefs(context).getStringSet("keys", emptySet()).orEmpty()
    @Synchronized fun hide(context: Context, item: HomeItem) {
        val key = key(item)
        val keys = prefs(context).getStringSet("keys", emptySet()).orEmpty().toMutableSet()
        val pending = prefs(context).getStringSet("replay_pending", emptySet()).orEmpty().toMutableSet()
        keys.add(key)
        pending.remove(key)
        prefs(context).edit().putStringSet("keys", keys).putStringSet("replay_pending", pending).apply()
    }
    @Synchronized fun restore(context:Context,item:HomeItem) {
        val keys=prefs(context).getStringSet("keys",emptySet()).orEmpty().toMutableSet()
        keys.remove(key(item));prefs(context).edit().putStringSet("keys",keys).apply()
    }
    /** Explicit playback requests can restore a hidden collection after it qualifies again. */
    @Synchronized fun beginReplay(context: Context, type: String, id: String) {
        val key = "$type:$id"
        if (key !in prefs(context).getStringSet("keys", emptySet()).orEmpty()) return
        val pending = prefs(context).getStringSet("replay_pending", emptySet()).orEmpty().toMutableSet()
        pending.add(key)
        prefs(context).edit().putStringSet("replay_pending", pending).apply()
    }
    @Synchronized fun qualifiedReplay(context: Context, type: String, id: String) {
        val key = "$type:$id"
        val pending = prefs(context).getStringSet("replay_pending", emptySet()).orEmpty().toMutableSet()
        if (!pending.remove(key)) return
        val keys = prefs(context).getStringSet("keys", emptySet()).orEmpty().toMutableSet()
        keys.remove(key)
        prefs(context).edit().putStringSet("keys", keys).putStringSet("replay_pending", pending).apply()
    }
}
