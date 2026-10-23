package com.music.spotui.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaRouter
import android.media.MediaRouter2
import android.media.MediaRoute2Info
import android.media.RouteDiscoveryPreference
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.ui.theme.*
import kotlinx.coroutines.delay

/** Selected routes, not merely connected devices. No Bluetooth scan or microphone access. */
data class AudioOutput(val id: String, val name: String, val selected: Boolean, val bluetooth: Boolean)

class AudioOutputs(context: Context) {
    private val context = context.applicationContext
    private val legacy = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter
    private val modern: MediaRouter2? = if(Build.VERSION.SDK_INT >= 30) MediaRouter2.getInstance(this.context) else null
    private val callback: MediaRouter2.RouteCallback? = if(Build.VERSION.SDK_INT >= 30) object: MediaRouter2.RouteCallback() {} else null
    fun start() { if(Build.VERSION.SDK_INT >= 30) runCatching {
        modern?.registerRouteCallback(context.mainExecutor, callback!!, RouteDiscoveryPreference.Builder(listOf(MediaRoute2Info.FEATURE_LIVE_AUDIO),false).build())
    } }
    fun close() { if(Build.VERSION.SDK_INT >= 30) runCatching { modern?.unregisterRouteCallback(callback!!) } }
    fun outputs(): List<AudioOutput> = runCatching {
        if(Build.VERSION.SDK_INT >= 30 && modern != null) {
            val selected = modern.systemController.selectedRoutes
            val selectedIds = selected.map { it.id }.toSet()
            (selected + modern.routes).filter { it.isSystemRoute }.distinctBy { it.id }.map { route ->
                AudioOutput(route.id,route.name.toString().ifBlank { "Phone speaker" },route.id in selectedIds,
                    route.type in listOf(MediaRoute2Info.TYPE_BLUETOOTH_A2DP,MediaRoute2Info.TYPE_HEARING_AID,26,27))
            }.takeIf { it.isNotEmpty() } ?: legacyOutputs()
        } else legacyOutputs()
    }.getOrElse { runCatching { legacyOutputs() }.getOrDefault(emptyList()) }
    @Suppress("DEPRECATION") private fun legacyOutputs(): List<AudioOutput> {
        val selected = legacy.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
        return (0 until legacy.routeCount).map { i -> legacy.getRouteAt(i) }.filter { it.supportedTypes and MediaRouter.ROUTE_TYPE_LIVE_AUDIO != 0 }
            .map { route -> AudioOutput("legacy:${route.hashCode()}",route.name.toString().ifBlank { "Phone speaker" },route == selected,route.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH) }
    }
    fun select(id: String): Boolean = runCatching {
        if(Build.VERSION.SDK_INT >= 30 && modern != null && !id.startsWith("legacy:")) {
            modern.routes.firstOrNull { it.id == id }?.let { modern.transferTo(it); return true }
            if(Build.VERSION.SDK_INT >= 34 && modern.showSystemOutputSwitcher()) return true
            return false
        }
        for(i in 0 until legacy.routeCount) {
            val route = legacy.getRouteAt(i)
            if(id == "legacy:${route.hashCode()}") { legacy.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO,route); return true }
        }
        false
    }.getOrDefault(false)
    fun systemPicker(): Boolean = Build.VERSION.SDK_INT >= 34 && runCatching { modern?.showSystemOutputSwitcher() == true }.getOrDefault(false)
    fun moreDevices(): Boolean = runCatching {
        if(systemPicker()) return true
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}

@Composable
fun rememberAudioOutputs(context: Context): Pair<AudioOutputs,List<AudioOutput>> {
    val router = remember(context.applicationContext) { AudioOutputs(context) }
    var outputs by remember { mutableStateOf(emptyList<AudioOutput>()) }
    DisposableEffect(router) { router.start(); onDispose { router.close() } }
    LaunchedEffect(router) { while(true) { outputs=router.outputs();delay(1000) } }
    return router to outputs
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputSheet(router: AudioOutputs, outputs: List<AudioOutput>, onDismiss: () -> Unit) {
    var message by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=AppSurface,contentColor=AppText,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
            Text("Where do you want to listen?",color=AppText,fontSize=24.sp,fontWeight=FontWeight.Bold)
            Text("Choose a connected speaker or headphones.",color=AppSecondary,modifier=Modifier.padding(vertical=12.dp))
            LazyColumn(Modifier.heightIn(max=300.dp)) {
                items(outputs,key={it.id}) { output ->
                    Row(Modifier.fillMaxWidth().clickable {
                        if(!output.selected && !router.select(output.id)) message="Open device controls to choose this output."
                    }.padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(output.name,color=if(output.selected)AppPalette else AppText,fontWeight=FontWeight.SemiBold)
                            Text(if(output.selected)"Playing here" else if(output.bluetooth)"Bluetooth" else "Audio output",color=AppSecondary,fontSize=12.sp)
                        }
                        if(output.selected) Text("✓",color=AppPalette,fontSize=24.sp)
                    }
                }
            }
            if(message.isNotBlank())Text(message,color=AppSecondary)
            TextButton(onClick={if(!router.moreDevices())message="Device controls are unavailable on this phone."}){Text("Open device controls",color=AppPalette)}
            Text("Pair new headphones in your phone's Bluetooth settings. Available outputs depend on your phone.",color=AppSecondary,fontSize=12.sp,modifier=Modifier.padding(bottom=16.dp))
        }
    }
}
