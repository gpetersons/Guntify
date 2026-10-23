package com.music.spotui.audio

import android.content.Context
import com.music.spotui.data.entity.PlaybackCollection
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.HomeHiddenItems
import com.music.spotui.data.preferences.RadioSessions
import com.music.spotui.di.CurrentSongState
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.repository.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Shared Radio start path for phone menus and media-session (Android Auto)
 * commands. State changes happen on the calling main thread. Only the Spotify
 * recommendation lookup happens off-thread, under the caller's lifecycle.
 */
class RadioPlaybackController @Inject constructor(
    private val state: CurrentSongState,
    private val repository: AppRepository,
) {
    fun startRadioFromSong(song: SongsModel, context: Context, scope: CoroutineScope): Boolean {
        if (song.url.isBlank()) return false
        val app = context.applicationContext
        val keepStream = state.songId.value == song.id &&
            state.queue.value.any { it.id == song.id } &&
            (SongPlayer.isPrepared() || SongPlayer.isPlaying() ||
                (SongPlayer.webPlaybackActive() && SongPlayer.getDuration() > 0L))
        val wasPlaying = state.playingState.value
        val stationId = song.spotifyTrackId.ifBlank { song.id.toString() }

        RadioSessions.remember(app, song)
        state.updateQueue(listOf(song))
        state.activateRadio(stationId)
        HomeHiddenItems.beginReplay(app, "radio", stationId)
        state.collection.value = PlaybackCollection(
            "radio:$stationId", "radio", stationId,
            RadioSessions.stationName(song), song.coverUri, song.singer
        )
        state.updateSongState(
            song.coverUri, song.title, song.singer,
            if (keepStream) wasPlaying else true, song.id, 0, song.album
        )
        if (!keepStream) SongPlayer.playSong(song.url, app)

        // A non-Spotify local song can still form a saved station, but only a
        // catalog ID can be used to ask Spotify for future recommendations.
        val seed = song.spotifyTrackId.takeIf { it.isNotBlank() } ?: return true
        val generation = state.generation
        scope.launch(Dispatchers.IO) {
            val recommendations = try {
                repository.provideRadioRecommendations(
                    seed, RadioSessions.recommendationSeed(app, seed)
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                // Never overwrite a different radio, or user-added queue entries
                // while the asynchronous recommendation request was in flight.
                val live = state.queue.value
                if (generation != state.generation ||
                    state.activeRadioSeed != stationId ||
                    state.songId.value != song.id ||
                    live.size != 1 || live.single().id != song.id
                ) return@withContext
                val fresh = recommendations.filter { it.id != song.id }
                    .distinctBy { it.id }.take(10)
                if (fresh.isNotEmpty()) {
                    state.updateQueue(live + fresh, preserveSession = true)
                    fresh.firstOrNull()?.let { SongPlayer.prefetch(it.url, app) }
                }
            }
        }
        return true
    }
}
