package com.music.spotui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.music.spotui.di.SongPlayer

/** Fades with the playback source; retains a restrained outline while paused. */
@Composable
fun Modifier.playbackGlow(active: Boolean, shape: Shape = RoundedCornerShape(14.dp)): Modifier {
    val alpha by animateFloatAsState(if (active) 1f else 0f, tween(350), label = "Playback glow")
    val green = Color(0xFF1ED760)
    return this.shadow((10f * alpha).dp, shape, clip = false,
        ambientColor = green.copy(alpha = 0.35f * alpha), spotColor = green.copy(alpha = 0.5f * alpha))
        .border(1.dp, green.copy(alpha = 0.45f * alpha), shape)
}

/** No animation jobs while paused/buffering or while this symbol is off screen. */
@Composable
fun PlayingBars(playing: Boolean) {
    var audible by remember { mutableStateOf(false) }
    LaunchedEffect(playing) {
        audible = playing && SongPlayer.isPlaying()
        while (playing) {
            audible = SongPlayer.isPlaying()
            kotlinx.coroutines.delay(250L)
        }
    }
    Row(Modifier.height(20.dp).semantics { stateDescription = if (audible) "Playing" else "Playback paused or buffering" },
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            val height = if (audible) {
                val transition = rememberInfiniteTransition(label = "Playing bar $index")
                val value by transition.animateFloat(4f + index * 3f, 18f - index * 2f,
                    infiniteRepeatable(tween(430 + index * 170, easing = LinearEasing), RepeatMode.Reverse), label = "Bar height")
                value
            } else 4f
            Box(Modifier.width(3.dp).height(height.dp).background(Color(0xFF1ED760), RoundedCornerShape(1.dp)))
        }
    }
}

@Composable
fun PlaybackBadge(playing: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xE6202020), RoundedCornerShape(8.dp)).padding(6.dp), contentAlignment = Alignment.Center) {
        PlayingBars(playing)
    }
}
