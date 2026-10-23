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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatePlaylistSheet(viewModel: LibraryViewModel, navController: NavController, onDismiss: () -> Unit) {
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.actionError.collectAsState()
    val playlist by viewModel.created.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    var name by remember { mutableStateOf("") }
    val created = playlist
    if (created != null) {
        com.music.spotui.ui.components.PlaylistEditorSheet(created.id, created.name, navController,
            newlyCreated = true, suggestions = suggestions,
            libraryWarning = error.takeIf { it.isNotBlank() } ?: created.libraryWarning,
            onRetrySave = { viewModel.saveCreated() }, onDismiss = { if (!busy) onDismiss() })
        return
    }
    ModalBottomSheet(onDismissRequest = { if(!busy) onDismiss() },containerColor=com.music.spotui.ui.theme.AppSurface,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Your next favorite playlist",color=com.music.spotui.ui.theme.AppText,style=MaterialTheme.typography.headlineSmall)
            Text("Give it a name. Then choose songs from your likes, playlists, or recommendations.",color=com.music.spotui.ui.theme.AppSecondary)
            OutlinedTextField(value=name,onValueChange={name=it.take(100)},label={Text("Playlist name")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=com.music.spotui.ui.theme.AppText,unfocusedTextColor=com.music.spotui.ui.theme.AppText,focusedBorderColor=com.music.spotui.ui.theme.AppPalette))
            if(error.isNotBlank())Text(error,color=com.music.spotui.ui.theme.AppText)
            Button(enabled=!busy&&name.isNotBlank(),onClick={viewModel.create(name)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=com.music.spotui.ui.theme.AppPalette,contentColor=Color.Black)){Text(if(busy)"Creating…"else"Create playlist")}
            if(busy)LinearProgressIndicator(color=com.music.spotui.ui.theme.AppPalette,modifier=Modifier.fillMaxWidth())
        }
    }
}
