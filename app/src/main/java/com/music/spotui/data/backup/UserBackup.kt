package com.music.spotui.data.backup

import android.content.Context
import android.net.Uri
import com.music.spotui.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Local SAF backup. No network calls, authentication cookies, or private signing keys. */
object UserBackup {
    private fun encode(value: Any): JSONObject = JSONObject().apply {
        when (value) {
            is String -> { put("type", "string"); put("value", value) }
            is Boolean -> { put("type", "boolean"); put("value", value) }
            is Int -> { put("type", "int"); put("value", value) }
            is Long -> { put("type", "long"); put("value", value.toString()) }
            is Float -> { require(value.isFinite()); put("type", "float"); put("value", value.toString()) }
            is Set<*> -> { require(value.all { it is String }); put("type", "set"); put("value", JSONArray(value.toList())) }
            else -> error("Unsupported preference value.")
        }
    }
    private fun decode(value: JSONObject): Any = when (value.getString("type")) {
        "string" -> value.getString("value")
        "boolean" -> value.getBoolean("value")
        "int" -> value.getInt("value")
        "long" -> value.getString("value").toLong()
        "float" -> value.getString("value").toFloat().also { require(it.isFinite()) }
        "set" -> value.getJSONArray("value").let { array -> (0 until array.length()).map { array.getString(it) }.toSet() }
        else -> error("Unsupported backup preference type.")
    }
    fun saveBackup(context: Context, uri: Uri, includeAudio: Boolean): Int {
        val prefs = JSONObject()
        BackupPreferencePolicy.stores.forEach { store ->
            val data = JSONObject()
            context.getSharedPreferences(store, 0).all.forEach { (key, value) ->
                if (value != null && BackupPreferencePolicy.allowed(store, key)) data.put(key, encode(value))
            }
            prefs.put(store, data)
        }
        val files = linkedMapOf<String, File>()
        listOfNotNull("playlist-covers", "downloads".takeIf { includeAudio }).forEach { folder ->
            File(context.filesDir, folder).walkTopDown().filter { it.isFile }.forEach { file ->
                val relative = file.relativeTo(context.filesDir).invariantSeparatorsPath
                if (file.canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator)) files["files/$relative"] = file
            }
        }
        val hashes = JSONObject()
        files.forEach { (name, file) -> hashes.put(name, JSONObject().put("bytes", file.length()).put("sha256", BackupArchive.hash(file))) }
        val manifest = JSONObject().put("format", "USpotBackup").put("schema", 1).put("appId", BuildConfig.APPLICATION_ID)
            .put("version", BuildConfig.VERSION_NAME).put("createdAt", System.currentTimeMillis()).put("originalFilesDir", context.filesDir.absolutePath)
            .put("includeAudio", includeAudio).put("preferences", prefs).put("files", hashes)
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot write this backup location.")
        BackupArchive.write(stream, manifest.toString().toByteArray(Charsets.UTF_8), files)
        return files.size
    }
    fun restore(context: Context, uri: Uri) {
        val stage = File(context.cacheDir, "backup-stage-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        val rollback = File(stage, "rollback")
        val oldPrefs = linkedMapOf<String, Map<String, *>>()
        val moved = mutableListOf<Pair<File, File?>>()
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot read this backup.")
            val files = BackupArchive.stage(stream, stage, (context.cacheDir.usableSpace - 32L * 1024 * 1024).coerceAtLeast(0L))
            val manifest = JSONObject(files.getValue("backup.json").readText())
            require(manifest.getString("format") == "USpotBackup" && manifest.getInt("schema") == 1) { "Unsupported backup format." }
            require(BackupIdentityPolicy.compatible(manifest.getString("appId"), BuildConfig.APPLICATION_ID)) { "This backup belongs to another app." }
            val declared = manifest.getJSONObject("files")
            require(files.keys - "backup.json" == declared.keys().asSequence().toSet()) { "Backup file list does not match." }
            declared.keys().forEach { name ->
                val data = declared.getJSONObject(name); val file = files.getValue(name)
                require(file.length() == data.getLong("bytes") && BackupArchive.hash(file) == data.getString("sha256")) { "A backup file is damaged." }
            }
            val incoming = linkedMapOf<String, Map<String, Any>>()
            val prefs = manifest.getJSONObject("preferences")
            val originalDir = manifest.getString("originalFilesDir")
            require(originalDir.startsWith("/") && originalDir.length > 1)
            prefs.keys().forEach { store ->
                require(store in BackupPreferencePolicy.stores) { "Unsupported preference group." }
                val json = prefs.getJSONObject(store)
                incoming[store] = json.keys().asSequence().filter { BackupPreferencePolicy.allowed(store, it) }.associateWith { key ->
                    val raw = decode(json.getJSONObject(key))
                    if (raw is String) raw.replace(originalDir + "/", context.filesDir.absolutePath + "/") else raw
                }
            }
            require("ListeningHistory" in incoming && "settings_prefs" in incoming) { "Backup is incomplete." }
            // Validation finishes before touching live preferences/files.
            files.filterKeys { it != "backup.json" }.forEach { (name, staged) ->
                val target = File(context.filesDir, name.removePrefix("files/"))
                require(target.canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator))
                val saved = if (target.exists()) File(rollback, name).apply { parentFile?.mkdirs(); check(target.renameTo(this)) { "Cannot prepare rollback." } } else null
                moved += target to saved
                target.parentFile?.mkdirs()
                check(staged.renameTo(target)) { "Cannot restore backup file." }
            }
            incoming.forEach { (store, values) ->
                val p = context.getSharedPreferences(store, 0)
                oldPrefs[store] = p.all.toMap()
                val editor = p.edit()
                p.all.keys.filter { BackupPreferencePolicy.allowed(store, it) }.forEach { editor.remove(it) }
                values.forEach { (key, value) ->
                    // Do not advertise missing downloaded audio if it was omitted from the archive.
                    if (store == "Downloads" && value is String && !File(JSONObject(value).optString("filePath")).isFile) return@forEach
                    put(editor, key, value)
                }
                if (store == "PlaybackState") editor.putBoolean("playing", false)
                check(editor.commit()) { "Cannot restore preferences." }
            }
        } catch (error: Throwable) {
            oldPrefs.forEach { (store, values) ->
                val editor = context.getSharedPreferences(store, 0).edit().clear()
                values.forEach { (key, value) -> if (value != null) put(editor, key, value) }
                editor.commit()
            }
            moved.asReversed().forEach { (target, saved) -> target.delete(); if (saved != null) saved.renameTo(target) }
            throw error
        } finally { stage.deleteRecursively() }
    }
    private fun put(editor: android.content.SharedPreferences.Editor, key: String, value: Any) {
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            else -> error("Unsupported preference type.")
        }
    }
}
