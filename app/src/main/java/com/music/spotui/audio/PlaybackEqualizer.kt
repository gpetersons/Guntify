package com.music.spotui.audio

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Per-player audio effects, including the incoming crossfade player. Never session zero. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object PlaybackEqualizer {
    val presets = listOf("Normal", "Bass Boost", "Rock", "Pop", "Classical", "Vocal")
    private val curves = listOf(
        intArrayOf(0, 0, 0, 0, 0), intArrayOf(600, 400, 100, 0, 0),
        intArrayOf(400, 200, -100, 200, 400), intArrayOf(-100, 200, 400, 200, -100),
        intArrayOf(300, 100, -100, 100, 300), intArrayOf(-200, 0, 400, 300, 0)
    )
    private val effects = mutableMapOf<ExoPlayer, Equalizer>()
    private val listeners = mutableMapOf<ExoPlayer, Player.Listener>()
    var enabled by mutableStateOf(false)
        private set
    var preset by mutableStateOf("Normal")
        private set
    var message by mutableStateOf("Start playback to use the equalizer.")
        private set
    val customGains = androidx.compose.runtime.mutableStateListOf(0, 0, 0, 0, 0)
    fun adjustBand(index: Int, milliBel: Int) {
        customGains[index] = milliBel.coerceIn(-1200, 1200)
        preset = "Custom"
    }
    private var initialized = false
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.getSharedPreferences("playback_equalizer", 0)
        enabled = prefs.getBoolean("enabled", false)
        preset = prefs.getString("preset", "Normal").orEmpty().takeIf { it in presets || it == "Custom" } ?: "Normal"
        for (i in 0..4) customGains[i] = prefs.getInt("gain_$i", 0).coerceIn(-1200, 1200)
        if (preset != "Custom") curves[presets.indexOf(preset)].forEachIndexed { i, value -> customGains[i] = value }
    }
    fun reload(context: Context) {
        initialized = false
        initialize(context)
        configure(context, enabled, preset)
    }
    fun configure(context: Context, on: Boolean, selected: String = preset) {
        initialize(context)
        enabled = on
        message = if (on) "Start playback to use the equalizer." else "Equalizer off."
        preset = selected.takeIf { it in presets || it == "Custom" } ?: "Normal"
        if (preset != "Custom") curves[presets.indexOf(preset)].forEachIndexed { i, value -> customGains[i] = value }
        context.getSharedPreferences("playback_equalizer", 0).edit()
            .putBoolean("enabled", on).putString("preset", preset).also { editor ->
                customGains.forEachIndexed { i, gain -> editor.putInt("gain_$i", gain) }
            }.apply()
        listeners.keys.toList().forEach { attachSession(it, it.audioSessionId) }
    }
    fun observe(context: Context, player: ExoPlayer) {
        initialize(context)
        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) { attachSession(player, audioSessionId) }
        }
        listeners[player] = listener
        player.addListener(listener)
        attachSession(player, player.audioSessionId)
    }
    private fun attachSession(player: ExoPlayer, session: Int) {
        effects.remove(player)?.let { runCatching { it.release() } }
        if (!enabled || session <= 0) return
        var effect: Equalizer? = null
        runCatching {
            val eq = Equalizer(0, session)
            effect = eq
            val range = eq.bandLevelRange
            val curve = if (preset == "Custom") customGains.toIntArray() else curves[presets.indexOf(preset)]
            val anchors = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)
            for (i in 0 until eq.numberOfBands.toInt()) {
                val hz = eq.getCenterFreq(i.toShort()) / 1000.0
                val upper = anchors.indexOfFirst { it >= hz }
                val gain = when {
                    upper == 0 -> curve.first().toDouble()
                    upper < 0 -> curve.last().toDouble()
                    else -> {
                        val mix = (kotlin.math.ln(hz) - kotlin.math.ln(anchors[upper - 1])) /
                            (kotlin.math.ln(anchors[upper]) - kotlin.math.ln(anchors[upper - 1]))
                        curve[upper - 1] + mix * (curve[upper] - curve[upper - 1])
                    }
                }
                eq.setBandLevel(i.toShort(), gain.toInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
            eq.enabled = true
            effects[player] = eq
            message = "Applied to music playback."
        }.onFailure {
            effect?.let { eq -> runCatching { eq.release() } }
            message = "Equalizer unavailable for this audio output."
        }
    }
    fun release(player: ExoPlayer?) {
        if (player == null) return
        listeners.remove(player)?.let { player.removeListener(it) }
        effects.remove(player)?.let { runCatching { it.release() } }
    }
}
