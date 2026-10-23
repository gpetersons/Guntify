package com.music.spotui.updates

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.music.spotui.BuildConfig
import com.music.spotui.ui.theme.AppSecondary
import com.music.spotui.ui.theme.AppSurface
import com.music.spotui.ui.theme.AppText
import kotlinx.coroutines.launch

private val UpdateGreen = androidx.compose.ui.graphics.Color(0xFF1ED760)

/** Single Activity-level host: navigation and the mini-player cannot cover the prompt. */
@Composable
fun UpdateUiHost() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by AppUpdates.state.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        AppUpdates.openInstaller(context)
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { AppUpdates.check(context) }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { AppUpdates.check(context) }
    val release = state.release
    if (state.showPrompt && release != null) {
        AlertDialog(
            onDismissRequest = { AppUpdates.cancel(context) },
            containerColor = AppSurface,
            title = { Text("A new Sprotify is ready", color = AppText, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sprotify ${release.version} · build ${release.code}", color = UpdateGreen)
                    Text("Install over your current app to keep your music, history and settings. Android will ask you to confirm installation.", color = AppSecondary)
                    if (state.downloading) {
                        LinearProgressIndicator(progress = { state.progress / 100f }, color = UpdateGreen, modifier = Modifier.fillMaxWidth())
                        Text("Downloading and verifying… ${state.progress}%", color = AppText)
                    } else if (release.notes.isNotBlank()) {
                        Text(release.notes.take(2800), color = AppSecondary, fontSize = 13.sp)
                    }
                    if (state.error.isNotBlank()) Text(state.error, color = AppText)
                }
            },
            confirmButton = {
                TextButton(enabled = !state.downloading, onClick = {
                    scope.launch {
                        if (AppUpdates.download(context)) {
                            if (context.packageManager.canRequestPackageInstalls()) AppUpdates.openInstaller(context)
                            else permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                        }
                    }
                }) { Text(if (state.downloading) "Downloading…" else "Install", color = UpdateGreen) }
            },
            dismissButton = { TextButton(onClick = { AppUpdates.cancel(context) }) { Text("Cancel", color = AppSecondary) } },
        )
    }
}

@Composable
fun UpdateSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by AppUpdates.state.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 160.dp)) {
        Text("Keep your music moving", color = AppText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Installed: Sprotify ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}", color = AppSecondary)
        Text("Updates come from Sprotify releases in the gpetersons/Sprotify GitHub repository. Sprotify checks each time it starts. While it stays open, returning to it checks at most once an hour. Cancel dismisses a release until you check manually or a newer release arrives.", color = AppSecondary)
        Button(onClick = { scope.launch { AppUpdates.check(context, manual = true) } }, enabled = !state.checking && !state.downloading,
            colors = ButtonDefaults.buttonColors(containerColor = UpdateGreen, contentColor = androidx.compose.ui.graphics.Color.Black)) {
            Text(if (state.checking) "Checking…" else "Check for updates")
        }
        if (state.checking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = UpdateGreen)
        state.release?.let { release ->
            Text("Available: ${release.version} · build ${release.code}", color = UpdateGreen, fontWeight = FontWeight.Bold)
            TextButton(enabled = !state.downloading, onClick = { AppUpdates.showAvailable() }) { Text("View update and install", color = UpdateGreen) }
        }
        if (state.checked && state.release == null && state.error.isBlank()) Text("You're up to date.", color = UpdateGreen)
        if (state.error.isNotBlank()) Text(state.error, color = AppText)
        Text("You may need to allow 'Install unknown apps' for Sprotify once. Installation always needs your confirmation; your current app stays installed.", color = AppSecondary)
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gpetersons/Sprotify/releases"))) }) {
            Text("View GitHub release notes", color = UpdateGreen)
        }
    }
}
