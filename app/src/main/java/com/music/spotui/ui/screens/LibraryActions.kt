package com.music.spotui.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.spotui.data.api.Api
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.viewmodel.LibraryViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

/** Mirrors the queue swipe in the opposite direction; the row springs back before confirmation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySwipe(entry: LibraryEntry, onRemove: (LibraryEntry) -> Unit, content: @Composable () -> Unit) {
    val removable = entry.spotifyId.isNotBlank() && entry.spotifyId !in listOf(Api.LIKED_SONGS_ID, Api.DOWNLOADS_ID)
    if (!removable) { content(); return }
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart) onRemove(entry)
        false
    })
    SwipeToDismissBox(state = state, enableDismissFromStartToEnd = false, enableDismissFromEndToStart = true,
        backgroundContent = {
            val revealed = with(LocalDensity.current) { runCatching { -state.requireOffset() }.getOrDefault(0f).coerceAtLeast(0f).toDp() }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.fillMaxHeight().width(revealed).background(Color(0xFFD94040)).clipToBounds(), contentAlignment = Alignment.CenterEnd) {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Remove from library",
                        tint = com.music.spotui.ui.theme.AppText, modifier = Modifier.padding(end = 20.dp).size(24.dp))
                }
            }
        }) { content() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
internal fun CreatePlaylistSheet(viewModel: LibraryViewModel, navController: NavController, onDismiss: () -> Unit) {
    val busy by viewModel.busy.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val playlist by viewModel.created.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val added by viewModel.added.collectAsState()
    val entries by viewModel.entries.collectAsState()
    var name by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, containerColor = Color(0xFF181818),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
            if (actionError.isNotBlank()) {
                Text(actionError, color = Color(0xFFE57373), modifier = Modifier.padding(bottom = 12.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                TextButton(onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://open.spotify.com/collection/playlists"))) }) { Text("Open Spotify", color = Color(0xFF1ED760)) }
            }
            Text(playlist?.name ?: "Create playlist", color = com.music.spotui.ui.theme.AppText, style = MaterialTheme.typography.headlineSmall)
            if (playlist == null) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(100) }, label = { Text("Playlist name") },
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = com.music.spotui.ui.theme.AppText, unfocusedTextColor = com.music.spotui.ui.theme.AppText))
                Button(enabled = !busy && name.isNotBlank(), onClick = { viewModel.create(name) }) { Text("Create on Spotify") }
            } else {
                Text("Recommended from Spotify and your local listening", color = com.music.spotui.ui.theme.AppSecondary, modifier = Modifier.padding(vertical = 12.dp))
                if (added.isNotEmpty()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val cover = (entries as? com.music.spotui.data.api.Response.Success)?.data?.firstOrNull { it.spotifyId == playlist!!.id }?.coverUri
                    GlideImage(model = cover, contentDescription = "Playlist cover", modifier = Modifier.size(96.dp))
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    items(suggestions, key = { it.spotifyTrackId }) { song ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlideImage(model = song.coverUri, contentDescription = null, modifier = Modifier.size(44.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(song.title, color = com.music.spotui.ui.theme.AppText, maxLines = 1)
                                Text(song.singer, color = com.music.spotui.ui.theme.AppSecondary, maxLines = 1)
                            }
                            TextButton(enabled = !busy && song !in added, onClick = { viewModel.addRecommendation(song) }) {
                                Text(if (song in added) "Added" else "Add", color = Color(0xFF1DB954))
                            }
                        }
                    }
                }
                if (!busy && suggestions.isEmpty()) Text("No recommendations available. Open your playlist to add songs.", color = com.music.spotui.ui.theme.AppSecondary)
                Row {
                    TextButton(enabled = !busy, onClick = { onDismiss() }) { Text("Done") }
                    TextButton(enabled = !busy, onClick = { val p = playlist ?: return@TextButton; onDismiss(); navController.navigate(playlistRoute(p.id, p.name)) }) { Text("Open playlist") }
                }
            }
            if (busy) CircularProgressIndicator(color = Color(0xFF1DB954), modifier = Modifier.padding(12.dp).size(24.dp))
        }
    }
}
