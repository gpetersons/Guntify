package com.music.spotui.data.preferences
import android.content.Context
import com.music.spotui.data.entity.SongsModel
import java.util.Locale

object RadioTaste {
    val revision = kotlinx.coroutines.flow.MutableStateFlow(0L)
    private fun prefs(c: Context) = c.getSharedPreferences("radio_taste", 0)
    fun mode(c: Context, station: String) = prefs(c).getString("mode:$station", "balanced") ?: "balanced"
    fun setMode(c: Context, station: String, value: String) { prefs(c).edit().putString("mode:$station", value).apply(); revision.value++ }
    fun blocked(c: Context): Set<String> = prefs(c).getStringSet("artists", emptySet()).orEmpty()
    fun disliked(c: Context): Set<String> = prefs(c).getStringSet("songs", emptySet()).orEmpty()
    fun dislike(c: Context, song: SongsModel) {
        prefs(c).edit().putStringSet("songs", disliked(c) + song.spotifyTrackId.ifBlank { "local:${song.id}" }).apply(); revision.value++
    }
    fun block(c: Context, song: SongsModel) {
        val artist = song.singer.substringBefore(',').substringBefore(" feat.").trim().lowercase(Locale.ROOT)
        if (artist.isNotBlank()) { prefs(c).edit().putStringSet("artists", blocked(c) + artist).apply(); revision.value++ }
    }
    fun allowed(c: Context, song: SongsModel) = !RadioSkipMemory.avoids(c,song) && com.music.spotui.data.recommendation.RadioTastePolicy.allowed(song, disliked(c), blocked(c))
    fun reset(c: Context) { prefs(c).edit().clear().apply(); revision.value++ }
}
