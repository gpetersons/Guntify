package com.music.spotui.data.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Base64
import com.bumptech.glide.Glide
import com.music.spotui.data.entity.SongsModel
import java.io.File
import java.io.ByteArrayOutputStream

/** Deterministic artwork collage, only for playlists created through the Library plus button. */
object PlaylistCover {
    fun localPath(context: Context, playlistId: String): String? {
        val path = context.getSharedPreferences("playlist_covers", 0).getString(playlistId, null) ?: return null
        return path.takeIf { File(it).isFile }
    }
    fun generate(context: Context, playlistId: String, songs: List<SongsModel>): String? {
        if (songs.isEmpty()) return null
        val images = songs.take(4).mapNotNull { song ->
            if (song.coverUri.isBlank()) return@mapNotNull null
            val request = Glide.with(context).asBitmap().load(song.coverUri).submit(256, 256)
            try { request.get(10, java.util.concurrent.TimeUnit.SECONDS).copy(Bitmap.Config.ARGB_8888, false) }
            catch (_: Exception) { null }
            finally { Glide.with(context).clear(request) }
        }
        if (images.isEmpty()) return null
        val output = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        repeat(4) { i ->
            val bitmap = images[i % images.size]
            val side = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - side) / 2
            val y = (bitmap.height - side) / 2
            canvas.drawBitmap(bitmap, Rect(x, y, x + side, y + side), Rect((i % 2) * 256, (i / 2) * 256, (i % 2 + 1) * 256, (i / 2 + 1) * 256), paint)
        }
        val dir = File(context.filesDir, "playlist-covers").apply { mkdirs() }
        val coverKey = songs.take(4).joinToString("|") { it.spotifyTrackId.ifBlank { it.id.toString() } }.hashCode()
        val file = File(dir, "$playlistId-$coverKey.jpg")
        file.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        output.recycle()
        images.forEach { it.recycle() }
        context.getSharedPreferences("playlist_covers", 0).edit().putString(playlistId, file.absolutePath).apply()
        return file.absolutePath
    }
    suspend fun upload(playlistId: String, path: String): Boolean {
        val bytes = File(path).readBytes()
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (encoded.length > 256 * 1024) return false
        return com.metrolist.spotify.Spotify.uploadPlaylistCover(playlistId, encoded).isSuccess
    }
}
