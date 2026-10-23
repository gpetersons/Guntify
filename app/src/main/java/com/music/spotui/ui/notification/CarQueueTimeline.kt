package com.music.spotui.ui.notification

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import com.music.spotui.data.entity.SongsModel

/**
 * Read-only queue projection for Android Auto's *built-in* media queue.
 *
 * Sprotify streams one resolved URL at a time, but the phone's CurrentSongState
 * retains the complete ordered list. This timeline is only exposed through the
 * MediaSession's ForwardingPlayer; it is NEVER passed to ExoPlayer as a playlist.
 */
@OptIn(UnstableApi::class)
class CarQueueTimeline(
    val songs: List<SongsModel>,
    private val source: String,
) : Timeline() {
    private val windows: List<MediaItem> = songs.map { song ->
        MediaItem.Builder()
            .setMediaId("car-queue/${song.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setDisplayTitle(song.title)
                    .setArtist(song.singer)
                    .setSubtitle(song.singer)
                    .setAlbumTitle(source.ifBlank { song.album })
                    .apply {
                        if (song.coverUri.isNotBlank()) {
                            setArtworkUri(android.net.Uri.parse(song.coverUri))
                        }
                    }
                    .build()
            )
            .build()
    }

    fun itemAt(index: Int): MediaItem = windows[index]
    fun currentIndex(songId: Int): Int =
        songs.indexOfFirst { it.id == songId }.let { if (it == -1) 0 else it }

    override fun getWindowCount(): Int = songs.size
    override fun getPeriodCount(): Int = songs.size

    override fun getWindow(
        windowIndex: Int,
        window: Window,
        defaultPositionProjectionUs: Long,
    ): Window {
        val song = songs[windowIndex]
        val durationUs = if (song.durationMs > 0) song.durationMs.toLong() * 1000L else C.TIME_UNSET
        return window.set(
            "car-window:${song.id}",
            windows[windowIndex],
            null,
            C.TIME_UNSET,
            C.TIME_UNSET,
            C.TIME_UNSET,
            true,
            false,
            null,
            0L,
            durationUs,
            windowIndex,
            windowIndex,
            0L,
        )
    }

    override fun getPeriod(index: Int, period: Period, setIds: Boolean): Period {
        val song = songs[index]
        val durationUs = if (song.durationMs > 0) song.durationMs.toLong() * 1000L else C.TIME_UNSET
        return period.set(
            if (setIds) "car-period:${song.id}" else null,
            if (setIds) "car-period:${song.id}" else null,
            index,
            durationUs,
            0L,
        )
    }

    override fun getIndexOfPeriod(uid: Any): Int =
        songs.indexOfFirst { "car-period:${it.id}" == uid }

    override fun getUidOfPeriod(periodIndex: Int): Any =
        "car-period:${songs[periodIndex].id}"
}
