package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SongsModel
import java.util.Locale
import kotlin.math.abs

/** Each original song gets its own recommendation lane, with deduplication across lanes. */
object RadioMixPolicy {
    fun key(song: SongsModel) = song.spotifyTrackId.ifBlank { "local:${song.id}" }
    fun selected(songs: List<SongsModel>) = songs.filter { it.spotifyTrackId.isNotBlank() }.distinctBy(::key).take(5)
    fun blend(seeds: List<String>, batches: List<List<SongsModel>>, originalCount: Int, mode: String): List<SongsModel> {
        val pointers=IntArray(batches.size);val seen=seeds.toMutableSet();val out=mutableListOf<SongsModel>()
        val originals=originalCount.coerceIn(0,batches.size)
        fun emitOne(i:Int) {
            val batch=batches[i]
            while(pointers[i]<batch.size) {
                val song=batch[pointers[i]++]
                if(seen.add(key(song))){out+=song;return}
            }
        }
        // Stay close never consumes an evolving lane. Balanced stops when the
        // originals run out instead of draining a broad evolving-only tail.
        while((if(mode=="explore")batches.indices else 0 until originals).any { pointers[it] < batches[it].size }) {
            repeat(if(mode=="balanced" || mode !in setOf("close","explore"))3 else 1){for(i in 0 until originals)emitOne(i)}
            if(mode!="close")repeat(if(mode=="explore")3 else 1){for(i in originals until batches.size)emitOne(i)}
        }
        return out
    }
}

/** Non-Radio callers retain their previous fallback behavior. */
object RadioDiscoveryPolicy {
    fun evolving(mode:String)=mode!="close"
    fun artistFallback(mode:String?)=mode!="close"
    fun profileFallback(mode:String?)=mode==null || mode=="explore"
}

/** Never repair by title alone: editions, artists, explicitness and recording evidence must agree. */
object RadioSeedRepairPolicy {
    private fun norm(s:String)=s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()
    private fun artists(s:String)=s.split(Regex(",| feat\\. | ft\\. ",RegexOption.IGNORE_CASE)).map(::norm).filter(String::isNotBlank).toSet()
    fun sameRecording(old:SongsModel, replacement:SongsModel, oldIsrc:String="", newIsrc:String=""):Boolean {
        if(old.spotifyTrackId.isBlank() || replacement.spotifyTrackId.isBlank() || norm(old.title).isBlank())return false
        if(norm(old.title)!=norm(replacement.title) || artists(old.singer).isEmpty() || artists(old.singer)!=artists(replacement.singer) || old.explicit!=replacement.explicit)return false
        val knownLengths=old.durationMs>0 && replacement.durationMs>0
        if(knownLengths && abs(old.durationMs.toLong()-replacement.durationMs)>2_000L)return false
        if(oldIsrc.isNotBlank())return newIsrc.isNotBlank() && oldIsrc.equals(newIsrc,true)
        return knownLengths && norm(old.album).isNotBlank() && norm(old.album)==norm(replacement.album)
    }
}
