package com.music.spotui.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.R
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.getDownloadedSongs
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppPalette
import com.music.spotui.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(navController: NavController) {

    val playerViewModel: PlayerViewModel = hiltViewModel()
    val context = LocalContext.current
    val accent = AppPalette

    // Completed downloads (from prefs) + live in-progress ones (from SongPlayer). Poll
    // while the screen is open so a track appears here the moment its download starts,
    // shows a live percentage, and moves into the list when it finishes.
    var songs by remember { mutableStateOf(getDownloadedSongs(context)) }
    var inProgress by remember {
        mutableStateOf(com.music.spotui.di.Downloads.downloadingSnapshot())
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            val snap = com.music.spotui.di.Downloads.downloadingSnapshot()
            if (snap.size != inProgress.size) songs = getDownloadedSongs(context)
            inProgress = snap
            kotlinx.coroutines.delay(400)
        }
    }

    var menuSong by remember { mutableStateOf<SongsModel?>(null) }
    menuSong?.let { sel ->
        com.music.spotui.ui.components.SongOptionsSheet(
            song = sel,
            navController = navController,
            context = context,
            onDismiss = { menuSong = null },
        )
    }

    Surface(
        color = Color(AppBackground.toArgb()),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                com.music.spotui.ui.components.CollectionBackBar(accent) { navController.navigateUp() }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(AppBackground.toArgb()))
                    .consumeWindowInsets(innerPadding)
                    .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(com.music.spotui.ui.theme.artworkHeader(accent), Color(AppBackground.toArgb())),
                                startY = 0f,
                            ),
                        ),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.padding(20.dp, 5.dp, 0.dp, 0.dp),
                        text = "Downloaded",
                        color = com.music.spotui.ui.theme.AppText,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 0.dp),
                        text = "${songs.size} songs • available offline",
                        color = com.music.spotui.ui.theme.AppSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (songs.isNotEmpty()) {
                    val collectionActive = com.music.spotui.audio.CollectionPlaybackPolicy.active("local:downloads",
                        playerViewModel.activeCollection.value?.key, playerViewModel.currentSongId.value, playerViewModel.queue.value.map { it.id })
                    val shuffled = collectionActive && playerViewModel.shuffleState.value
                    fun startDownloads(shuffle: Boolean) {
                        if (shuffle) playerViewModel.startShuffled(songs) else playerViewModel.updateQueue(songs)
                        playerViewModel.activeCollection.value = com.music.spotui.data.entity.PlaybackCollection(
                            "local:downloads", "local", "downloads", "Downloaded", songs.first().coverUri, "Offline music")
                        val first = playerViewModel.queue.value.firstOrNull() ?: return
                        playerViewModel.updateSongState(first.coverUri, first.title, first.singer, true, first.id, 0, first.album)
                        SongPlayer.playSong(first.url, context)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.music.spotui.ui.components.CollectionAction(androidx.compose.ui.res.painterResource(R.drawable.ic_player_shuffle), "Shuffle downloads", shuffled) {
                            if (collectionActive) playerViewModel.updateShuffleState(!shuffled) else startDownloads(true)
                        }
                        Spacer(Modifier.weight(1f))
                        com.music.spotui.ui.components.CollectionPlayButton(collectionActive && playerViewModel.currentSongPlayingState.value) {
                            if (collectionActive) {
                                if (playerViewModel.currentSongPlayingState.value) SongPlayer.pause() else SongPlayer.play()
                            } else startDownloads(false)
                        }
                    }
                }

                // ── Bulk actions: Export all / Clear all ──
                if (songs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp, 0.dp, 20.dp, 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        Text(
                            text = "Export all",
                            color = Color(AppPalette.toArgb()),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(com.music.spotui.ui.theme.AppSurface)
                                .clickable {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val (n, dest) = com.music.spotui.data.preferences.exportDownloads(context)
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                context,
                                                if (n > 0) "Exported $n track${if (n == 1) "" else "s"} to $dest"
                                                else dest,
                                                android.widget.Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                        Text(
                            text = "Clear all",
                            color = Color(0xFFE57373),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(com.music.spotui.ui.theme.AppSurface)
                                .clickable {
                                    val n = com.music.spotui.data.preferences.clearAllDownloads(context)
                                    songs = getDownloadedSongs(context)
                                    android.widget.Toast.makeText(
                                        context, "Removed $n download${if (n == 1) "" else "s"}",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                }

                // ── In-progress downloads (with live progress bar) ──
                if (inProgress.isNotEmpty()) {
                    androidx.compose.material3.TextButton(
                        onClick = { com.music.spotui.di.Downloads.cancelAll() },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) { Text("Cancel all downloads (${inProgress.size})", color = com.music.spotui.ui.theme.AppText) }
                }
                inProgress.forEach { (song, pct) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp, 5.dp).clip(RoundedCornerShape(14.dp)).background(com.music.spotui.ui.theme.AppSurface).padding(10.dp),
                    ) {
                        GlideImage(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            model = song.coverUri,
                            failure = placeholder(R.drawable.placeholder),
                            contentScale = ContentScale.Crop,
                            contentDescription = "",
                        )
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(text = song.title, color = com.music.spotui.ui.theme.AppText, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { (pct.coerceIn(0, 100)) / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = accent,
                                trackColor = com.music.spotui.ui.theme.AppRaised,
                            )
                        }
                        Text(
                            text = "$pct%",
                            color = com.music.spotui.ui.theme.AppSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }

                if (songs.isEmpty() && inProgress.isEmpty()) {
                    Text(
                        text = "No downloads yet. Tap ⋯ on a track and choose Download.",
                        color = com.music.spotui.ui.theme.AppSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    repeat(songs.size) { index ->
                        com.music.spotui.ui.components.SwipeToQueue(songs[index]) {
                        val song = songs[index]
                        val currentColor = if (song.id == playerViewModel.currentSongId.value)
                            Color(AppPalette.toArgb()) else com.music.spotui.ui.theme.AppText

                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp, 5.dp).clip(RoundedCornerShape(14.dp)).background(com.music.spotui.ui.theme.AppSurface).padding(10.dp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onLongClick = { menuSong = song },
                                    onClick = {
                                        playerViewModel.updateQueue(songs)
                                        playerViewModel.activeCollection.value = com.music.spotui.data.entity.PlaybackCollection("local:downloads", "local", "downloads", "Downloaded", song.coverUri, "Offline music")
                                        SongPlayer.playSong(song.url, context)
                                        playerViewModel.updateSongState(
                                            song.coverUri, song.title, song.singer,
                                            true, song.id, index, "Downloaded"
                                        )
                                    },
                                )
                        ) {
                            GlideImage(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                model = song.coverUri,
                                failure = placeholder(R.drawable.placeholder),
                                contentScale = ContentScale.Crop,
                                contentDescription = ""
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(text = song.title, color = currentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(text = song.singer, color = com.music.spotui.ui.theme.AppSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                            // Visible way into the song menu (export, remove…); long-press alone was hidden.
                            androidx.compose.material3.IconButton(onClick = { menuSong = song }) {
                                androidx.compose.material3.Icon(
                                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_dots),
                                    contentDescription = "More options for ${song.title}",
                                    tint = Color.Gray,
                                )
                            }
                        }
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(80.dp))
            }
        }
    }
}
