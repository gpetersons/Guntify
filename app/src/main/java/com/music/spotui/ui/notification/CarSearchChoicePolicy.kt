package com.music.spotui.ui.notification

/**
 * Android Auto search selects playable rows by sending Play.
 * A browsable search result provides an explicit choice instead:
 * Play now, or Add to queue without replacing the playing song.
 */
object CarSearchChoicePolicy {
    private const val BROWSE = "search-choice/"
    private const val PLAY = "search-play/"
    private const val ADD = "search-add/"

    fun folderId(trackId: String) = BROWSE + trackId
    fun playId(trackId: String) = PLAY + trackId
    fun addId(trackId: String) = ADD + trackId

    fun folderTarget(id: String): String? =
        id.takeIf { it.startsWith(BROWSE) }?.removePrefix(BROWSE)?.takeIf { it.isNotBlank() }

    fun playTarget(id: String): String? =
        id.takeIf { it.startsWith(PLAY) }?.removePrefix(PLAY)?.takeIf { it.isNotBlank() }

    fun addTarget(id: String): String? =
        id.takeIf { it.startsWith(ADD) }?.removePrefix(ADD)?.takeIf { it.isNotBlank() }
}
