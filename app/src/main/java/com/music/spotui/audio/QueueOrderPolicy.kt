package com.music.spotui.audio

import com.music.spotui.data.entity.SongsModel

/** Already-played queue positions stay stable, including while browsing backward. */
object QueueOrderPolicy {
    fun playedIndex(queue: List<SongsModel>, currentId: Int, playedThroughId: Int?): Int =
        maxOf(queue.indexOfFirst { it.id == currentId }, queue.indexOfFirst { it.id == playedThroughId })

    fun shuffled(queue: List<SongsModel>, currentId: Int, playedThroughId: Int?): List<SongsModel> {
        val end = playedIndex(queue, currentId, playedThroughId)
        return queue.take(end + 1) + queue.drop(end + 1).shuffled()
    }

    fun unshuffled(queue: List<SongsModel>, currentId: Int, playedThroughId: Int?, original: List<SongsModel>): List<SongsModel> {
        val prefix = queue.take(playedIndex(queue, currentId, playedThroughId) + 1)
        val prefixIds = prefix.map { it.id }.toSet()
        val remaining = queue.filter { it.id !in prefixIds }
        val remainingIds = remaining.map { it.id }.toSet()
        return prefix + (original.filter { it.id in remainingIds } + remaining).distinctBy { it.id }
    }

    fun previousIndex(currentIndex: Int): Int = (currentIndex - 1).coerceAtLeast(0)
}
