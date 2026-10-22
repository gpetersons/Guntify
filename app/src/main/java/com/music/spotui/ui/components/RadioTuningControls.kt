package com.music.spotui.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.data.preferences.RadioTaste
import com.music.spotui.ui.theme.*

@Composable
fun RadioTuningControls(station: String, onChanged: () -> Unit) {
    val context = LocalContext.current
    val revision by RadioTaste.revision.collectAsState()
    val selected = remember(revision, station) { RadioTaste.mode(context, station) }
    Text("Tune Radio", color = AppText, modifier = Modifier.padding(top = 16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("close" to "Stay close", "balanced" to "Balanced", "explore" to "Explore more").forEach { (key, title) ->
            Text((if (selected == key) "● " else "○ ") + title,
                color = if (selected == key) androidx.compose.ui.graphics.Color(0xFF1ED760) else AppSecondary,
                fontSize = 12.sp, modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { RadioTaste.setMode(context, station, key); onChanged() }.padding(vertical = 16.dp))
        }
    }
    Text("Stay close uses only your original songs and direct Spotify suggestions. Balanced favors those originals. Explore more gives newer songs greater influence. Similarity is not a genre guarantee.", color = AppSecondary)
}
