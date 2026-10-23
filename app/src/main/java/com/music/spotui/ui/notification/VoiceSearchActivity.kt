package com.music.spotui.ui.notification

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore

/** Legacy Assistant entry point; all searching and playback belongs to the media service. */
class VoiceSearchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val request = Intent(this, PlaybackService::class.java).setAction(VoiceSearchPolicy.PLAY_ACTION)
            listOf(SearchManager.QUERY, MediaStore.EXTRA_MEDIA_TITLE, MediaStore.EXTRA_MEDIA_ARTIST, MediaStore.EXTRA_MEDIA_ALBUM).forEach { key ->
                intent.getStringExtra(key)?.let { request.putExtra(key, it.take(200)) }
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(request) else startService(request)
        } catch (error: Exception) {
            android.widget.Toast.makeText(this, "Open Sprotify and try the voice request again", android.widget.Toast.LENGTH_SHORT).show()
        } finally { finish() }
    }
}
