package com.music.spotui.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.R
import com.music.spotui.data.entity.PlaybackCollection
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.*
import com.music.spotui.data.recommendation.ListeningStatsPolicy
import com.music.spotui.data.recommendation.RankedListeningSong
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.*
import com.music.spotui.ui.navigation.LocalBottomOverlayInset
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.theme.*
import com.music.spotui.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.TextStyle
import java.util.Locale

private val StatsGreen = Color(0xFF1ED760)
private val StatsAccent: Color get() = if (AppearanceSettings.effectiveDark) StatsGreen else Color(0xFF087F36)

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun StatsContent(navController: NavController) {
    val context = LocalContext.current
    val player: PlayerViewModel = hiltViewModel()
    val playsRevision by ListeningHistorySignals.revision.collectAsState()
    val timeRevision by ListeningStatsSignals.revision.collectAsState()
    val backupRevision by com.music.spotui.ui.viewmodel.BackupSignals.revision.collectAsState()
    var days by rememberSaveable { mutableStateOf(30) }
    val summary by produceState<Pair<Int, com.music.spotui.data.recommendation.ListeningSummary>?>(
        initialValue = null, key1 = days, key2 = playsRevision to timeRevision, key3 = backupRevision
    ) {
        while (true) {
            value = days to withContext(Dispatchers.IO) { ListeningStatsPolicy.summary(getListeningHistory(context), System.currentTimeMillis(), days) }
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val stats = summary?.takeIf { it.first == days }?.second
    val songs = stats?.songs.orEmpty().take(10)
    val playable = songs.map { it.song }.filter { it.url.isNotBlank() }
    val period = when (days) { 7 -> "week"; 30 -> "month"; else -> "all time" }
    val source = PlaybackCollection("stats:$days", "playlist", "stats:$days", "Your top songs • $period",
        playable.firstOrNull()?.coverUri.orEmpty(), "Your Guntify listening")
    val active = player.activeCollection.value?.key == source.key
    val playing = active && player.currentSongPlayingState.value
    var menuSong by remember { mutableStateOf<SongsModel?>(null) }
    menuSong?.let { song -> SongOptionsSheet(song, navController, context) { menuSong = null } }

    fun playSong(song: SongsModel? = null, shuffled: Boolean = false) {
        if (playable.isEmpty()) return
        if (song != null && active && player.currentSongId.value == song.id) {
            if (player.currentSongPlayingState.value) SongPlayer.pause() else SongPlayer.play()
            return
        }
        if (shuffled) player.startShuffled(playable) else player.updateQueue(playable)
        player.activeCollection.value = source
        val queue = player.queue.value
        val index = if (shuffled || song == null) 0 else queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        val track = queue.getOrNull(index) ?: return
        player.updateSongState(track.coverUri, track.title, track.singer, true, track.id, index, track.album)
        SongPlayer.playSong(track.url, context)
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomOverlayInset.current + 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize().background(AppBackground)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your listening", color = AppText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Your music. Your numbers.", color = AppSecondary, modifier = Modifier.padding(top = 4.dp))
                }
                Icon(Icons.Default.BarChart, contentDescription = null, tint = StatsAccent,
                    modifier = Modifier.size(46.dp).background(StatsGreen.copy(alpha = 0.12f), CircleShape).padding(10.dp))
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "Week", 30 to "Month", 0 to "All time").forEach { (value, label) ->
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                        .background(if (days == value) StatsGreen else AppRaised)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { days = value }
                        .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (days == value) Color.Black else AppText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (stats == null) item { Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Loader() } }
        else if (stats.plays == 0) item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(AppSurface, RoundedCornerShape(20.dp)).padding(24.dp)) {
                Text("Your soundtrack starts here", color = AppText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Listen for at least 30 seconds and your favorites will appear here. Try another time range to see earlier plays.",
                    color = AppSecondary, modifier = Modifier.padding(top = 12.dp))
            }
        } else {
            item {
                Column(Modifier.padding(horizontal = 20.dp).playbackGlow(active, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(artworkHeader(Color(0xFF245346)), AppSurface))).padding(20.dp)) {
                    Text("YOUR PERSONAL CHART", color = StatsAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (days == 0) "Your all-time rotation" else "The sound of your $period",
                        color = AppText, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.size(108.dp).clip(RoundedCornerShape(12.dp))) {
                            repeat(2) { row -> Row(Modifier.weight(1f)) {
                                repeat(2) { col ->
                                    val cover = songs.getOrNull((row * 2 + col) % songs.size)?.song?.coverUri
                                    GlideImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.weight(1f).fillMaxHeight().background(AppRaised))
                                }
                            } }
                        }
                        Column(Modifier.padding(start = 16.dp).weight(1f)) {
                            Text("YOUR #1", color = AppSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(songs.first().song.title, color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(songs.first().song.singer, color = AppSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            Text("${songs.first().plays} ${if (songs.first().plays == 1) "play" else "plays"}", color = StatsAccent, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        CollectionAction(painterResource(R.drawable.ic_player_shuffle), "Shuffle your top songs",
                            active = active && player.shuffleState.value, enabled = playable.isNotEmpty(),
                            smart = active && player.smartShuffleState.value) {
                            if (active) player.cycleShuffle() else playSong(shuffled = true)
                        }
                        if (active) PlayingBars(player.currentSongPlayingState.value)
                        Spacer(Modifier.weight(1f))
                        CollectionPlayButton(playing, enabled = playable.isNotEmpty()) {
                            if (active) { if (playing) SongPlayer.pause() else SongPlayer.play() } else playSong()
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsMetric("Plays", stats.plays.toString(), Modifier.weight(1f))
                    val minutes = stats.recordedMs / 60_000
                    StatsMetric("Recorded", if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m", Modifier.weight(1f))
                    StatsMetric("Artists", stats.artistCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(AppSurface, RoundedCornerShape(20.dp)).padding(18.dp)) {
                    Text("Your last 7 days", color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Plays lasting at least 30 seconds", color = AppSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    val maximum = stats.activity.maxOfOrNull { it.plays }?.coerceAtLeast(1) ?: 1
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        stats.activity.forEach { day ->
                            val fraction by animateFloatAsState(day.plays.toFloat() / maximum, label = "Daily listening")
                            Column(Modifier.weight(1f).semantics { contentDescription = "${day.date}: ${day.plays} plays" }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.height(92.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                    Box(Modifier.widthIn(max = 26.dp).fillMaxWidth().height((fraction * 86).coerceAtLeast(3f).dp)
                                        .background(if (day.plays == 0) AppRaised else StatsGreen.copy(alpha = if (day == stats.activity.last()) 1f else 0.65f), RoundedCornerShape(6.dp)))
                                }
                                Text(day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = AppSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
            item { StatsHeading("Top songs", "Ranked by your plays") }
            items(songs, key = { it.song.spotifyTrackId.ifBlank { "local:${it.song.id}" } }) { ranked ->
                val index = songs.indexOf(ranked)
                val songActive = active && player.currentSongId.value == ranked.song.id
                StatsSong(ranked, index + 1, songActive, player.currentSongPlayingState.value,
                    onPlay = { playSong(ranked.song) }, onOptions = { menuSong = ranked.song })
            }
            if (stats.artists.isNotEmpty()) {
                item { StatsHeading("Your top artists", "The voices you come back to") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(stats.artists.take(10), key = { it.name.lowercase(Locale.ROOT) }) { artist ->
                            Column(Modifier.width(132.dp).clip(RoundedCornerShape(18.dp)).background(AppSurface)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { navController.navigate(artistRoute(artist.name)) }.padding(12.dp)) {
                                GlideImage(model = artist.artwork, contentDescription = "Music artwork for ${artist.name}", contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(108.dp).clip(RoundedCornerShape(12.dp)).background(AppRaised))
                                Text(artist.name, color = AppText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                                Text("${artist.plays} plays", color = AppSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Only your listening in Guntify counts here. Recorded time is approximate; older history may include only the first qualifying seconds.",
                color = AppSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun StatsMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(AppSurface).padding(horizontal = 12.dp, vertical = 16.dp)) {
        Text(value, color = AppText, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = AppSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun StatsHeading(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(title, color = AppText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = AppSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun StatsSong(ranked: RankedListeningSong, rank: Int, active: Boolean, playing: Boolean, onPlay: () -> Unit, onOptions: () -> Unit) {
    val song = ranked.song
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).playbackGlow(active)
        .clip(RoundedCornerShape(14.dp)).background(if (active) AppSurface else Color.Transparent)
        .clickable(enabled = song.url.isNotBlank(), interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPlay)
        .padding(start = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rank.toString(), color = if (active) StatsAccent else AppSecondary, fontSize = 12.sp, modifier = Modifier.width(22.dp))
        GlideImage(model = song.coverUri, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(AppRaised))
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(song.title, color = if (active) StatsAccent else AppText, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(song.singer, color = AppSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${ranked.plays} ${if (ranked.plays == 1) "play" else "plays"}", color = AppSecondary, fontSize = 11.sp)
        }
        if (active) PlayingBars(playing)
        else Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AppSecondary, modifier = Modifier.size(20.dp))
        Box(Modifier.size(48.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOptions), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options for ${song.title}", tint = AppSecondary)
        }
    }
}
