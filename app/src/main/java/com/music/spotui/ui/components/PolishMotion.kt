package com.music.spotui.ui.components

import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

/** Small, compositor-only movements; Compose respects the device animator duration scale. */
fun Modifier.menuReveal(): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(180, easing = FastOutSlowInEasing), label = "Menu reveal"
    )
    val distance = with(LocalDensity.current) { 6f * density }
    graphicsLayer { alpha = progress; translationY = distance * (1f - progress) }
}

/** Tactile feedback without a ripple or a grey pressed background. */
fun Modifier.motionClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.97f else 1f,
        tween(if (pressed) 90 else 180, easing = FastOutSlowInEasing), label = "Press response"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** Finish the Material sheet exit before its owner removes it from composition. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun rememberSheetDismiss(state: androidx.compose.material3.SheetState, onDismiss: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentDismiss by rememberUpdatedState(onDismiss)
    var closing by remember { mutableStateOf(false) }
    return {
        if (!closing) {
            closing = true
            scope.launch {
                try {
                    state.hide()
                    if (!state.isVisible) currentDismiss()
                } finally { closing = false }
            }
        }
    }
}

/** Consistent transitions for in-page destinations. No animated measurement of long lists. */
@Composable
fun <T> PageMotion(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val distance = with(LocalDensity.current) { (12f * density).toInt() }
    androidx.compose.animation.AnimatedContent(
        targetState = target, modifier = modifier,
        transitionSpec = {
            (androidx.compose.animation.fadeIn(tween(200)) +
                androidx.compose.animation.slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { distance })
                .togetherWith(androidx.compose.animation.fadeOut(tween(100)))
                .using(null)
        }, label = "Page transition"
    ) { page -> content(page) }
}
