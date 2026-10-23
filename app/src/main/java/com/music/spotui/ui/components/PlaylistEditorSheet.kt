package com.music.spotui.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.*
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.ui.theme.*
import com.music.spotui.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun PlaylistEditorSheet(id: String, name: String, navController: NavController, newlyCreated: Boolean = false,
    initialRemove: SongsModel? = null, suggestions: List<SongsModel> = emptyList(), libraryWarning: String? = null, onRetrySave: (() -> Unit)? = null,
    onChanged: () -> Unit = {}, onDismiss: () -> Unit) {
    val model: PlaylistEditorViewModel = hiltViewModel(key = "playlist-editor-$id")
    val picker: RadioCreatorViewModel = hiltViewModel(key = "playlist-picker-$id")
    val player: PlayerViewModel = hiltViewModel()
    val state by model.state.collectAsState()
    val library by picker.library.collectAsState(); val results by picker.results.collectAsState(); val tracks by picker.tracks.collectAsState()
    val context = LocalContext.current
    var tab by remember { mutableStateOf(if(newlyCreated) "Add songs" else "In playlist") }
    var source by remember { mutableStateOf("Recommended") }
    var opened by remember { mutableStateOf<LibraryEntry?>(null) }
    var query by remember { mutableStateOf("") }
    var askDownload by remember { mutableStateOf(false) }
    val chosen = remember { mutableStateMapOf<String, SongsModel>() }
    var menu by remember { mutableStateOf<SongsModel?>(null) }
    var removing by remember(initialRemove) { mutableStateOf(initialRemove) }
    var observedChanges by remember { mutableIntStateOf(0) }
    LaunchedEffect(id) { model.load(id, newlyCreated); picker.loadLibrary(); picker.search(""); picker.closePlaylist() }
    LaunchedEffect(state.changes) { if(state.changes > observedChanges) { observedChanges = state.changes; chosen.clear(); onChanged(); android.widget.Toast.makeText(context,"Playlist updated",android.widget.Toast.LENGTH_SHORT).show() } }
    DisposableEffect(id) { onDispose { picker.search(""); picker.closePlaylist() } }
    if (askDownload) ConfirmDownloadAllDialog(state.songs.size, onDismiss = { askDownload = false }) { com.music.spotui.di.Downloads.downloadAll(state.songs, context) }
    fun start(song: SongsModel, index: Int, shuffled: Boolean = false) {
        if (!shuffled) player.updateQueue(state.songs)
        player.activeCollection.value = com.music.spotui.data.entity.PlaybackCollection("playlist:$id", "playlist", id, name, state.songs.firstOrNull()?.coverUri.orEmpty(), "Playlist")
        com.music.spotui.data.preferences.HomeRecents.rememberPlaylist(context, id, name, state.songs.firstOrNull()?.coverUri.orEmpty())
        com.music.spotui.di.SongPlayer.playSong(song.url, context)
        player.updateSongState(song.coverUri, song.title, song.singer, true, song.id, index, name)
    }
    menu?.let { song -> SongOptionsSheet(song, navController, context, onDismiss = { menu = null },
        onRemoveFromPlaylist = if(tab == "In playlist" && state.editable) ({ removing = song; menu = null }) else null) }
    removing?.let { song -> AlertDialog(onDismissRequest = { removing = null }, containerColor = AppSurface,
        title = { Text("Remove this song?",color=AppText) }, text = { Text("Remove ${song.title} from $name? Your Liked Songs won't change.",color=AppSecondary) },
        confirmButton = { TextButton(enabled=state.editable&&!state.loading&&!state.busy,onClick = { model.remove(song); removing=null }) { Text("Remove",color=AppPalette) } },
        dismissButton = { TextButton(onClick = { removing=null }) { Text("Cancel",color=AppSecondary) } }) }
    ModalBottomSheet(onDismissRequest = { if(!state.busy) onDismiss() },containerColor=AppSurface,contentColor=AppText,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).navigationBarsPadding().imePadding().padding(horizontal=16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(if(newlyCreated)"Make it yours" else "Edit playlist",color=AppSecondary,fontSize=12.sp); Text(name,color=AppText,fontSize=23.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis) }
                TextButton(enabled=!state.busy,onClick=onDismiss) { Text("Done",color=AppPalette) }
            }
            if(libraryWarning != null) {
                Text("Created on Spotify, but saving it to your library needs another try.",color=AppSecondary)
                if(onRetrySave != null) TextButton(onClick=onRetrySave) { Text("Save to Spotify library",color=AppPalette) }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { for(label in listOf("In playlist","Add songs")) FilterChip(selected=tab==label,onClick={tab=label},label={Text(label)}) }
            if(tab=="In playlist" && state.songs.isNotEmpty()) {
                val active=player.activeCollection.value?.key=="playlist:$id"
                val playing=active && player.currentSongPlayingState.value
                val downloaded=rememberCollectionDownloaded(state.songs,context)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
                    Text("${state.songs.size} songs",color=AppSecondary,modifier=Modifier.weight(1f))
                    CollectionAction(androidx.compose.ui.res.painterResource(if(downloaded)com.music.spotui.R.drawable.added else com.music.spotui.R.drawable.ic_download),"Download playlist",active=downloaded){if(!downloaded)askDownload=true}
                    CollectionAction(androidx.compose.ui.res.painterResource(com.music.spotui.R.drawable.ic_player_shuffle),"Shuffle",active=active&&player.shuffleState.value,smart=active&&player.smartShuffleState.value) {
                        if(active)player.cycleShuffle() else player.startShuffled(state.songs)?.let { start(it,0,true) }
                    }
                    CollectionPlayButton(playing) { when {
                        playing -> { com.music.spotui.di.SongPlayer.pause(); player.updateSongState(player.currentSongCoverUri.value,player.currentSongTitle.value,player.currentSongSinger.value,false,player.currentSongId.value,player.currentSongIndex.value,player.currentSongAlbum.value) }
                        active -> { com.music.spotui.di.SongPlayer.play(); player.updateSongState(player.currentSongCoverUri.value,player.currentSongTitle.value,player.currentSongSinger.value,true,player.currentSongId.value,player.currentSongIndex.value,player.currentSongAlbum.value) }
                        else -> start(state.songs.first(),0)
                    } }
                }
            }
            if(state.loading || state.busy) LinearProgressIndicator(color=AppPalette,modifier=Modifier.fillMaxWidth())
            if(state.error.isNotBlank()) { Text(state.error,color=AppText,fontSize=13.sp); if(!state.busy) TextButton(onClick={model.load(id,newlyCreated)}){Text("Refresh",color=AppPalette)} }
            if(!state.loading && !state.editable) Text("You can edit playlists you own. Save songs to one of your playlists using the song menu.",color=AppSecondary,fontSize=13.sp)
            if(tab=="Add songs") {
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    for(label in listOf("Recommended","Liked Songs","Playlists","Search")) FilterChip(selected=source==label,onClick={source=label;opened=null;query="";picker.search("");if(label=="Liked Songs")picker.openPlaylist("liked-songs")},label={Text(label)})
                }
                if(opened!=null) TextButton(onClick={opened=null;picker.closePlaylist()}){Text("‹ ${opened!!.name}",color=AppPalette)}
                OutlinedTextField(value=query,onValueChange={query=it;if(source=="Search")picker.search(it)},placeholder={Text(if(source=="Search")"Search Spotify" else "Find a song or playlist")},singleLine=true,modifier=Modifier.fillMaxWidth(),
                    colors=OutlinedTextFieldDefaults.colors(focusedTextColor=AppText,unfocusedTextColor=AppText,focusedBorderColor=AppPalette,unfocusedBorderColor=AppRaised))
            }
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(top=10.dp,bottom=16.dp)) {
                if(tab=="Add songs" && source=="Playlists" && opened==null) {
                    when(val response=library) {
                        is Response.Loading -> item { Text("Loading your playlists…",color=AppSecondary) }
                        is Response.Error -> item { Text(response.error,color=AppText);TextButton(onClick={picker.loadLibrary()}){Text("Try again",color=AppPalette)} }
                        is Response.Success -> {
                            val rows=response.data.filter{it.spotifyId!=id && (query.isBlank()||it.name.contains(query,true))}
                            if(rows.isEmpty()) item { Text("No playlists found.",color=AppSecondary) }
                            itemsIndexed(rows,key={_,p->p.spotifyId}) { _,entry -> Row(Modifier.fillMaxWidth().clickable{opened=entry;query="";picker.openPlaylist(entry.spotifyId)}.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                                GlideImage(model=entry.coverUri,contentDescription=null,modifier=Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop)
                                Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(entry.name,color=AppText);Text("Choose songs  ›",color=AppSecondary,fontSize=12.sp)}
                            } }
                        }
                    }
                } else {
                    val response: Response<List<SongsModel>> = if(tab=="In playlist") Response.Success(state.songs) else when(source) {
                        "Recommended" -> Response.Success(suggestions)
                        "Search" -> results
                        else -> tracks
                    }
                    when(response) {
                        is Response.Loading -> item { Text("Loading songs…",color=AppSecondary) }
                        is Response.Error -> item { Text(response.error,color=AppText);TextButton(onClick={if(source=="Search")picker.search(query)else picker.openPlaylist(opened?.spotifyId?:"liked-songs")}){Text("Try again",color=AppPalette)} }
                        is Response.Success -> {
                            val rows=response.data.filter{query.isBlank()||source=="Search"||it.title.contains(query,true)||it.singer.contains(query,true)}
                            if(rows.isEmpty()) item { Text(if(tab=="In playlist")"No songs yet. Add a few favorites." else if(source=="Recommended")"Choose Liked Songs, a playlist, or search Spotify to add songs." else "No songs found.",color=AppSecondary) }
                            itemsIndexed(rows,key={i,s->s.playlistItemUid.ifBlank{"${s.id}:$i"}}) { index,song ->
                                Row(Modifier.fillMaxWidth().padding(vertical=4.dp).clip(RoundedCornerShape(12.dp)).background(AppRaised.copy(alpha=0.3f)).clickable{
                                    if(tab=="Add songs") { if(chosen.containsKey(song.spotifyTrackId))chosen.remove(song.spotifyTrackId)else if(chosen.size<50&&song.spotifyTrackId.isNotBlank())chosen[song.spotifyTrackId]=song }
                                    else {
                                        player.updateQueue(rows)
                                        player.activeCollection.value=com.music.spotui.data.entity.PlaybackCollection("playlist:$id","playlist",id,name,state.songs.firstOrNull()?.coverUri.orEmpty(),"Playlist")
                                        com.music.spotui.di.SongPlayer.playSong(song.url,context)
                                        player.updateSongState(song.coverUri,song.title,song.singer,true,song.id,index,name)
                                    }
                                }.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                    GlideImage(model=song.coverUri,contentDescription=null,modifier=Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop)
                                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(song.title,color=AppText,maxLines=1,overflow=TextOverflow.Ellipsis);Text(song.singer,color=AppSecondary,maxLines=1,fontSize=12.sp)}
                                    if(tab=="Add songs") Checkbox(checked=chosen.containsKey(song.spotifyTrackId),enabled=state.editable&&!state.busy&&song.spotifyTrackId.isNotBlank(),onCheckedChange={checked->if(!checked)chosen.remove(song.spotifyTrackId)else if(chosen.size<50)chosen[song.spotifyTrackId]=song},colors=CheckboxDefaults.colors(checkedColor=AppPalette))
                                    SongOverflowButton(song.title){menu=song}
                                }
                            }
                        }
                    }
                }
            }
            if(tab=="Add songs") Button(enabled=state.editable&&!state.busy&&!state.loading&&chosen.isNotEmpty(),onClick={model.add(chosen.values.toList())},modifier=Modifier.fillMaxWidth().padding(bottom=16.dp),colors=ButtonDefaults.buttonColors(containerColor=AppPalette,contentColor=androidx.compose.ui.graphics.Color.Black)) { Text("Add ${chosen.size} song${if(chosen.size==1)"" else "s"}") }
        }
    }
}

@Composable
fun SongOverflowButton(title: String, onClick: () -> Unit) {
    IconButton(onClick=onClick,modifier=Modifier.size(48.dp)) { Icon(painter=androidx.compose.ui.res.painterResource(com.music.spotui.R.drawable.ic_dots),contentDescription="More options for $title",tint=AppSecondary,modifier=Modifier.size(20.dp)) }
}
