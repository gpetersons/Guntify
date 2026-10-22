package com.music.spotui.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.spotui.ui.viewmodel.BackupViewModel

@Composable
fun BackupControls(viewModel: BackupViewModel = hiltViewModel()) {
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    var audio by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Uri?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.saveBackup(uri, audio)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> selected = uri }
    Text("Your data, saved safely", color = com.music.spotui.ui.theme.AppText, fontWeight = FontWeight.Bold)
    Text("Export local listening history, settings, likes, library data, Radio sessions, queue and playlist covers. Account passwords and login cookies are excluded.", color = com.music.spotui.ui.theme.AppSecondary, modifier = Modifier.padding(vertical = 12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Include downloaded audio", color = com.music.spotui.ui.theme.AppText, modifier = Modifier.weight(1f).padding(top = 12.dp))
        Switch(checked = audio, onCheckedChange = { audio = it }, enabled = !busy)
    }
    Text("Audio makes backups larger. Music linked from external folders must be selected again after a reinstall.", color = com.music.spotui.ui.theme.AppSecondary)
    Spacer(Modifier.height(16.dp))
    Button(enabled = !busy, onClick = { exporter.launch("USpot-backup-${java.time.LocalDate.now()}.zip") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760), contentColor = Color.Black)) { Text("Export backup") }
    OutlinedButton(enabled = !busy, onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("Import backup", color = com.music.spotui.ui.theme.AppText) }
    if (busy) CircularProgressIndicator(color = Color(0xFF1ED760), modifier = Modifier.padding(vertical = 16.dp))
    if (message.isNotBlank()) Text(message, color = com.music.spotui.ui.theme.AppSecondary, modifier = Modifier.padding(vertical = 12.dp))
    Text("Save the ZIP in Downloads, an SD card or your own cloud drive—not inside the app. This feature cannot recover data from an older installation that has no export button.", color = com.music.spotui.ui.theme.AppSecondary)
    selected?.let { uri ->
        AlertDialog(onDismissRequest = { selected = null }, containerColor = com.music.spotui.ui.theme.AppSurface, titleContentColor = com.music.spotui.ui.theme.AppText, textContentColor = com.music.spotui.ui.theme.AppSecondary,
            title = { Text("Restore this backup?") },
            text = { Text("Playback will pause. Backed-up local settings and history will replace their current values. This does not change your Spotify account. Export your current data first if you want to keep it.") },
            confirmButton = { TextButton(onClick = { selected = null; viewModel.restore(uri) }) { Text("Restore", color = Color(0xFF1ED760)) } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancel", color = com.music.spotui.ui.theme.AppText) } })
    }
}
