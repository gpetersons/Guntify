package com.music.spotui.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.data.preferences.SmartDownloads
import com.music.spotui.ui.theme.*

@Composable
fun SmartDownloadSettings() {
    val context = LocalContext.current
    val revision by SmartDownloads.revision.collectAsState()
    val status by com.music.spotui.di.SmartDownloadManager.status.collectAsState()
    val config = remember(revision) { Triple(SmartDownloads.enabled(context), SmartDownloads.count(context), SmartDownloads.wifiOnly(context)) }
    var amount by remember(config.second) { mutableStateOf(config.second.toFloat()) }
    Column(Modifier.fillMaxWidth()) {
        Text("Smart Downloads", color = AppText, fontSize = 23.sp)
        Text(status, color = AppPalette, modifier = Modifier.padding(vertical = 8.dp))
        Text("Keep your most-played songs ready offline. Uses recent Guntify listening, with a small boost for liked songs.", color = AppSecondary, modifier = Modifier.padding(vertical = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enable Smart Downloads", color = AppText)
            Switch(config.first, { SmartDownloads.setEnabled(context, it) })
        }
        Text("Keep ${amount.toInt()} songs", color = AppText, modifier = Modifier.padding(top = 16.dp))
        Slider(amount, { amount = it }, valueRange = 1f..100f, onValueChangeFinished = { SmartDownloads.setCount(context, amount.toInt()) }, enabled = config.first)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Unmetered networks only", color = AppText)
            Switch(config.third, { SmartDownloads.setWifi(context, it) })
        }
        Text("Updates as you listen and when you reopen Guntify. Automatic files rotate with your taste; manual downloads are kept. Turning this off stops updates and leaves downloaded songs available.", color = AppSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
}
