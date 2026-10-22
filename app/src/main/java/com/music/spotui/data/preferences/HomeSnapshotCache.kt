package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.api.SpotifySession
import com.music.spotui.data.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Last successful catalog results, kept locally across process death and updates.
 * No credentials are serialized. A session change discards the snapshot.
 */
object HomeSnapshotCache {
    private const val PREF = "home_snapshot"
    private fun fingerprint(session: String): String = session.takeIf { it.isNotBlank() }
        ?.let { cookie -> MessageDigest.getInstance("SHA-256").digest(cookie.toByteArray()).joinToString("") { "%02x".format(it) } }.orEmpty()
    private fun owner(context: Context) = fingerprint(SpotifySession.spDc(context))
    private fun store(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    fun clear(context: Context) { store(context).edit().clear().apply() }
    fun readFeed(context: Context): HomeFeedModel? = read(context, "feed", "catalog")
    fun writeFeed(context: Context, feed: HomeFeedModel, session: String = SpotifySession.spDc(context)) = write(context, "feed", "catalog", feed, session)
    fun readDiscoveries(context: Context, window: Int): List<HomeSection> = read(context, "discovery", window.toString())?.sections.orEmpty()
    fun writeDiscoveries(context: Context, window: Int, rows: List<HomeSection>, session: String = SpotifySession.spDc(context)) =
        write(context, "discovery", window.toString(), HomeFeedModel(sections = rows), session)

    private fun read(context: Context, key: String, scope: String): HomeFeedModel? {
        val account = owner(context)
        if (account.isEmpty()) return null
        return store(context).getString(key, null)?.let { HomeSnapshotCodec.decode(it, account, scope) }
    }
    private fun write(context: Context, key: String, scope: String, feed: HomeFeedModel, session: String) {
        val account = fingerprint(session)
        if (session != SpotifySession.spDc(context)) return
        if (account.isEmpty() || feed.sections.none { it.items.isNotEmpty() }) return
        store(context).edit().putString(key, HomeSnapshotCodec.encode(account, scope, feed)).apply()
    }
}

/** Versioned format. A damaged or incompatible cache simply falls back to a live feed. */
object HomeSnapshotCodec {
    fun retainAvailable(fresh: List<HomeSection>, previous: List<HomeSection>): List<HomeSection> = fresh.map { row ->
        row.takeIf { it.items.isNotEmpty() } ?: previous.firstOrNull { it.title == row.title } ?: row
    }

    fun encode(owner: String, scope: String, feed: HomeFeedModel): String = JSONObject()
        .put("version", 1).put("owner", owner).put("scope", scope).put("greeting", feed.greeting)
        .put("sections", JSONArray().apply { feed.sections.take(30).forEach { section ->
            put(JSONObject().put("title", section.title).put("items", JSONArray().apply {
                section.items.take(30).forEach { put(itemJson(it)) }
            }))
        } }).toString()

    fun decode(raw: String, owner: String, scope: String): HomeFeedModel? = runCatching {
        require(raw.length <= 2_000_000 && owner.isNotBlank())
        val root = JSONObject(raw)
        require(root.getInt("version") == 1 && root.getString("owner") == owner && root.getString("scope") == scope)
        val sections = root.getJSONArray("sections")
        require(sections.length() <= 30)
        HomeFeedModel(root.optString("greeting"), (0 until sections.length()).map { index ->
            val section = sections.getJSONObject(index)
            val items = section.getJSONArray("items")
            require(items.length() <= 30)
            HomeSection(section.getString("title"), (0 until items.length()).mapNotNull { readItem(items.getJSONObject(it)) })
        })
    }.getOrNull()

    private fun itemJson(item: HomeItem): JSONObject = JSONObject().put("name", item.name).put("image", item.imageUrl).apply {
        when (item) {
            is HomeItem.Album -> { put("type", "album"); put("subtitle", item.subtitle); put("artists", item.artists) }
            is HomeItem.Playlist -> { put("type", "playlist"); put("subtitle", item.subtitle); put("id", item.id); put("artistKey", item.artistKey); put("mixedArtists", item.mixedArtists) }
            is HomeItem.Artist -> { put("type", "artist"); put("id", item.id) }
            is HomeItem.Radio -> { put("type", "radio"); put("song", songJson(item.seed)) }
            is HomeItem.RecentTrack -> { put("type", "track"); put("song", songJson(item.song)) }
        }
    }
    private fun songJson(song: SongsModel) = JSONObject().put("id", song.id).put("title", song.title)
        .put("album", song.album).put("singer", song.singer).put("cover", song.coverUri).put("url", song.url)
        .put("spotify", song.spotifyTrackId).put("explicit", song.explicit).put("duration", song.durationMs)
    private fun readSong(o: JSONObject) = SongsModel(o.getInt("id"), o.getString("title"), o.optString("album"),
        o.optString("singer"), o.optString("cover"), o.getString("url"), o.optString("spotify"),
        o.optBoolean("explicit", false), o.optInt("duration", 0))
    private fun readItem(o: JSONObject): HomeItem? = when (o.getString("type")) {
        "album" -> HomeItem.Album(o.getString("name"), o.optString("image"), o.optString("subtitle"), o.optString("artists"))
        "playlist" -> HomeItem.Playlist(o.getString("name"), o.optString("image"), o.optString("subtitle"), o.optString("id"), o.optString("artistKey"), o.optBoolean("mixedArtists", false))
        "artist" -> HomeItem.Artist(o.getString("name"), o.optString("image"), o.optString("id"))
        "radio" -> HomeItem.Radio(readSong(o.getJSONObject("song")))
        "track" -> HomeItem.RecentTrack(readSong(o.getJSONObject("song")))
        else -> null
    }
}
