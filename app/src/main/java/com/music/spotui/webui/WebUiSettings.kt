package com.music.spotui.webui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.music.spotui.ui.theme.*
import kotlinx.coroutines.delay

@Composable
internal fun WebUiSettings() {
    val context = LocalContext.current
    val state by WebUiController.state.collectAsState()
    var wifi by remember { mutableStateOf(WebUiController.wifiAddress(context)) }
    var showStart by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while(true) { wifi=WebUiController.wifiAddress(context); delay(3000) } }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { WebUiController.start(context) }
    fun start() {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else WebUiController.start(context)
    }
    fun batterySettings() {
        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}"))) }
    }
    Text("Guntify on your computer",color=AppText,fontSize=25.sp,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Text("Play through your computer's speakers using the Spotify account and audio sources already connected on this phone.",color=AppSecondary)
    Spacer(Modifier.height(20.dp))
    Card(colors=CardDefaults.cardColors(containerColor=AppSurface),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(state.running)"desktop mode is running" else "desktop mode is off",color=if(state.running)AppPalette else AppText,fontWeight=FontWeight.Bold,fontSize=19.sp)
            if(state.running) {
                Text("Type this address on your computer",color=AppSecondary)
                SelectionContainer { Text(state.link.substringBefore('#'),color=AppText,fontSize=20.sp) }
                Text("The browser connects automatically while desktop mode is running.",color=AppSecondary)
                TextButton(onClick={
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Guntify desktop mode",state.link))
                    copied=true
                }) { Text(if(copied)"Address copied" else "Copy address",color=AppPalette) }
                OutlinedButton(onClick={WebUiController.stop(context);copied=false}) { Text("Stop desktop mode",color=AppText) }
            } else {
                Text(if(wifi==null)"Connect this phone to Wi-Fi to begin." else "Wi-Fi is connected. Ready when you are.",color=AppSecondary)
                Button(onClick={showStart=true},enabled=wifi!=null,colors=ButtonDefaults.buttonColors(containerColor=AppPalette)) { Text("Start desktop mode",color=androidx.compose.ui.graphics.Color.Black) }
            }
            if(state.error.isNotBlank())Text(state.error,color=AppSecondary)
        }
    }
    Spacer(Modifier.height(20.dp))
    Text("How to connect",color=AppText,fontSize=19.sp,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Text("1. Connect your phone and computer to the same home Wi-Fi/router. A computer connected by Ethernet to that router also works.\n\n2. Start desktop mode, then type the complete http:// address above into the computer's browser.\n\n3. Choose a song and press Play. Sound comes from the computer, not the phone. Keep the browser tab open.\n\n4. Keep the phone on Wi-Fi. Locking its screen is fine; force-stopping Guntify or changing Wi-Fi will disconnect the browser.",color=AppSecondary)
    Spacer(Modifier.height(18.dp))
    Text("Keep the connection alive",color=AppText,fontSize=19.sp,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Guntify shows a running notification while serving the browser. If your phone stops the connection with its screen off, open app settings → Battery and choose Unrestricted / Don't optimize. Names vary by phone. This is optional, uses more battery, and can't prevent Android or the manufacturer from stopping the app. Plug in for long sessions.",color=AppSecondary)
    TextButton(onClick={batterySettings()}) { Text("Open app & battery settings",color=AppPalette) }
    Spacer(Modifier.height(10.dp))
    Text("Connection tips",color=AppText,fontWeight=FontWeight.Bold)
    Text("Use http:// rather than https://. Guest Wi-Fi or router client isolation may block devices from seeing each other. A VPN may also interfere. If Wi-Fi reconnects, restart desktop mode and use the new address. Some audio formats depend on browser support.",color=AppSecondary)
    Spacer(Modifier.height(12.dp))
    Text("Anyone on this Wi-Fi can connect while desktop mode is running, browse this account's music and play it. This local HTTP connection is for trusted Wi-Fi only; don't forward its port or expose it to the internet. Stopping desktop mode disconnects the session and invalidates browser sessions.",color=AppSecondary,fontSize=12.sp)
    Spacer(Modifier.height(130.dp))
    if(showStart) AlertDialog(onDismissRequest={showStart=false},containerColor=AppSurface,
        title={Text("Start your desktop player?",color=AppText)},
        text={Text("Keep this phone on Wi-Fi while you listen on your computer. A running notification and temporary Wi-Fi/CPU locks help keep the connection available. For long sessions, you can optionally set Guntify's battery usage to Unrestricted in app settings.",color=AppSecondary)},
        confirmButton={TextButton(onClick={showStart=false;start()}){Text("Start desktop mode",color=AppPalette)}},
        dismissButton={TextButton(onClick={showStart=false}){Text("Not now",color=AppSecondary)}})
}
