package com.music.spotui.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.collectAsState
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
import com.music.spotui.data.api.Response
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.Loader
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppPalette
import com.music.spotui.ui.viewmodel.LikedSongsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun LikedSongsScreen(navController: NavController) {

    val likedSongsViewModel: LikedSongsViewModel = hiltViewModel()
    val songsResp by likedSongsViewModel.songs.collectAsState()
    val context = LocalContext.current

    val songs = (songsResp as? Response.Success)?.data.orEmpty()


    var menuSong by remember { mutableStateOf<com.music.spotui.data.entity.SongsModel?>(null) }
    menuSong?.let { sel ->
        com.music.spotui.ui.components.SongOptionsSheet(
            song = sel,
            navController = navController,
            context = context,
            onDismiss = {
                // If the song was unliked in the menu, drop it from the list right
                // away (the Spotify-side removal is already in flight).
                if (!com.music.spotui.data.preferences.isSongLiked(context, sel.id.toString())) {
                    likedSongsViewModel.removeLocally(sel.id)
                }
                menuSong = null
            },
        )
    }

    // Spotify's Liked Songs uses a purple → dark gradient.
    val likedColor = Color(0xFF5038A0)

    Surface(
        color = Color(AppBackground.toArgb()),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
    ) {
        if (songsResp is Response.Loading) {
            Loader()
            return@Surface
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                com.music.spotui.ui.components.CollectionBackBar(likedColor) { navController.navigateUp() }
            }
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(AppBackground.toArgb()))
                    .consumeWindowInsets(innerPadding)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(com.music.spotui.ui.theme.artworkHeader(likedColor), Color(AppBackground.toArgb())),
                                    startY = 0f,
                                ),
                            ),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            modifier = Modifier.padding(20.dp, 5.dp, 0.dp, 0.dp),
                            text = "Liked Songs",
                            color = com.music.spotui.ui.theme.AppText,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 0.dp),
                            text = "${songs.size} songs",
                            color = com.music.spotui.ui.theme.AppSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(20.dp, 0.dp)
                        ) {
                            // Download all liked songs for offline playback.
                            var askDownloadAll by remember { mutableStateOf(false) }
                            val likedDownloaded = com.music.spotui.ui.components.rememberCollectionDownloaded(songs, context)
                            if (songs.isNotEmpty()) {
                                com.music.spotui.ui.components.CollectionAction(painter = painterResource(if (likedDownloaded) R.drawable.added else R.drawable.ic_download), label = "Download liked songs", active = likedDownloaded, enabled = songs.isNotEmpty()) {

                                            if (!likedDownloaded) askDownloadAll = true
                                        
                            }
                                if (askDownloadAll) {
                                    com.music.spotui.ui.components.ConfirmDownloadAllDialog(
                                        songs.size, onDismiss = { askDownloadAll = false },
                                    ) {
                                        com.music.spotui.di.Downloads.downloadAll(songs, context)
                                        android.widget.Toast.makeText(
                                            context, "Downloading ${songs.size} tracks…", android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // Shuffle-play: start liked songs in random order.
                                com.music.spotui.ui.components.CollectionAction(painter = painterResource(R.drawable.ic_player_shuffle), label = "Shuffle play", active = likedSongsViewModel.shuffleActive, smart = likedSongsViewModel.smartShuffleActive, enabled = songs.isNotEmpty()) {

                                            if (likedSongsViewModel.collectionActive) likedSongsViewModel.cycleShuffle()
                                        else likedSongsViewModel.startShuffled(songs)?.let { first ->
                                                SongPlayer.playSong(first.url, context)
                                                likedSongsViewModel.updateSongState(
                                                    first.coverUri,
                                                    first.title,
                                                    first.singer,
                                                    true,
                                                    first.id,
                                                    0,
                                                    "Liked Songs",
                                                )
                                            }
                                        
                            }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            // Always visible: pause when playing, resume when this
                            // list's track is paused, otherwise start from the top.
                            if (songs.isNotEmpty()) {
                                val playing = likedSongsViewModel.collectionActive && likedSongsViewModel.currentSongPlayingState.value
                                com.music.spotui.ui.components.CollectionPlayButton(playing = playing, onClick = {
                                            when {
                                                playing -> likedSongsViewModel.setPlaying(false)
                                                likedSongsViewModel.collectionActive ->
                                                    likedSongsViewModel.setPlaying(true)
                                                else -> {
                                                    likedSongsViewModel.updateQueue(songs)
                                                    SongPlayer.playSong(songs[0].url, context)
                                                    likedSongsViewModel.updateSongState(
                                                        songs[0].coverUri,
                                                        songs[0].title,
                                                        songs[0].singer,
                                                        true,
                                                        songs[0].id,
                                                        0,
                                                        "Liked Songs"
                                                    )
                                                }
                                            
                                            }
                                })
                            }
                        }
                    }
                }

                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    com.music.spotui.ui.components.SwipeToQueue(song) {
                    val currentColor = if (song.id == likedSongsViewModel.currentSongId.value)
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
                                    likedSongsViewModel.updateQueue(songs)
                                    SongPlayer.playSong(song.url, context)
                                    likedSongsViewModel.updateSongState(
                                        song.coverUri,
                                        song.title,
                                        song.singer,
                                        true,
                                        song.id,
                                        index,
                                        "Liked Songs"
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
                            Text(
                                text = song.title,
                                color = currentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = song.singer,
                                color = com.music.spotui.ui.theme.AppSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    }
                }

                item { Spacer(modifier = Modifier.padding(80.dp)) }
            }
        }
    }
}
