package com.music.spotui

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.music.spotui.ui.navigation.MainBottomNavigation
import com.music.spotui.ui.navigation.MyNavHost
import com.music.spotui.ui.navigation.Routes


@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun App() {
    val queueSnackbar = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.flow.merge(
            com.music.spotui.ui.components.QueueFeedback.messages.map { com.music.spotui.ui.components.ActionFeedback.Notice(it) },
            com.music.spotui.ui.components.PlaybackFeedback.messages.map { com.music.spotui.ui.components.ActionFeedback.Notice(it) },
            com.music.spotui.ui.components.ActionFeedback.messages
        ).collectLatest { notice ->
            val result=queueSnackbar.showSnackbar(notice.text,actionLabel=if(notice.undo!=null)"Undo"else null,
                duration=if(notice.undo!=null)androidx.compose.material3.SnackbarDuration.Long else androidx.compose.material3.SnackbarDuration.Short)
            if(result==androidx.compose.material3.SnackbarResult.ActionPerformed)notice.undo?.invoke()
        }
    }
    val bottomBarState = rememberSaveable { (mutableStateOf(true)) }
    val bottomBarPlayerState = rememberSaveable { (mutableStateOf(true)) }
    val overlayHeight = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val overlayInset = with(androidx.compose.ui.platform.LocalDensity.current) { if (overlayHeight.intValue > 0) overlayHeight.intValue.toDp() else 164.dp }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // Control TopBar and BottomBar
    when (navBackStackEntry?.destination?.route) {
        Routes.Home.route -> {
            bottomBarState.value = true
        }
        Routes.Search.route -> {
            bottomBarState.value = true
        }
        Routes.Library.route -> {
            bottomBarState.value = true
        }
        Routes.Album.route -> {
            bottomBarState.value = false
        }
    }

    Scaffold(
        modifier = Modifier,
        snackbarHost = {
            androidx.compose.material3.SnackbarHost(queueSnackbar) { data ->
                androidx.compose.material3.Snackbar(data,
                    containerColor = com.music.spotui.ui.theme.AppRaised,
                    actionColor = com.music.spotui.ui.theme.AppPalette,
                    contentColor = com.music.spotui.ui.theme.AppText,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
        bottomBar = {
            MainBottomNavigation(navController = navController, bottomBarState = bottomBarState, bottomBarPlayerState, onHeightChanged = { if (overlayHeight.intValue != it) overlayHeight.intValue = it })
        }
    ) {
        androidx.compose.runtime.CompositionLocalProvider(com.music.spotui.ui.navigation.LocalBottomOverlayInset provides (if (bottomBarState.value) overlayInset else androidx.compose.ui.unit.Dp(0f))) {
            MyNavHost(navHostController = navController, bottomBarState = bottomBarState, bottomBarPlayerState)
        }
    }
}


