package com.music.spotui.data.backup

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Streams only known backup paths into staging; never extracts directly into live app data. */
object BackupArchive {
    fun safePath(name: String): Boolean {
        if (name.contains('\\') || name.startsWith('/') || name.split('/').any { it.isBlank() || it == "." || it == ".." }) return false
        return name == "backup.json" || name.startsWith("files/downloads/") || name.startsWith("files/playlist-covers/")
    }
    fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = stream.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    fun write(output: OutputStream, manifest: ByteArray, files: Map<String, File>) {
        require(manifest.size <= 16 * 1024 * 1024) { "Backup metadata is too large." }
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json")); zip.write(manifest); zip.closeEntry()
            files.forEach { (name, file) ->
                require(safePath(name) && name != "backup.json")
                zip.putNextEntry(ZipEntry(name)); file.inputStream().use { it.copyTo(zip, 64 * 1024) }; zip.closeEntry()
            }
        }
    }
    fun stage(input: InputStream, root: File, maxBytes: Long): Map<String, File> {
        val files = linkedMapOf<String, File>()
        var total = 0L
        try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(!entry.isDirectory && safePath(entry.name)) { "Unsupported or unsafe backup path." }
                    require(entry.name !in files && files.size < 20_000) { "Duplicate or excessive backup entries." }
                    val file = File(root, entry.name)
                    require(file.canonicalPath.startsWith(root.canonicalPath + File.separator)) { "Unsafe backup path." }
                    file.parentFile?.mkdirs()
                    var size = 0L
                    file.outputStream().use { out ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = zip.read(buffer); if (count < 0) break
                            total += count; size += count
                            require(total <= maxBytes && (entry.name != "backup.json" || size <= 16 * 1024 * 1024)) { "Backup exceeds available space or metadata limit." }
                            out.write(buffer, 0, count)
                        }
                    }
                    files[entry.name] = file; zip.closeEntry()
                }
            }
            require("backup.json" in files) { "This is not a USpot backup." }
            return files
        } catch (error: Throwable) {
            root.deleteRecursively(); throw error
        }
    }
}

object BackupPreferencePolicy {
    val stores = setOf("radio_skip_memory", "smart_downloads", "radio_taste", "ListeningHistory", "LikedSongs", "LikedSongsFromSpotify", "LikedAlbums", "FollowedArtists", "RecentItems", "Downloads", "LocalLibrary", "AlternativeStreams", "LibraryCache", "created_playlists", "library_organization", "download_failures", "music_source", "settings_prefs", "home_personalization", "home_hidden_items", "spotui_modified_home_recents", "spotui_home_frequency_v1", "spotui_radio_sessions", "radio_progress", "PlaybackState", "playlist_covers", "playback_equalizer", "Deezer", "Soulseek")
    fun allowed(store: String, key: String): Boolean {
        if (store !in stores) return false
        // Random track/Radio IDs are data keys, not credential field names.
        if (store !in setOf("settings_prefs", "Deezer", "Soulseek")) return true
        val name = key.lowercase(java.util.Locale.ROOT)
        return listOf("cookie", "token", "password", "secret", "sp_dc", "arl", "visitor_data", "data_sync_id", "login", "username").none { name.contains(it) }
    }
}
