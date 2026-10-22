package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.LibraryEntry

/** Paginated/cached library responses can overlap. Lazy layouts require unique keys. */
object LibraryIdentityPolicy {
    fun key(entry: LibraryEntry): String =
        "library:${if (entry.isPlaylist) "playlist" else "album"}:${entry.spotifyId}"

    fun unique(entries: List<LibraryEntry>): List<LibraryEntry> =
        entries.filter { it.spotifyId.isNotBlank() }.distinctBy(::key)
}
