package com.music.spotui.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.LaunchedEffect
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
import com.music.spotui.data.entity.AlbumsModel
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.addLikedAlbumId
import com.music.spotui.data.preferences.addLikedSongId
import com.music.spotui.data.preferences.isAlbumLiked
import com.music.spotui.data.preferences.isSongLiked
import com.music.spotui.data.preferences.removeLikedAlbumId
import com.music.spotui.data.preferences.removeLikedSongId
import com.music.spotui.di.Palette
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.LikedSongsScreen
import com.music.spotui.ui.components.Loader
import com.music.spotui.ui.components.Snackbar
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppPalette
import com.music.spotui.ui.viewmodel.AlbumViewModel
import kotlinx.coroutines.delay


@Composable
fun AlbumScreen(navController: NavController, albumName: String, artist: String = "") {


    val albumViewModel : AlbumViewModel = hiltViewModel()
    val songs by albumViewModel.songs.collectAsState()
    val albums by albumViewModel.albums.collectAsState()

    // Load this album's actual tracks from Spotify (by name, disambiguated by artist).
    LaunchedEffect(albumName, artist) {
        albumViewModel.loadAlbumSongs(albumName, artist)
    }

    val context = LocalContext.current




    Log.d("check", albumName.toString())

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
    ) {
        val albumsResponse = (albums as? Response.Success)?.data.orEmpty()
        val songsResponse = (songs as? Response.Success)?.data.orEmpty()

        when {
            albums is Response.Loading && songs is Response.Loading -> {
                Log.d("homeMain", "loading..-albums")
                Loader()
            }

            else -> {
                Log.d("homeMain", "albums ready")
                if (albumName == "Liked Songs"){
                    LikedSongsScreen(albumsResponse, songsResponse, navController, context)
                }
                else{
                    SumUpAlbumScreen(navController = navController,albumViewModel, albumsResponse, songsResponse, albumName, context)
                }
            }
        }
    }

}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun SumUpAlbumScreen(
    navController: NavController,
    albumViewModel: AlbumViewModel,
    albums: List<AlbumsModel>,
    songs: List<SongsModel>,
    albumName: String,
    context: Context
) {
    // `songs` is already this album's track list (loaded by AlbumViewModel).
    val albumSongs: List<SongsModel> = songs

    val albumByName : Map<String, List<AlbumsModel>> = albums.groupBy { it.name }
    // The album may not be in the cached new-releases list (e.g. opened from
    // search) — fall back to a model built from the album's first track.
    val album : List<AlbumsModel> = albumByName[albumName]
        ?: listOf(
            AlbumsModel(
                id = albumName.hashCode() and 0x7fffffff,
                artists = albumSongs.firstOrNull()?.singer ?: "",
                coverUri = albumSongs.firstOrNull()?.coverUri ?: "",
                name = albumName,
                time = "",
            )
        )
    var dominentColor by remember {
        mutableStateOf(Color(AppBackground.toArgb()))
    }
    Palette().extractSecondColorFromCoverUrl(context = context, album[0].coverUri){ color ->
        dominentColor = color
    }

    var isAlbumLiked by remember { mutableStateOf( isAlbumLiked(context, album[0].id.toString())) }

    var snackbarMessage by remember {
        mutableStateOf("")
    }
    var snackbarVisible by remember {
        mutableStateOf(false)
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
    LaunchedEffect(snackbarVisible) {
        delay(1500)
        snackbarVisible = false
    }



    Log.d("color", dominentColor.toString())
    Scaffold(
        containerColor = Color(AppBackground.toArgb()),
        topBar = {
            com.music.spotui.ui.components.CollectionBackBar(dominentColor) { navController.navigateUp() }
        }
    ) { innerPadding ->


        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
            .consumeWindowInsets(innerPadding)
            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 380.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(com.music.spotui.ui.theme.artworkHeader(dominentColor), Color(AppBackground.toArgb())),
                            startY = 0f,

                            ),

                        )
                ,
                verticalArrangement = Arrangement.Center,
               // horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                com.music.spotui.ui.components.CollectionArtwork(album[0].coverUri, "Collection artwork")
                Spacer(modifier = Modifier.padding(5.dp))
                Text(modifier = Modifier
                    .padding(20.dp, 5.dp, 0.dp, 0.dp),
                    text = albumName,
                    color = com.music.spotui.ui.theme.AppText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold)
                Text(modifier = Modifier
                    .padding(20.dp, 0.dp, 0.dp, 0.dp),
                    text = album[0].artists.ifBlank { albumSongs.firstOrNull()?.singer ?: "" },
                    color = com.music.spotui.ui.theme.AppText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium)
                Text(modifier = Modifier
                    .padding(20.dp, 0.dp, 0.dp, 0.dp),
                    text = "Album : ${album[0].time}",
                    color = com.music.spotui.ui.theme.AppSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(20.dp, 0.dp)
                ){

                    if (snackbarVisible){
                            Snackbar(showMessage = snackbarMessage)
                        }
                    else{
                        // Let the action icons take their natural width — a fixed
                        // 75dp squeezed the add + download buttons together.
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {

                            GlideImage(
                                modifier = Modifier
                                    .height(60.dp)
                                    .width(32.dp)
                                    .padding(0.dp, 5.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                ,
                                model = album[0].coverUri,
                                failure = placeholder(R.drawable.placeholder),
                                //loading = placeholder(R.drawable.album),
                                contentScale = ContentScale.Crop,
                                contentDescription = "",
                            )
                            Icon(
                                modifier = Modifier
                                    .size(23.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (isAlbumLiked) {
                                            removeLikedAlbumId(context, album[0].id.toString())
                                            snackbarMessage = "Removed from Library"
                                        } else {
                                            addLikedAlbumId(context, album[0].id.toString())
                                            snackbarMessage = "Added to Library"
                                        }
                                        isAlbumLiked = isAlbumLiked(context, album[0].id.toString())
                                        snackbarVisible = true

                                    },
                                painter = if (isAlbumLiked){
                                    painterResource(id = R.drawable.added)
                                }
                                else{
                                    painterResource(id = R.drawable.ic_add)
                                }
                                ,
                                tint = if (isAlbumLiked){
                                    Color(AppPalette.toArgb())
                                }
                                else{
                                    com.music.spotui.ui.theme.AppText
                                },
                                contentDescription = ""
                            )
                            // Download the whole album (all tracks) for offline playback.
                            var askDownloadAll by remember { mutableStateOf(false) }
                            val albumDownloaded = com.music.spotui.ui.components.rememberCollectionDownloaded(albumSongs, context)
                            com.music.spotui.ui.components.CollectionAction(painter = painterResource(if (albumDownloaded) R.drawable.added else R.drawable.ic_download), label = "Download album", active = albumDownloaded, enabled = albumSongs.isNotEmpty()) {

                                        if (!albumDownloaded && albumSongs.isNotEmpty()) askDownloadAll = true
                                    
                            }
                            if (askDownloadAll) {
                                com.music.spotui.ui.components.ConfirmDownloadAllDialog(
                                    albumSongs.size, onDismiss = { askDownloadAll = false },
                                ) {
                                    com.music.spotui.di.Downloads.downloadAll(albumSongs, context)
                                    snackbarMessage = "Downloading ${albumSongs.size} tracks…"
                                    snackbarVisible = true
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            // Shuffle-play: start the album in random order.
                            com.music.spotui.ui.components.CollectionAction(painter = painterResource(R.drawable.ic_player_shuffle), label = "Shuffle play", active = albumViewModel.shuffleActive, smart = albumViewModel.smartShuffleActive, enabled = albumSongs.isNotEmpty()) {

                                        if (albumViewModel.collectionActive) albumViewModel.cycleShuffle()
                                        else albumViewModel.startShuffled(albumSongs)?.let { first ->
                                            com.music.spotui.data.preferences.HomeFrequency.played(context, "album", albumName + "|" + album[0].artists, albumName, first.coverUri, album[0].artists)
                                            SongPlayer.playSong(first.url, context)
                                            albumViewModel.updateSongState(
                                                first.coverUri,
                                                first.title,
                                                first.singer,
                                                true,
                                                first.id,
                                                0,
                                                albumName,
                                            )
                                        }
                                    
                            }
                        }


                        // Always visible: pause when playing, resume when this
                        // album's track is paused, otherwise start from the top.
                        if (albumSongs.isNotEmpty()) {
                            val playing = albumViewModel.collectionActive && albumViewModel.currentSongPlayingState.value
                            com.music.spotui.ui.components.CollectionPlayButton(playing = playing, onClick = {
                                        when {
                                            playing -> albumViewModel.setPlaying(false)
                                            albumViewModel.collectionActive ->
                                                albumViewModel.setPlaying(true)
                                            else -> {
                                                albumViewModel.updateQueue(albumSongs)
                                                com.music.spotui.data.preferences.HomeFrequency.played(context, "album", albumName + "|" + album[0].artists, albumName, albumSongs[0].coverUri, album[0].artists)
                                                SongPlayer.playSong(albumSongs[0].url, context)
                                                albumViewModel.updateSongState(
                                                    albumSongs[0].coverUri,
                                                    albumSongs[0].title,
                                                    albumSongs[0].singer,
                                                    true,
                                                    albumSongs[0].id,
                                                    0,
                                                    albumName
                                                )
                                            }
                                        
                                            }
                                })
                        }
                    }




                }

            }

//            Spacer(modifier = Modifier.height(8.dp))

            if(albumSongs.isNotEmpty()){
                repeat(albumSongs.size) {song ->
                    com.music.spotui.ui.components.SwipeToQueue(albumSongs[song]) {


                    var isLiked by com.music.spotui.ui.components.rememberSongLiked(context, albumSongs[song].id.toString())
                    val likeState = albumViewModel.likeState.value
                    val songId = albumSongs[song].id

                    val currentPlayingIndicatorColor = if(songId == albumViewModel.currentSongId.value) Color(AppPalette.toArgb()) else com.music.spotui.ui.theme.AppText

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp, 8.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onLongClick = { menuSong = albumSongs[song] },
                                onClick = {
                                    albumViewModel.updateQueue(albumSongs)
                                    com.music.spotui.data.preferences.HomeFrequency.played(context, "album", albumName + "|" + album[0].artists, albumName, albumSongs[song].coverUri, album[0].artists)
                                    SongPlayer.playSong(albumSongs[song].url, context)
                                    albumViewModel.updateSongState(
                                        albumSongs[song].coverUri,
                                        albumSongs[song].title,
                                        albumSongs[song].singer,
                                        true,
                                        albumSongs[song].id,
                                        song,
                                        albumName
                                    )
                                },
                            )
                    ) {

                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        ) {
//                        GlideImage(
//                            modifier = Modifier.size(60.dp),
//                            model = albumSongs[song].coverUri,
//                            contentScale = ContentScale.Crop,
//                            contentDescription = ""
//                        )
                            Column {
                                Text(
                                    text = albumSongs[song].title,
                                    color = currentPlayingIndicatorColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = albumSongs[song].singer,
                                    color = com.music.spotui.ui.theme.AppSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        com.music.spotui.ui.components.SongOverflowButton(albumSongs[song].title) { menuSong = albumSongs[song] }
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isLiked) {
                                        removeLikedSongId(context, songId.toString())
                                    } else {
                                        addLikedSongId(context, songId.toString())
                                    }
                                    isLiked = isSongLiked(context, songId.toString())
                                    val track = albumSongs[song]
                                    com.music.spotui.data.api.SpotifySync.setTrackSaved(context, track.spotifyTrackId, isSongLiked(context, songId.toString()))
                                    albumViewModel.updateLikeState(!albumViewModel.likeState.value)

                                },
                            painter = if (isLiked){
                                painterResource(id = R.drawable.added)
                            }
                            else{
                                painterResource(id = R.drawable.ic_add)
                            }
                            ,
                            tint = if (isLiked){
                                Color.White
                            }else{
                                Color.Gray
                            },
                            contentDescription = ""
                        )
                    }
                    }
                }
            }

            Spacer(modifier = Modifier.padding(80.dp))
        }

    }
}
