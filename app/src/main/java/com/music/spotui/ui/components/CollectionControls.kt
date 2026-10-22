package com.music.spotui.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.ui.theme.*

@Composable
fun CollectionPlayButton(playing: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled,
        modifier = Modifier.height(48.dp).animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760), contentColor = Color.Black)) {
        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (playing) "Pause" else "Play", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CollectionAction(painter: Painter, label: String, active: Boolean = false, enabled: Boolean = true, smart: Boolean = false, onClick: () -> Unit) {
    val tint by animateColorAsState(if (active) Color(0xFF1ED760) else AppText, label = label)
    Box(Modifier.size(44.dp).clickable(enabled = enabled,
        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(painter, contentDescription = label, tint = if (enabled) tint else AppSecondary.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
        if (smart && active) Text("◆", color = Color(0xFF1ED760), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
fun CollectionBackBar(color: Color, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(artworkHeader(color)).statusBarsPadding().height(40.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppText)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CollectionArtwork(image: String, description: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        GlideImage(model = image, contentDescription = description, contentScale = ContentScale.Crop,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)))
    }
}

@Composable
fun rememberCollectionDownloaded(songs: List<com.music.spotui.data.entity.SongsModel>, context: android.content.Context): Boolean {
    var downloaded by remember(songs) { mutableStateOf(false) }
    LaunchedEffect(songs) {
        while (true) {
            downloaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.music.spotui.di.Downloads.allDownloaded(songs, context)
            }
            kotlinx.coroutines.delay(1_000L)
        }
    }
    return downloaded
}
