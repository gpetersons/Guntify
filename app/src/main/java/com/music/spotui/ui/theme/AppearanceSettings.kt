package com.music.spotui.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object AppearanceSettings {
    val followDevice = mutableStateOf(true)
    val darkMode = mutableStateOf(true)
    val artworkScreen = mutableStateOf(false)
    val deviceDark = mutableStateOf(true)
    val effectiveDark: Boolean get() = ThemeChoicePolicy.dark(followDevice.value, darkMode.value, deviceDark.value)
    private var loaded = false
    fun initialize(context: Context) { if (!loaded) reload(context) }
    fun reload(context: Context) {
        val prefs = context.getSharedPreferences("settings_prefs", 0)
        followDevice.value = prefs.getBoolean("uspot_follow_device", !prefs.getBoolean("uspot_true_black", false))
        darkMode.value = prefs.getBoolean("uspot_dark_mode", true)
        loaded = true
    }
    fun setFollowDevice(context: Context, enabled: Boolean) {
        followDevice.value = enabled
        context.getSharedPreferences("settings_prefs", 0).edit().putBoolean("uspot_follow_device", enabled).apply()
    }
    fun setDarkMode(context: Context, enabled: Boolean) {
        if (followDevice.value) return
        darkMode.value = enabled
        context.getSharedPreferences("settings_prefs", 0).edit().putBoolean("uspot_dark_mode", enabled).apply()
    }
}
