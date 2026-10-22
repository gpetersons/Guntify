package com.music.spotui.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.music.spotui.data.preferences.isSongLiked

/** A heart belongs to one song; preference changes update all visible copies. */
@Composable
fun rememberSongLiked(context:Context,songId:String):MutableState<Boolean> {
    val app=context.applicationContext
    val state=remember(app,songId){mutableStateOf(isSongLiked(app,songId))}
    DisposableEffect(app,songId) {
        val prefs=app.getSharedPreferences("LikedSongs",Context.MODE_PRIVATE)
        var disposed=false
        val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,key ->
            if(!disposed && (key==null || key==songId))state.value=isSongLiked(app,songId)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // Close the read/register gap, including imports or a completed Spotify fetch.
        state.value=isSongLiked(app,songId)
        onDispose{disposed=true;prefs.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    return state
}
