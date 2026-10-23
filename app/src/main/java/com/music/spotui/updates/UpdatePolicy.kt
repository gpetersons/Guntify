package com.music.spotui.updates

import java.net.URI
import org.json.JSONObject
import org.json.JSONArray

data class AppRelease(val code: Long, val version: String, val tag: String, val page: String,
    val apk: String, val sha256: String, val bytes: Long, val notes: String)

/** Accept only a stable, versioned APK from our own repository. Never compare display versions. */
object UpdatePolicy {
    const val REPO = "gpetersons/Guntify"
    const val API = "https://api.github.com/repos/$REPO/releases?per_page=20"
    const val MAX_APK_BYTES = 150L * 1024 * 1024
    /** The /releases/latest endpoint excludes pre-releases, so use published releases. */
    fun parse(raw: String): AppRelease? {
        if (!raw.trimStart().startsWith("[")) return parseOne(JSONObject(raw))
        val releases = JSONArray(raw)
        return (0 until releases.length()).mapNotNull { index ->
            runCatching { parseOne(releases.getJSONObject(index)) }.getOrNull()
        }.maxByOrNull { it.code }
    }

    private fun parseOne(j: JSONObject): AppRelease? {
        if (j.optBoolean("draft")) return null
        val tag = j.getString("tag_name")
        val beta = Regex("^v?([0-9]+(?:\\.[0-9]+){1,3})-beta\\.([0-9]+)$").matchEntire(tag)
        val stable = Regex("^v?([0-9]+(?:\\.[0-9]+){1,3})-build-([0-9]+)$").matchEntire(tag)
        val match = beta ?: stable ?: error("The release does not contain a supported build number.")
        if (j.optBoolean("prerelease") != (beta != null)) return null
        val code = match.groupValues[2].toLongOrNull()?.takeIf { it > 0 }
            ?: error("Invalid build number.")
        val version = if (beta != null) match.groupValues[1] + "-beta." + match.groupValues[2]
            else match.groupValues[1]
        val page = "https://github.com/$REPO/releases/tag/$tag"
        require(j.getString("html_url") == page) { "Unexpected release repository." }
        val assets = j.getJSONArray("assets")
        val apks = (0 until assets.length()).map { assets.getJSONObject(it) }
            .filter { it.optString("name") in setOf("Guntify.apk", "SprotiFy.apk") &&
                it.optString("state") == "uploaded" }
        require(apks.size == 1) { "The release APK is not ready yet." }
        val asset = apks.single()
        val name = asset.getString("name")
        val apk = asset.getString("browser_download_url")
        require(apk == "https://github.com/$REPO/releases/download/$tag/$name") {
            "Unexpected update download address."
        }
        val uri = URI(apk)
        require(uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null &&
            uri.query == null && uri.fragment == null)
        val digest = Regex("^sha256:([a-fA-F0-9]{64})$").matchEntire(asset.optString("digest"))
            ?.groupValues?.get(1)?.lowercase() ?: error("The release APK has no SHA-256 checksum yet.")
        val bytes = asset.getLong("size")
        require(bytes in 1..MAX_APK_BYTES) { "Invalid APK download size." }
        return AppRelease(code, version, tag, page, apk, digest, bytes, j.optString("body").take(8000))
    }

    fun compatible(installedPackage: String, downloadedPackage: String?, installedCode: Long, downloadedCode: Long,
        expectedCode: Long, installedSigners: Set<String>, downloadedSigners: Set<String>): Boolean =
        installedPackage == downloadedPackage && downloadedCode == expectedCode && downloadedCode > installedCode &&
            installedSigners.isNotEmpty() && installedSigners == downloadedSigners
}
