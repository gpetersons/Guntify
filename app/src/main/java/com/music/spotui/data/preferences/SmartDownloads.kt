package com.music.spotui.data.preferences

import android.content.Context

object SmartDownloads {
    private const val PREF = "smart_downloads"
    val revision = kotlinx.coroutines.flow.MutableStateFlow(0L)
    private fun prefs(c: Context) = c.getSharedPreferences(PREF, 0)
    fun enabled(c: Context) = prefs(c).getBoolean("enabled", false)
    fun count(c: Context) = prefs(c).getInt("count", 20).coerceIn(1, 100)
    fun wifiOnly(c: Context) = prefs(c).getBoolean("wifi", true)
    fun setEnabled(c: Context, value: Boolean) { prefs(c).edit().putBoolean("enabled", value).apply(); revision.value++ }
    fun setCount(c: Context, value: Int) { prefs(c).edit().putInt("count", value.coerceIn(1, 100)).apply(); revision.value++ }
    fun setWifi(c: Context, value: Boolean) { prefs(c).edit().putBoolean("wifi", value).apply(); revision.value++ }
    fun owned(c: Context): Set<String> = prefs(c).getStringSet("owned", emptySet()).orEmpty().toSet()
    fun pinned(c: Context): Set<String> = prefs(c).getStringSet("pinned", emptySet()).orEmpty().toSet()
    @Synchronized fun markManual(c: Context, id: String) {
        prefs(c).edit().putStringSet("owned", owned(c) - id).putStringSet("pinned", pinned(c) + id).apply()
    }
    @Synchronized fun claim(c: Context, id: String) {
        if (id !in pinned(c)) prefs(c).edit().putStringSet("owned", owned(c) + id).apply()
    }
    @Synchronized fun forget(c: Context, id: String) { prefs(c).edit().putStringSet("owned", owned(c) - id).apply() }
}
