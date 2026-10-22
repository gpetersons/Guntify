package com.music.spotui.audio

object CollectionPlaybackPolicy {
    fun active(expectedKey: String, actualKey: String?, currentId: Int, queuedIds: List<Int>): Boolean =
        expectedKey == actualKey && currentId in queuedIds
}
