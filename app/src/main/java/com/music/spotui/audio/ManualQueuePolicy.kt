package com.music.spotui.audio

import com.music.spotui.data.entity.SongsModel

/** Explicit additions form an ordered block immediately after the current track. */
object ManualQueuePolicy {
    fun add(queue: List<SongsModel>, currentId: Int, pending: Set<Int>, song: SongsModel): List<SongsModel>? {
        val current = queue.indexOfFirst { it.id == currentId }
        val existing = queue.indexOfFirst { it.id == song.id }
        if (existing >= 0 && (existing <= current || song.id in pending)) return null
        val next = queue.filterNot { it.id == song.id }.toMutableList()
        val start = (next.indexOfFirst { it.id == currentId } + 1).coerceAtLeast(0)
        val block = next.drop(start).takeWhile { it.id in pending }.size
        next.add(start + block, song)
        return next
    }
    fun protect(queue: List<SongsModel>, currentId: Int, pending: List<SongsModel>): List<SongsModel> {
        val current = queue.indexOfFirst { it.id == currentId }
        if (current < 0 || pending.isEmpty()) return queue
        val prefix = queue.take(current + 1)
        val ids = prefix.map { it.id }.toSet()
        return (prefix + pending.filterNot { it.id in ids } + queue.drop(current + 1)).distinctBy { it.id }
    }
}
