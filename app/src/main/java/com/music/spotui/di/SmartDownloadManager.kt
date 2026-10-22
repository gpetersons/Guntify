package com.music.spotui.di

import android.content.Context
import com.music.spotui.data.preferences.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect

/** Refreshes the automatic set while the process is available; rechecks on each launch. */
object SmartDownloadManager {
    private var job: Job? = null
    private val pending = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val retryAfter = java.util.concurrent.ConcurrentHashMap<String, Long>()
    val status = kotlinx.coroutines.flow.MutableStateFlow("Waiting for listening history")
    fun start(context: Context) {
        if (job?.isActive == true) return
        val ctx = context.applicationContext
        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Recheck connectivity periodically without a new scheduler or playback engine.
            val timer = kotlinx.coroutines.flow.MutableStateFlow(0L)
            launch { while (isActive) { delay(60_000); timer.value++ } }
            combine(ListeningHistorySignals.revision, SmartDownloads.revision,
                com.music.spotui.ui.viewmodel.BackupSignals.revision, timer) { _, _, _, _ -> Unit }.collect {
                delay(500)
                runCatching { refresh(ctx) }.onFailure { android.util.Log.w("SmartDownloads", "Automatic refresh deferred", it) }
            }
        }
    }
    private fun refresh(ctx: Context) {
        val target = if (SmartDownloads.enabled(ctx)) com.music.spotui.data.recommendation.ListeningTastePolicy.topSongs(
            getListeningHistory(ctx), System.currentTimeMillis(), SmartDownloads.count(ctx), getLikedSongIds(ctx))
            .filterNot { it.url.startsWith("content://") || it.url.startsWith("file://") } else emptyList()
        val ids = target.map { it.id.toString() }.toSet()
        pending.entries.filter { it.key !in ids }.forEach { (id, query) ->
            if (id !in SmartDownloads.pinned(ctx)) Downloads.cancelSong(query)
            pending.remove(id)
        }
        // Disabling stops automation and leaves existing files in place.
        if (!SmartDownloads.enabled(ctx)) { status.value = "Off · Existing downloads kept"; return }
        // Keep the previous offline set until all its replacements are available.
        val replacementsReady = target.isNotEmpty() && target.all { downloadedPathForQuery(ctx, it.url) != null }
        SmartDownloads.owned(ctx).filter { replacementsReady && it !in ids && it !in SmartDownloads.pinned(ctx) }.forEach { id ->
            if (getDownloadedSongs(ctx).none { it.id.toString() == id && it.id == currentSongId() } &&
                Downloads.downloadingSnapshot().none { it.first.id.toString() == id }) {
                removeDownload(ctx, id); SmartDownloads.forget(ctx, id)
            }
        }
        val connectivity = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        @Suppress("DEPRECATION") val connected = connectivity.activeNetworkInfo?.isConnected == true
        if (!connected || (SmartDownloads.wifiOnly(ctx) && connectivity.isActiveNetworkMetered)) {
            status.value = "Waiting for an allowed network"; return
        }
        if (ctx.filesDir.usableSpace < 256L * 1024 * 1024) { status.value = "Paused · Low storage"; return }
        status.value = if (target.isEmpty()) "Listen to songs to build your automatic set" else
            "${target.count { downloadedPathForQuery(ctx, it.url) != null }} of ${target.size} favorite songs saved"
        if (SongPlayer.isResolvingAudio() || pending.values.any { Downloads.isDownloading(it) }) return
        for (song in target) {
            val id = song.id.toString()
            if (downloadedPathForQuery(ctx, song.url) != null || Downloads.isDownloading(song.url)) continue
            if ((retryAfter[id] ?: 0L) > System.currentTimeMillis()) continue
            if (isDownloaded(ctx, id)) removeDownload(ctx, id) // Repair stale metadata for a missing file.
            pending[id] = song.url
            SmartDownloads.claim(ctx, id)
            Downloads.downloadSong(song, ctx, automatic = true) { success ->
                pending.remove(id)
                if (!success) retryAfter[id] = System.currentTimeMillis() + 15 * 60_000L
                SmartDownloads.revision.value++
            }
            break // One automatic transfer at a time; manual download behavior is unchanged.
        }
        Downloads.onDownloadsChanged?.invoke()
    }
    private fun currentSongId(): Int? = SongPlayer.activeSongId()
}
