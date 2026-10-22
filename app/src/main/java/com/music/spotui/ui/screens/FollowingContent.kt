package com.music.spotui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.*
import com.music.spotui.data.recommendation.FollowingPolicy
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.Loader
import com.music.spotui.ui.components.playbackGlow
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FollowingContent(navController: NavController, response: Response<FollowingFeed>, refresh: () -> Unit) {
    val context = LocalContext.current
    val player: PlayerViewModel = hiltViewModel()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val green = Color(0xFF1ED760)
    when (response) {
        is Response.Loading -> Loader()
        is Response.Error -> Column(Modifier.padding(24.dp)) {
            Text(response.error, color = com.music.spotui.ui.theme.AppText)
            TextButton(onClick = refresh) { Text("Try again", color = green) }
        }
        is Response.Success -> {
            val feed = response.data
            val collection = PlaybackCollection("following:popular", "playlist", "following:popular", "Popular from your following", feed.popularSongs.firstOrNull()?.coverUri.orEmpty(), "Following mix")
            val active = player.activeCollection.value?.key == collection.key
            val shuffled = active && player.shuffleState.value
            fun playSongs(songs: List<SongsModel>, mix: PlaybackCollection, shuffle: Boolean = false, index: Int = 0) {
                if (songs.isEmpty()) return
                if (shuffle) player.startShuffled(songs) else player.updateQueue(songs)
                player.activeCollection.value = mix
                val queue = player.queue.value
                val track = queue.getOrNull(if (shuffle) 0 else index) ?: return
                player.updateSongState(track.coverUri, track.title, track.singer, true, track.id, if (shuffle) 0 else index, track.album)
                SongPlayer.playSong(track.url, context)
            }
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your following", color = com.music.spotui.ui.theme.AppText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Text("Fresh releases. Familiar voices.", color = com.music.spotui.ui.theme.AppSecondary)
                        }
                        TextButton(onClick = refresh) { Text("Refresh", color = green) }
                    }
                }
                if (feed.loadedArtists < feed.followedCount) item {
                    Text("Loading artists ${feed.loadedArtists}/${feed.followedCount}…", color = green, modifier = Modifier.padding(horizontal = 20.dp))
                }
                if (feed.followedCount == 0) item {
                    Text("Follow artists in Spotify to see their releases and popular songs here.", color = com.music.spotui.ui.theme.AppSecondary, modifier = Modifier.padding(24.dp))
                }
                if (feed.failedArtists > 0) item {
                    Text("Couldn't load ${feed.failedArtists} followed artists. Refresh to try again.", color = com.music.spotui.ui.theme.AppSecondary, modifier = Modifier.padding(horizontal = 20.dp))
                }
                if (feed.popularSongs.isNotEmpty()) item {
                    Column(Modifier.padding(horizontal = 20.dp).playbackGlow(active, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(com.music.spotui.ui.theme.artworkHeader(Color(0xFF245346)), com.music.spotui.ui.theme.AppSurface))).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                                repeat(2) { row -> Row(Modifier.weight(1f)) {
                                    repeat(2) { col ->
                                        val artwork = feed.popularSongs[(row * 2 + col) % feed.popularSongs.size].coverUri
                                        GlideImage(model = artwork, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight())
                                    }
                                } }
                            }
                            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                                Text("MADE FROM YOUR FOLLOWING", color = green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (active) com.music.spotui.ui.components.PlayingBars(player.currentSongPlayingState.value)
                                Text("On repeat, everywhere", color = com.music.spotui.ui.theme.AppText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Popular songs from artists you follow, ranked by Spotify play counts.", color = com.music.spotui.ui.theme.AppSecondary, modifier = Modifier.padding(vertical = 14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle following mix", tint = if (shuffled) green else com.music.spotui.ui.theme.AppText,
                                modifier = Modifier.size(28.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (active) player.updateShuffleState(!shuffled) else playSongs(feed.popularSongs, collection, shuffle = true)
                                })
                            Spacer(Modifier.weight(1f))
                            com.music.spotui.ui.components.CollectionPlayButton(active && player.currentSongPlayingState.value) {
                                if (active) { if (player.currentSongPlayingState.value) SongPlayer.pause() else SongPlayer.play() }
                                else playSongs(feed.popularSongs, collection)
                            }
                        }
                    }
                }
                val recent = feed.releases.filter { FollowingPolicy.isRecent(it.date) }
                val releases = recent.ifEmpty { feed.releases }
                if (releases.isNotEmpty()) {
                    item { FollowingTitle(if (recent.isNotEmpty()) "New releases" else "Latest releases") }
                    if (recent.isEmpty()) item { Text("No confirmed releases in the last 90 days. Here are the latest available.", color = com.music.spotui.ui.theme.AppSecondary, modifier = Modifier.padding(horizontal = 20.dp)) }
                    item { LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(releases, key = { it.id }) { release ->
                            val releaseActive = com.music.spotui.data.recommendation.PlaybackHighlightPolicy.album(release.name, release.artist, player.activeCollection.value)
                            Column(Modifier.width(152.dp).clickable { navController.navigate(albumRoute(release.name, release.artist)) }) {
                                Box(Modifier.size(152.dp).playbackGlow(releaseActive, RoundedCornerShape(8.dp))) {
                                GlideImage(model = release.image, contentDescription = release.name, contentScale = ContentScale.Crop, modifier = Modifier.size(152.dp).clip(RoundedCornerShape(8.dp)))
                                    if (releaseActive) com.music.spotui.ui.components.PlaybackBadge(player.currentSongPlayingState.value, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                                }
                                Text(release.name, color = com.music.spotui.ui.theme.AppText, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                                Text(release.artist, color = com.music.spotui.ui.theme.AppSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                if (release.date.isNotBlank()) Text(release.date.take(10), color = com.music.spotui.ui.theme.AppSecondary, fontSize = 11.sp)
                            }
                        }
                    } }
                }
                if (feed.popularSongs.isNotEmpty()) {
                    item { FollowingTitle("Most popular songs") }
                    items(feed.popularSongs.take(10), key = { "popular:${it.id}" }) { song ->
                        FollowingSong(song, collection.key) { playSongs(feed.popularSongs, collection, index = feed.popularSongs.indexOf(song)) }
                    }
                }
                feed.artists.filter { it.songs.isNotEmpty() }.forEach { artist ->
                    item(key = "artist:${artist.id}") {
                        val artistActive = com.music.spotui.data.recommendation.PlaybackHighlightPolicy.active(HomeItem.Artist(artist.name, artist.image, artist.id), player.activeCollection.value, player.currentSongId.value)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).playbackGlow(artistActive).clickable { navController.navigate(artistRoute(artist.name, artist.id)) }.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlideImage(model = artist.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(50)))
                            Text("Popular from ${artist.name}", color = com.music.spotui.ui.theme.AppText, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).weight(1f))
                            if (artistActive) com.music.spotui.ui.components.PlayingBars(player.currentSongPlayingState.value)
                        }
                    }
                    items(artist.songs.take(3), key = { "${artist.id}:${it.song.id}" }) { track ->
                        FollowingSong(track.song, "artist:${artist.id}") { playSongs(artist.songs.map { it.song }, PlaybackCollection("artist:${artist.id}", "artist", artist.id, artist.name, artist.image, "Popular songs"), index = artist.songs.indexOf(track)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingTitle(title: String) {
    Text(title, color = com.music.spotui.ui.theme.AppText, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun FollowingSong(song: SongsModel, sourceKey: String, onPlay: () -> Unit) {
    val player: PlayerViewModel = hiltViewModel()
    val active = com.music.spotui.data.recommendation.PlaybackHighlightPolicy.followingSong(song.id, sourceKey, player.currentSongId.value, player.activeCollection.value)
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).playbackGlow(active).clip(RoundedCornerShape(14.dp)).background(if (active) com.music.spotui.ui.theme.AppSurface else Color.Transparent).clickable(onClick = onPlay).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        GlideImage(model = song.coverUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(54.dp).clip(RoundedCornerShape(4.dp)))
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(song.title, color = com.music.spotui.ui.theme.AppText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.singer, color = com.music.spotui.ui.theme.AppSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (active) com.music.spotui.ui.components.PlayingBars(player.currentSongPlayingState.value)
        else Icon(Icons.Default.PlayArrow, contentDescription = null, tint = com.music.spotui.ui.theme.AppSecondary)
    }
}
