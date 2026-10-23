package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.LibraryEntry
import kotlinx.coroutines.flow.MutableStateFlow

/** Local organization only; pinning never follows, unfollows or mutates Spotify. */
object LibraryPins {
    val revision = MutableStateFlow(0L)
    private fun prefs(c: Context) = c.getSharedPreferences("library_organization",0)
    fun key(entry: LibraryEntry) = "${if(entry.isPlaylist)"playlist"else"album"}:${entry.spotifyId}"
    fun contains(c: Context,key: String) = key in prefs(c).getStringSet("pins",emptySet()).orEmpty()
    @Synchronized fun toggle(c: Context,key: String) {
        val set=prefs(c).getStringSet("pins",emptySet()).orEmpty().toMutableSet()
        if(!set.remove(key))set.add(key)
        prefs(c).edit().putStringSet("pins",set).apply();revision.value++
    }
    fun sort(c:Context)=prefs(c).getString("sort","Recent") ?: "Recent"
    fun setSort(c:Context,value:String){prefs(c).edit().putString("sort",value).apply();revision.value++}
    fun arrange(c:Context,items:List<LibraryEntry>,sort:String):List<LibraryEntry> {
        val ordered=when(sort){"Name"->items.sortedBy{it.name.lowercase()};"Creator"->items.sortedBy{it.artists.ifBlank{it.subtitle}.lowercase()};else->items}
        return ordered.sortedByDescending { contains(c,key(it)) }
    }
}
