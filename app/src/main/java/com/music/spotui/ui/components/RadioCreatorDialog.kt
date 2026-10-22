package com.music.spotui.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.ui.theme.*
import com.music.spotui.ui.viewmodel.RadioCreatorViewModel
import kotlinx.coroutines.launch

private val RadioGreen=Color(0xFF1ED760)
private fun Modifier.radioTap(enabled:Boolean=true,action:()->Unit)=clickable(enabled=enabled,indication=null,interactionSource=MutableInteractionSource(),onClick=action)

@Composable
fun RadioCreationMenu(onCreated:(String)->Unit) {
    var expanded by remember{mutableStateOf(false)}
    var mix by remember{mutableStateOf<Boolean?>(null)}
    mix?.let { mode -> RadioCreatorDialog(mix=mode,onDismiss={mix=null},onCreated={mix=null;onCreated(it)}) }
    Box {
        Box(Modifier.size(48.dp).radioTap{expanded=true},contentAlignment=Alignment.Center) {
            Text("+",color=AppText,fontSize=32.sp)
        }
        MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(surface=AppSurface),shapes=MaterialTheme.shapes.copy(extraSmall=RoundedCornerShape(20.dp))) {
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.widthIn(min=260.dp,max=320.dp)) {
                Text("CREATE A STATION",color=AppSecondary,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp,vertical=10.dp))
                DropdownMenuItem(text={Column{Text("USpot Radio",color=AppText,fontWeight=FontWeight.Bold);Text("Start with one song",color=AppSecondary,fontSize=12.sp)}},
                    onClick={expanded=false;mix=false})
                DropdownMenuItem(text={Column{Text("USpot Radio Mix",color=AppText,fontWeight=FontWeight.Bold);Text("Blend up to five songs",color=AppSecondary,fontSize=12.sp)}},
                    onClick={expanded=false;mix=true})
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun RadioCreatorDialog(mix:Boolean,onDismiss:()->Unit,onCreated:(String)->Unit,model:RadioCreatorViewModel=hiltViewModel()) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val max=if(mix)5 else 1
    var query by remember{mutableStateOf("")};var name by remember{mutableStateOf("")}
    var tab by remember{mutableStateOf("Your library")}
    var opened by remember{mutableStateOf<LibraryEntry?>(null)}
    val chosen=remember{mutableStateListOf<SongsModel>()}
    var saving by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    val results by model.results.collectAsState();val library by model.library.collectAsState();val tracks by model.tracks.collectAsState()
    val fieldColors=OutlinedTextFieldDefaults.colors(focusedTextColor=AppText,unfocusedTextColor=AppText,focusedBorderColor=RadioGreen,unfocusedBorderColor=AppRaised,focusedContainerColor=AppRaised,unfocusedContainerColor=AppRaised,cursorColor=RadioGreen,focusedPlaceholderColor=AppSecondary,unfocusedPlaceholderColor=AppSecondary)
    LaunchedEffect(Unit){model.search("");model.closePlaylist();model.loadLibrary()}
    DisposableEffect(Unit){onDispose{model.search("");model.closePlaylist()}}
    fun toggle(song:SongsModel) {
        error=""
        if(chosen.any{it.spotifyTrackId==song.spotifyTrackId})chosen.removeAll{it.spotifyTrackId==song.spotifyTrackId}
        else if(!mix){chosen.clear();chosen.add(song)}
        else if(chosen.size<max)chosen.add(song)
        else error="Five songs selected. Uncheck one to choose another."
    }
    Dialog(onDismissRequest={if(!saving)onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(color=AppBackground,modifier=Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Column(Modifier.background(Brush.verticalGradient(listOf(RadioGreen.copy(alpha=0.18f),AppBackground))).padding(horizontal=20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("CREATE",color=AppSecondary,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        Text("×",color=AppText,fontSize=30.sp,modifier=Modifier.size(44.dp).radioTap(!saving){onDismiss()}.wrapContentSize())
                    }
                    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(bottom=12.dp)) {
                        Column {
                            Text(if(mix)"USpot Radio Mix" else "USpot Radio",color=AppText,fontSize=25.sp,fontWeight=FontWeight.Bold)
                            Text(if(mix)"Your songs. One continuous mix." else "One song. A station that keeps going.",color=AppSecondary,fontSize=12.sp)
                        }
                    }
                    if(mix)OutlinedTextField(value=name,onValueChange={name=it.take(80)},placeholder={Text("Name your mix (optional)")},colors=fieldColors,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().padding(bottom=10.dp),singleLine=true,enabled=!saving)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(bottom=10.dp)) {
                        listOf("Your library","Search Spotify").forEach { label ->
                            Text(label,color=if(tab==label)Color.Black else AppText,fontSize=13.sp,fontWeight=FontWeight.Bold,
                                modifier=Modifier.clip(CircleShape).background(if(tab==label)RadioGreen else AppRaised).radioTap(!saving){tab=label;query="";model.search("");keyboard?.hide()}.padding(horizontal=15.dp,vertical=10.dp))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(tab=="Your library" && opened!=null)Text("‹",color=AppText,fontSize=32.sp,modifier=Modifier.size(40.dp).radioTap(!saving){opened=null;query="";model.closePlaylist()}.wrapContentSize())
                    OutlinedTextField(value=query,onValueChange={query=it;if(tab=="Search Spotify")model.search(it)},placeholder={Text(if(tab=="Search Spotify")"Search Spotify songs" else if(opened!=null)"Find in ${opened?.name}" else "Find a playlist")},colors=fieldColors,shape=RoundedCornerShape(12.dp),modifier=Modifier.weight(1f),singleLine=true,enabled=!saving)
                }
                if(tab=="Your library" && opened!=null)Text(opened!!.name,color=AppText,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=20.dp,top=12.dp))
                Text(if(mix)"Choose up to 5 songs across playlists or search" else "Choose 1 song to start your station",color=AppSecondary,fontSize=12.sp,modifier=Modifier.padding(horizontal=20.dp,vertical=10.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if(tab=="Your library" && opened==null) {
                        LazyColumn(contentPadding=PaddingValues(start=12.dp,end=12.dp,bottom=12.dp)) {
                            when(val state=library) {
                                is Response.Loading -> item{RadioLoading()}
                                is Response.Error -> item{RadioMessage(state.error){model.loadLibrary()}}
                                is Response.Success -> {
                                    val rows=state.data.filter{query.isBlank() || it.name.contains(query,true)}
                                    if(rows.isEmpty())item{RadioMessage("No playlists found.")}
                                    items(rows,key={it.spotifyId}) { entry ->
                                        Row(Modifier.fillMaxWidth().radioTap(!saving){opened=entry;query="";keyboard?.hide();model.openPlaylist(entry.spotifyId)}.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                            if(entry.spotifyId=="liked-songs")Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFF5434B8),Color(0xFFB9D5C4)))) ,contentAlignment=Alignment.Center){Text("♥",color=Color.White,fontSize=26.sp)}
                                            else GlideImage(model=entry.coverUri,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
                                            Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(entry.name,color=AppText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(entry.subtitle,color=AppSecondary,fontSize=12.sp,maxLines=1)}
                                            Text("›",color=AppSecondary,fontSize=26.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val state=if(tab=="Search Spotify")results else tracks
                        key(tab,opened?.spotifyId,query) {
                            LazyColumn(contentPadding=PaddingValues(start=12.dp,end=12.dp,bottom=12.dp)) {
                                when(state) {
                                    is Response.Loading -> item{RadioLoading()}
                                    is Response.Error -> item{RadioMessage(state.error){if(tab=="Search Spotify")model.search(query)else opened?.let{model.openPlaylist(it.spotifyId)}}}
                                    is Response.Success -> {
                                        val rows=state.data.filter{it.spotifyTrackId.isNotBlank() && (tab=="Search Spotify" || query.isBlank() || it.title.contains(query,true) || it.singer.contains(query,true))}.distinctBy{it.spotifyTrackId}
                                        if(rows.isEmpty())item{RadioMessage(if(tab=="Search Spotify" && query.isBlank())"Find a song or artist on Spotify, then check the songs you want." else "No songs found.")}
                                        items(rows,key={it.spotifyTrackId}) { song -> RadioSelectableSong(song,chosen.any{it.spotifyTrackId==song.spotifyTrackId},!saving){toggle(song)} }
                                    }
                                }
                            }
                        }
                    }
                }
                Column(Modifier.background(AppSurface).padding(horizontal=20.dp,vertical=12.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${chosen.size} of $max selected",color=RadioGreen,fontWeight=FontWeight.Bold,fontSize=13.sp);if(chosen.isNotEmpty())Text("Clear",color=AppSecondary,fontSize=13.sp,modifier=Modifier.radioTap(!saving){chosen.clear();error=""}.padding(horizontal=8.dp))}
                    if(chosen.isNotEmpty())LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=8.dp)) {
                        items(chosen.toList(),key={it.spotifyTrackId}) { song ->
                            Row(Modifier.clip(CircleShape).background(RadioGreen.copy(alpha=0.12f)).radioTap(!saving){toggle(song)}.padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text(song.title,color=AppText,maxLines=1,fontSize=12.sp,overflow=TextOverflow.Ellipsis,modifier=Modifier.widthIn(max=150.dp));Text("  ×",color=RadioGreen)
                            }
                        }
                    }
                    if(error.isNotBlank())Text(error,color=AppSecondary,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))
                    Button(enabled=chosen.isNotEmpty()&&!saving,onClick={
                        keyboard?.hide();saving=true;error=""
                        scope.launch {
                            try{val marker=model.create(context,chosen.toList(),name,mix);onCreated(marker.spotifyTrackId)}catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(failed:Exception){error="Couldn't save the station. Try again."}finally{saving=false}
                        }
                    },modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(50.dp),shape=CircleShape,colors=ButtonDefaults.buttonColors(containerColor=RadioGreen,contentColor=Color.Black,disabledContainerColor=AppRaised,disabledContentColor=AppSecondary)) {
                        Text(if(saving)"Creating…" else if(mix)"Create Radio Mix" else "Create Radio",fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable private fun RadioSelectableSong(song:SongsModel,selected:Boolean,enabled:Boolean,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp).clip(RoundedCornerShape(10.dp)).background(if(selected)RadioGreen.copy(alpha=0.10f)else Color.Transparent).radioTap(enabled,onClick).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
        GlideImage(model=song.coverUri,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)))
        Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(song.title,color=if(selected)RadioGreen else AppText,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(song.singer,color=AppSecondary,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
        Box(Modifier.size(26.dp).clip(CircleShape).background(if(selected)RadioGreen else Color.Transparent).border(1.5.dp,if(selected)RadioGreen else AppSecondary,CircleShape),contentAlignment=Alignment.Center){if(selected)Text("✓",color=Color.Black,fontWeight=FontWeight.Bold)}
    }
}
@Composable private fun RadioLoading(){Box(Modifier.fillMaxWidth().padding(32.dp),contentAlignment=Alignment.Center){CircularProgressIndicator(color=RadioGreen)}}
@Composable private fun RadioMessage(message:String,retry:(()->Unit)?=null){Column(Modifier.padding(20.dp)){Text(message,color=AppSecondary,fontSize=14.sp);if(retry!=null)Text("Try again",color=RadioGreen,fontWeight=FontWeight.Bold,modifier=Modifier.radioTap(action=retry).padding(vertical=14.dp))}}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable fun SavedRadiosLibrary(navController:androidx.navigation.NavController,query:String) {
    val context=LocalContext.current
    var revision by remember{mutableStateOf(0)}
    var saved by remember{mutableStateOf(emptyList<SongsModel>())}
    DisposableEffect(context) {
        val prefs=context.getSharedPreferences("spotui_radio_sessions",0)
        val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener{_,_->revision++}
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    LaunchedEffect(revision){saved=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){com.music.spotui.data.preferences.RadioSessions.get(context)}}
    val visible=saved.filter{query.isBlank() || it.title.contains(query,true) || it.singer.contains(query,true)}
    val player:com.music.spotui.ui.viewmodel.PlayerViewModel=hiltViewModel()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=140.dp,top=12.dp)) {
        if(visible.isEmpty())item{Text("Your USpot radios appear here. Create one using + on Home.",color=AppSecondary)}
        items(visible,key={it.spotifyTrackId.ifBlank{it.id.toString()}}) { song ->
            val id=song.spotifyTrackId.ifBlank{song.id.toString()}
            val active=player.activeCollection.value?.key=="radio:$id"
            Row(Modifier.fillMaxWidth().padding(vertical=5.dp).playbackGlow(active).clickable{
                navController.navigate("radio/"+android.net.Uri.encode(id))
            }.padding(10.dp)) {
                GlideImage(model=song.coverUri,contentDescription=null,modifier=Modifier.size(60.dp))
                Column(Modifier.weight(1f).padding(start=14.dp)) {
                    Text(com.music.spotui.data.preferences.RadioSessions.stationName(song),color=if(active)AppPalette else AppText,fontWeight=FontWeight.SemiBold)
                    Text(if(com.music.spotui.data.preferences.RadioSessions.isMix(id))"USpot Radio Mix · ${song.singer}" else "USpot Radio · ${song.singer}",color=AppSecondary,fontSize=12.sp)
                }
                if(active)PlayingBars(player.currentSongPlayingState.value)
            }
        }
    }
}
