package com.music.spotui.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.R
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.viewmodel.PlayerViewModel

/**
 * The "play queue" — the track that's playing now plus everything coming up
 * (Spotify's Queue view). Tapping an upcoming track jumps straight to it;
 * dragging the handle reorders it; swiping a row removes it.
 */
@OptIn(ExperimentalGlideComposeApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(navController: NavController) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    QueueContent(navController, playerViewModel, onClose = { navController.navigateUp() }, modifier = Modifier.fillMaxSize().statusBarsPadding())
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(navController: NavController, player: PlayerViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = com.music.spotui.ui.theme.AppSurface, contentColor = com.music.spotui.ui.theme.AppText, dragHandle = null,
    ) {
        QueueContent(navController, player, onDismiss, Modifier.fillMaxHeight(0.78f))
    }
}

@OptIn(ExperimentalGlideComposeApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun QueueContent(navController: NavController, playerViewModel: PlayerViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var menuSong by remember { mutableStateOf<com.music.spotui.data.entity.SongsModel?>(null) }
    menuSong?.let { selected ->
        key(selected.id) {
            com.music.spotui.ui.components.SongOptionsSheet(
                song = selected, navController = navController, context = context,
                onDismiss = { menuSong = null },
            )
        }
    }
    val queue by playerViewModel.queue
    val currentId = playerViewModel.currentSongId.value
    val curIdx = queue.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    val upcoming = if (queue.isNotEmpty()) queue.drop(curIdx + 1) else emptyList()
    val current = queue.getOrNull(curIdx)

    // Drag-to-reorder state: which upcoming row is dragged and its live offset.
    var draggingId by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 88.dp.toPx() }
    val playing = playerViewModel.currentSongPlayingState.value
    val shuffled = playerViewModel.shuffleState.value
    val refreshing by playerViewModel.radioRefreshing.collectAsState()
    val refreshMessage by playerViewModel.radioRefreshMessage.collectAsState()
    androidx.compose.runtime.LaunchedEffect(refreshMessage) {
        if (refreshMessage.isNotBlank()) { android.widget.Toast.makeText(context, refreshMessage, android.widget.Toast.LENGTH_LONG).show(); playerViewModel.clearRadioRefreshMessage() }
    }
    val collectionName = playerViewModel.activeCollection.value?.name ?: "your queue"
    val undo = playerViewModel.queueUndo.value
    androidx.compose.runtime.LaunchedEffect(undo) {
        if (undo != null) {
            kotlinx.coroutines.delay(8_000L)
            if (playerViewModel.queueUndo.value == undo) playerViewModel.dismissQueueUndo()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(com.music.spotui.ui.theme.AppSurface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp).size(44.dp, 5.dp).background(Color.Gray, RoundedCornerShape(4.dp)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp, 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close",
                tint = com.music.spotui.ui.theme.AppText,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onClose() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Queue", color = com.music.spotui.ui.theme.AppText, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (playerViewModel.activeCollection.value?.type == "radio") {
                androidx.compose.material3.IconButton(enabled = !refreshing, onClick = { playerViewModel.refreshRadioQueue() }) {
                    if (refreshing) androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp), color = Color(0xFF1ED760), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = "Refresh upcoming Radio songs", tint = Color(0xFF1ED760))
                }
            }
        }

        Text("Playing $collectionName", color = com.music.spotui.ui.theme.AppSecondary, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (undo != null) {
                Text(undo.label, color = com.music.spotui.ui.theme.AppSecondary, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(enabled = playerViewModel.canUndoQueue(), onClick = { playerViewModel.undoQueueEdit() }) {
                    Text("Undo", color = com.music.spotui.ui.theme.AppPalette)
                }
            } else {
                androidx.compose.material3.TextButton(enabled = upcoming.isNotEmpty(), onClick = { playerViewModel.clearUnplayedQueue() }) {
                    Text("Clear unplayed songs", color = com.music.spotui.ui.theme.AppSecondary)
                }
            }
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
            modifier = Modifier
                .weight(1f)
        ) {
            current?.let {
                item {
                    Text(
                        "Now playing",
                        color = com.music.spotui.ui.theme.AppText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 6.dp)
                    )
                    QueueRow(song = it, highlight = true, onClick = {}, isPlaying = playing, onTogglePlay = { if (playing) SongPlayer.pause() else SongPlayer.play() }, onMenu = { menuSong = it })
                }
            }
            if (upcoming.isNotEmpty()) {
                item {
                    Text(
                        if (shuffled) "Shuffling from: $collectionName" else "Next from: $collectionName",
                        color = com.music.spotui.ui.theme.AppSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(24.dp, 18.dp, 16.dp, 10.dp)
                    )
                }
                itemsIndexed(upcoming, key = { _, s -> s.id }) { upIdx, song ->
                    val isDragging = draggingId == song.id
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                playerViewModel.removeFromQueue(song)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF7A1F1F))
                                    .padding(horizontal = 24.dp),
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = com.music.spotui.ui.theme.AppText)
                            }
                        },
                        content = {
                            QueueRow(
                                song = song,
                                highlight = false,
                                onMenu = { menuSong = song },
                                smartSuggestion = playerViewModel.isSmartSuggestion(song.id),
                                isDragging = isDragging,
                                dragOffsetY = if (isDragging) dragOffset else 0f,
                                onClick = {
                                    val abs = queue.indexOfFirst { it.id == song.id }
                                    if (abs >= 0) playerViewModel.playSongAt(queue, abs, context)
                                },
                                onDragStart = { draggingId = song.id; dragOffset = 0f },
                                onDragEnd = { draggingId = -1; dragOffset = 0f },
                                onDragDelta = { dy ->
                                    dragOffset += dy
                                    val abs = queue.indexOfFirst { it.id == song.id }
                                    if (abs >= 0) {
                                        if (dragOffset > rowHeightPx / 2 && abs < queue.size - 1) {
                                            playerViewModel.moveQueueItem(abs, abs + 1)
                                            dragOffset -= rowHeightPx
                                        } else if (dragOffset < -rowHeightPx / 2 && upIdx > 0) {
                                            playerViewModel.moveQueueItem(abs, abs - 1)
                                            dragOffset += rowHeightPx
                                        }
                                    }
                                },
                            )
                        }
                    )
                }
            }
        }
        Button(
            onClick = { playerViewModel.cycleShuffle() }, enabled = queue.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = com.music.spotui.ui.theme.AppRaised, contentColor = if (shuffled) Color(0xFF1ED760) else com.music.spotui.ui.theme.AppText),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Shuffle, contentDescription = null)
            Text(if (playerViewModel.smartShuffleState.value) "◆ Smart Shuffle" else if (shuffled) "Shuffle on" else "Shuffle", modifier = Modifier.padding(12.dp))
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun QueueRow(
    song: com.music.spotui.data.entity.SongsModel,
    highlight: Boolean,
    smartSuggestion: Boolean = false,
    onClick: () -> Unit,
    onMenu: () -> Unit = {},
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {},
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragDelta: (Float) -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffsetY }
            .background(if (isDragging) com.music.spotui.ui.theme.AppRaised else com.music.spotui.ui.theme.AppSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(24.dp, 12.dp)
    ) {
        GlideImage(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp)),
            model = song.coverUri,
            contentScale = ContentScale.Crop,
            loading = placeholder(R.drawable.placeholder),
            failure = placeholder(R.drawable.placeholder),
            contentDescription = ""
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            if (highlight) { com.music.spotui.ui.components.PlayingBars(isPlaying); Spacer(Modifier.width(8.dp)) }
            Text(
                text = (if (smartSuggestion) "◆ " else "") + song.title,
                color = if (highlight) Color(0xFF1ED760) else com.music.spotui.ui.theme.AppText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            }
            Text(
                text = song.singer,
                color = com.music.spotui.ui.theme.AppSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (highlight) {
            Box(Modifier.padding(start = 12.dp).size(44.dp).background(Color.White, RoundedCornerShape(50)).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { onTogglePlay() }, contentAlignment = Alignment.Center) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", tint = Color.Black)
            }
        } else {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Reorder",
                tint = Color(0xFFB3B3B3),
                modifier = Modifier
                    .size(28.dp)
                    .pointerInput(song.id) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.y)
                            },
                        )
                    }
            )
        }
        Icon(
            Icons.Default.MoreVert, contentDescription = "Options for ${song.title}", tint = Color(0xFFB3B3B3),
            modifier = Modifier.padding(start = 8.dp).size(36.dp).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { onMenu() },
        )
    }
}


