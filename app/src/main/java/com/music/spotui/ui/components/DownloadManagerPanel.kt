package com.music.spotui.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.spotui.data.preferences.*
import com.music.spotui.di.Downloads
import com.music.spotui.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
fun DownloadManagerPanel() {
    val context=LocalContext.current
    var failed by remember { mutableStateOf(FailedDownloads.entries(context)) }
    var used by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while(true) {
            val snapshot=withContext(Dispatchers.IO) {
                FailedDownloads.entries(context) to getDownloadedEntries(context).sumOf { java.io.File(it.second).length() }
            }
            failed=snapshot.first;used=snapshot.second;delay(1000)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("On this device · ${String.format(java.util.Locale.ROOT,"%.1f",used/1048576.0)} MB",color=AppSecondary)
        if(failed.isNotEmpty()) {
            Text("Needs attention · ${failed.size}",color=AppText,fontWeight=FontWeight.Bold)
            TextButton(onClick={failed.forEach{Downloads.downloadSong(it.song,context)}}){Text("Retry all",color=AppPalette)}
            failed.forEach { entry ->
                Column(Modifier.fillMaxWidth().padding(vertical=8.dp)) {
                    Text(entry.song.title,color=AppText,maxLines=2)
                    Text(entry.reason,color=AppSecondary)
                    Row {
                        TextButton(enabled=!Downloads.isDownloading(entry.song.url),onClick={Downloads.downloadSong(entry.song,context)}){Text("Retry",color=AppPalette)}
                        TextButton(onClick={FailedDownloads.dismiss(context,entry.song.id);failed=FailedDownloads.entries(context)}){Text("Dismiss",color=AppSecondary)}
                    }
                }
            }
        }
    }
}
