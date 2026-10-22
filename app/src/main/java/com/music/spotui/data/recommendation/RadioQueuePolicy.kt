package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.SongsModel

/** Radio recommendations always retain the original seed; resuming only changes the first track. */
object RadioQueuePolicy {
    fun seeds(originalSeed: String?, queue: List<SongsModel>): List<String> =
        originalSeed?.takeIf { it.isNotBlank() }?.let { listOf(it) }
            ?: queue.takeLast(5).map { it.spotifyTrackId }.filter { it.isNotBlank() }.distinct()

    fun shuffled(queue: List<SongsModel>): List<SongsModel> {
        if (queue.size < 2) return queue
        val first = queue.drop(1).random()
        return listOf(first) + queue.filter { it != first }.shuffled()
    }

    private fun key(song: SongsModel) = song.spotifyTrackId.ifBlank { "local:${song.id}" }
    fun resumed(seed: SongsModel?, last: SongsModel?, suggestions: List<SongsModel>, rotation: Int = 0): List<SongsModel> {
        val first = last ?: seed
        val candidates = suggestions.filter { key(it) != first?.let(::key) && key(it) != seed?.let(::key) }.distinctBy(::key)
        val offset = if (candidates.isEmpty()) 0 else Math.floorMod(rotation, candidates.size)
        return (listOfNotNull(first) + candidates.drop(offset) + candidates.take(offset)).distinctBy(::key).take(11)
    }
    /** Prefer unused Spotify candidates; a refresh never replaces/restarts the current track. */
    fun refreshed(current: SongsModel, previous: List<SongsModel>, suggestions: List<SongsModel>,
                  playedThroughId: Int? = current.id, shuffled: Boolean = false): List<SongsModel> {
        val protected = EvolvingRadioPolicy.playedSegment(previous, current.id, playedThroughId).ifEmpty { listOf(current) }
        val protectedKeys = protected.map(::key).toSet()
        val candidates = suggestions.filter { key(it) !in protectedKeys }.distinctBy(::key)
        if (candidates.isEmpty()) return emptyList()
        val oldKeys = previous.map(::key).toSet()
        var next = (candidates.filter { key(it) !in oldKeys } + candidates.filter { key(it) in oldKeys }).take(10)
        if (shuffled) next = next.shuffled()
        val end = com.music.spotui.audio.QueueOrderPolicy.playedIndex(previous, current.id, playedThroughId)
        val oldNext = previous.drop(end + 1).take(10)
        if (shuffled && next.size > 1 && next.map(::key) == oldNext.map(::key)) next = next.drop(1) + next.first()
        return protected + next
    }

}
