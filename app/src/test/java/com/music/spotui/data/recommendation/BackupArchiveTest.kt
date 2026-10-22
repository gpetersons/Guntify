package com.music.spotui.data.recommendation

import com.music.spotui.data.backup.BackupArchive
import com.music.spotui.data.backup.BackupPreferencePolicy
import java.io.*
import java.nio.file.Files
import java.util.zip.*
import org.junit.Assert.*
import org.junit.Test

class BackupArchiveTest {
    private fun zip(vararg paths: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z -> paths.forEach { (name, text) -> z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray()); z.closeEntry() } }
    }.toByteArray()
    @Test fun metadataAndAudioRoundTripWithIdenticalHashes() {
        val root = Files.createTempDirectory("uspot-test").toFile()
        try {
            val audio = File(root, "song.flac").apply { writeBytes(ByteArray(200_000) { (it % 251).toByte() }) }
            val out = ByteArrayOutputStream()
            BackupArchive.write(out, "metadata".toByteArray(), mapOf("files/downloads/song.flac" to audio))
            val restored = BackupArchive.stage(ByteArrayInputStream(out.toByteArray()), File(root, "stage"), 300_000)
            assertEquals("metadata", restored.getValue("backup.json").readText())
            assertEquals(BackupArchive.hash(audio), BackupArchive.hash(restored.getValue("files/downloads/song.flac")))
        } finally { root.deleteRecursively() }
    }
    @Test fun unsafeAndUnknownPathsAreRejected() {
        listOf("../backup.json", "/backup.json", "files/downloads/../../secret", "files/downloads\\secret", "files/preferences.xml", "files/downloads//a").forEach {
            assertFalse(it, BackupArchive.safePath(it))
        }
    }
    @Test fun extractionRejectsTraversalAndRemovesStaging() {
        val root = Files.createTempDirectory("uspot-test").toFile()
        val stage = File(root, "stage")
        try {
            assertThrows(IllegalArgumentException::class.java) { BackupArchive.stage(ByteArrayInputStream(zip("../escape" to "bad")), stage, 1000) }
            assertFalse(File(root, "escape").exists()); assertFalse(stage.exists())
        } finally { root.deleteRecursively() }
    }
    @Test fun quotaUsesActualExpandedBytes() {
        val root = Files.createTempDirectory("uspot-test").toFile()
        try {
            val bytes = zip("backup.json" to "metadata", "files/downloads/a" to "a".repeat(10_000))
            assertThrows(IllegalArgumentException::class.java) { BackupArchive.stage(ByteArrayInputStream(bytes), File(root, "stage"), 100) }
            assertFalse(File(root, "stage").exists())
        } finally { root.deleteRecursively() }
    }
    @Test fun ordinaryZipIsNotAcceptedAsBackup() {
        val root = Files.createTempDirectory("uspot-test").toFile()
        try { assertThrows(IllegalArgumentException::class.java) { BackupArchive.stage(ByteArrayInputStream(zip("files/downloads/a" to "audio")), File(root, "stage"), 100) } }
        finally { root.deleteRecursively() }
    }
    @Test fun authDataCannotBeExportedOrRestored() {
        listOf("youtube_cookie", "youtube_login_version", "youtube_visitor_data", "password", "arl", "session_secret").forEach {
            assertFalse(it, BackupPreferencePolicy.allowed("settings_prefs", it))
        }
        assertFalse(BackupPreferencePolicy.allowed("spotify_session", "sp_dc"))
        assertFalse(BackupPreferencePolicy.allowed("SpotiflacSession", "session_id"))
        assertTrue(BackupPreferencePolicy.allowed("radio_progress", "seed:arlTokenExample"))
        assertTrue(BackupPreferencePolicy.allowed("ListeningHistory", "entries"))
        assertTrue(BackupPreferencePolicy.allowed("settings_prefs", "uspot_true_black"))
    }
}
