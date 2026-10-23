package com.music.spotui.updates

import java.net.URI
import org.json.JSONObject

data class AppRelease(val code: Long, val version: String, val tag: String, val page: String,
    val apk: String, val sha256: String, val bytes: Long, val notes: String)

/** Accept only a stable, versioned APK from our own repository. Never compare display versions. */
object UpdatePolicy {
    const val REPO = "gpetersons/Guntify"
    const val API = "https://api.github.com/repos/$REPO/releases/latest"
    const val MAX_APK_BYTES = 150L * 1024 * 1024
    fun parse(raw: String): AppRelease? {
        val j = JSONObject(raw)
        if (j.optBoolean("draft") || j.optBoolean("prerelease")) return null
        val tag = j.getString("tag_name")
        val match = Regex("^v?([0-9]+(?:\\.[0-9]+){1,3})-build-([0-9]+)$").matchEntire(tag)
            ?: error("The release does not contain a supported build number.")
        val code = match.groupValues[2].toLongOrNull()?.takeIf { it > 0 } ?: error("Invalid build number.")
        val page = "https://github.com/$REPO/releases/tag/$tag"
        require(j.getString("html_url") == page) { "Unexpected release repository." }
        val assets = j.getJSONArray("assets")
        val apks = (0 until assets.length()).map { assets.getJSONObject(it) }
            .filter { it.optString("name") == "Guntify.apk" && it.optString("state") == "uploaded" }
        require(apks.size == 1) { "The release APK is not ready yet." }
        val asset = apks.single()
        val apk = asset.getString("browser_download_url")
        require(apk == "https://github.com/$REPO/releases/download/$tag/Guntify.apk") { "Unexpected update download address." }
        val uri = URI(apk)
        require(uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null && uri.query == null && uri.fragment == null)
        val digest = Regex("^sha256:([a-fA-F0-9]{64})$").matchEntire(asset.optString("digest"))
            ?.groupValues?.get(1)?.lowercase() ?: error("The release APK has no SHA-256 checksum yet.")
        val bytes = asset.getLong("size")
        require(bytes in 1..MAX_APK_BYTES) { "Invalid APK download size." }
        return AppRelease(code, match.groupValues[1], tag, page, apk, digest, bytes, j.optString("body").take(8000))
    }
    fun compatible(installedPackage: String, downloadedPackage: String?, installedCode: Long, downloadedCode: Long,
        expectedCode: Long, installedSigners: Set<String>, downloadedSigners: Set<String>): Boolean =
        installedPackage == downloadedPackage && downloadedCode == expectedCode && downloadedCode > installedCode &&
            installedSigners.isNotEmpty() && installedSigners == downloadedSigners
}
