package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.ArtistTrackUi
import com.music.spotui.data.entity.FollowingRelease
import com.music.spotui.data.entity.SongsModel
import java.time.LocalDate

object FollowingPolicy {
    fun popular(tracks: List<ArtistTrackUi>): List<SongsModel> = tracks
        .sortedByDescending { it.playcount ?: -1L }
        .distinctBy { it.song.spotifyTrackId.ifBlank { it.song.id.toString() } }
        .take(50).map { it.song }

    fun newest(releases: List<FollowingRelease>): List<FollowingRelease> = releases
        .sortedByDescending { it.date }.distinctBy { it.id }.take(30)

    // Year-only dates cannot establish that a release is new this quarter.
    fun isRecent(date: String, today: LocalDate = LocalDate.now()): Boolean {
        val day = runCatching { LocalDate.parse(date.take(10)) }.getOrNull() ?: return false
        return !day.isAfter(today) && !day.isBefore(today.minusDays(90))
    }
}
