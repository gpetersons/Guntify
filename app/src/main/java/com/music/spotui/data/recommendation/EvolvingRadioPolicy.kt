package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.SongsModel
object EvolvingRadioPolicy {
    data class Progress(val seed: String, val count: Int = 0, val visit: String = "")
    fun qualify(previous: Progress, trackId: String, visit: String): Progress {
        if (trackId.isBlank() || visit == previous.visit) return previous
        val count = (previous.count.coerceIn(0, 4) + 1) % 5
        return Progress(if (count == 0) trackId else previous.seed, count, visit)
    }
    private const val MAX_PREVIOUS = 100
    fun playedSegment(queue: List<SongsModel>, currentId: Int, playedThroughId: Int? = currentId): List<SongsModel> {
        val index = queue.indexOfFirst { it.id == currentId }
        if (index < 0) return emptyList()
        val end = com.music.spotui.audio.QueueOrderPolicy.playedIndex(queue, currentId, playedThroughId)
        return queue.subList((index - MAX_PREVIOUS).coerceAtLeast(0), end + 1)
    }
    fun window(queue: List<SongsModel>, currentId: Int, playedThroughId: Int? = currentId): List<SongsModel> {
        val end = com.music.spotui.audio.QueueOrderPolicy.playedIndex(queue, currentId, playedThroughId)
        return (playedSegment(queue, currentId, playedThroughId) + queue.drop(end + 1).take(10)).distinctBy { it.id }
    }
    fun fill(current: SongsModel, previous: List<SongsModel>, candidates: List<SongsModel>, rotate: Boolean,
             playedThroughId: Int? = current.id, shuffled: Boolean = false): List<SongsModel> {
        val protected = playedSegment(previous, current.id, playedThroughId).ifEmpty { listOf(current) }
        val protectedIds = protected.map { it.id }.toSet()
        val end = com.music.spotui.audio.QueueOrderPolicy.playedIndex(previous, current.id, playedThroughId)
        val upcoming = previous.drop(end + 1)
        val oldIds = previous.map { it.id }.toSet()
        val fresh = candidates.filter { it.id != current.id && it.id !in oldIds }.distinctBy { it.id }
        val freshOrder = if (shuffled) fresh.shuffled() else fresh
        val next = if (rotate) freshOrder + upcoming + candidates else upcoming + freshOrder + candidates
        return protected + next.filter { it.id !in protectedIds }.distinctBy { it.id }.take(10)
    }
}
