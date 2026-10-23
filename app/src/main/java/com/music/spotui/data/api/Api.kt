package com.music.spotui.data.api

import android.content.Context
import android.util.Log
import com.metrolist.spotify.Spotify
import com.metrolist.spotify.models.SpotifyAlbum
import com.metrolist.spotify.models.SpotifyArtist
import com.metrolist.spotify.models.SpotifyTrack
import com.music.spotui.data.entity.AlbumsModel
import com.music.spotui.data.entity.PodcastModel
import com.music.spotui.data.entity.ArtistOverviewModel
import com.music.spotui.data.entity.ArtistTrackUi
import com.metrolist.spotify.models.SpotifyHomeFeedItem
import com.music.spotui.data.entity.ArtistsModel
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import com.music.spotui.data.entity.SearchResults
import com.music.spotui.data.entity.SongsModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Spotify-backed replacement for the original Firebase Firestore data source.
 * Preserves the original `Flow<Response<List<...>>>` contract so the existing
 * ViewModels / UI keep working unchanged.
 *
 * Metadata (albums, artists, songs) comes from Spotify's web API; actual audio
 * is resolved from YouTube at playback time (see SongPlayer), so [SongsModel.url]
 * carries a stable playback key with the Spotify id plus the title/artist search
 * text, rather than a direct stream URL.
 */
class Api @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun stableId(key: String): Int = key.hashCode() and 0x7fffffff

    /**
     * Process-level cache for the Home feeds. The Home ViewModel is recreated on
     * every navigation back to Home and would otherwise re-hit the network each
     * time (slow). Once loaded we emit the cached result instantly, then refresh
     * in the background so the list stays fresh without blocking the UI.
     */
    companion object HomeCache {
        /** Sentinel id for the special pinned "Liked Songs" library entry. */
        const val LIKED_SONGS_ID = "liked-songs"
        /** Sentinel id for the special pinned "Downloaded" library entry. */
        const val DOWNLOADS_ID = "downloaded-offline"

        /** Pinned "Downloaded" shortcut to the offline tracks, like Spotify's library. */
        val DOWNLOADED_ENTRY = com.music.spotui.data.entity.LibraryEntry(
            spotifyId = DOWNLOADS_ID,
            name = "Downloaded",
            subtitle = "Available offline",
            coverUri = "",
            isPlaylist = true,
        )

        @Volatile var albums: List<AlbumsModel>? = null
        @Volatile var artists: List<ArtistsModel>? = null
        @Volatile var home: HomeFeedModel? = null
        @Volatile var library: List<com.music.spotui.data.entity.LibraryEntry>? = null

        /** Drop all cached feeds (e.g. on logout / account switch). */
        fun clear() {
            albums = null; artists = null; home = null; library = null
        }
    }

    private fun SpotifyTrack.toSongModel(): SongsModel {
        val singer = artists.joinToString(", ") { it.name }
        val cover = album?.images?.firstOrNull()?.url ?: ""
        return SongsModel(
            // Local files in a playlist have no track id; keyed by name they would all
            // share one id, which crashed lists keyed by id (#88, #91).
            id = stableId(if (id.isNotBlank()) "track:$id" else "local:$name|$singer"),
            title = name.take(128),
            album = album?.name ?: "",
            singer = singer,
            coverUri = cover,
            // Playback resolves the search text in this key against YouTube, but
            // keeps the Spotify id in the cache key so same-named tracks do not
            // reuse each other's stream.
            url = com.music.spotui.di.SongPlayer.buildSpotifyPlayQuery(id, name, singer),
            spotifyTrackId = id,
            explicit = explicit,
            durationMs = durationMs,
        )
    }

    private fun SpotifyAlbum.toAlbumModel(): AlbumsModel = AlbumsModel(
        id = stableId("album:$id"),
        artists = artists.joinToString(", ") { it.name },
        coverUri = images.firstOrNull()?.url ?: "",
        name = name,
        time = releaseDate ?: "",
        type = albumType.orEmpty(),
    )

    private fun SpotifyArtist.toArtistModel(): ArtistsModel = ArtistsModel(
        name = name,
        coverUri = images.firstOrNull()?.url ?: "",
        id = id,
    )

    suspend fun getAlbums(): Flow<Response<List<AlbumsModel>>> = flow {
        HomeCache.albums?.let { emit(Response.Success(it)) } ?: emit(Response.Loading())
        if (!SpotifyTokenProvider.ensureToken(context)) {
            if (HomeCache.albums == null) emit(Response.Error("Spotify not authenticated — set sp_dc cookie"))
            return@flow
        }
        Spotify.newReleases(limit = 20).fold(
            onSuccess = { resp ->
                val list = resp.albums?.items.orEmpty().map { it.toAlbumModel() }
                HomeCache.albums = list
                emit(Response.Success(list))
            },
            onFailure = {
                Log.e("Api", "getAlbums failed", it)
                if (HomeCache.albums == null) emit(Response.Error(it.message ?: "error"))
            },
        )
    }

    /** Artists the user follows on Spotify (library "Artists" filter). */
    suspend fun getFollowedArtists(): List<ArtistsModel> {
        if (!SpotifyTokenProvider.ensureToken(context)) return emptyList()
        return fetchAllPages { offset -> Spotify.myArtists(limit = 50, offset = offset) }
            .map { it.toArtistModel() }
            .also { if (it.isEmpty()) Log.d("Api", "getFollowedArtists: none") }
    }

    suspend fun getFollowingFeed(onUpdate: (com.music.spotui.data.entity.FollowingFeed) -> Unit = {}): com.music.spotui.data.entity.FollowingFeed = kotlinx.coroutines.coroutineScope {
        check(SpotifyTokenProvider.ensureToken(context)) { "Sign in to Spotify to see followed artists." }
        // Distinguish failed following-list requests from an empty account.
        val followed = mutableListOf<ArtistsModel>()
        var offset = 0
        do {
            val page = kotlinx.coroutines.withTimeout(20_000L) { Spotify.myArtists(limit = 50, offset = offset).getOrThrow() }
            followed += page.items.map { it.toArtistModel() }
            offset += page.items.size
        } while (page.items.isNotEmpty() && offset < page.total)
        val unique = followed.distinctBy { it.id }
        val gate = kotlinx.coroutines.sync.Semaphore(3)
        val lock = kotlinx.coroutines.sync.Mutex()
        val artists = mutableListOf<com.music.spotui.data.entity.FollowingArtist>()
        val releases = mutableListOf<com.music.spotui.data.entity.FollowingRelease>()
        var loaded = 0
        var failed = 0
        fun snapshot() = com.music.spotui.data.entity.FollowingFeed(
            artists = artists.toList(),
            releases = com.music.spotui.data.recommendation.FollowingPolicy.newest(releases),
            popularSongs = com.music.spotui.data.recommendation.FollowingPolicy.popular(artists.flatMap { it.songs }),
            failedArtists = failed, followedCount = unique.size, loadedArtists = loaded,
        )
        onUpdate(snapshot())
        unique.map { artist ->
            async(kotlinx.coroutines.Dispatchers.IO) {
                gate.withPermit {
                    val overview = kotlinx.coroutines.withTimeoutOrNull(20_000L) { Spotify.artistOverview(artist.id).getOrNull() }
                    lock.withLock {
                        loaded++
                        if (overview == null) failed++ else {
                            artists += com.music.spotui.data.entity.FollowingArtist(
                                artist.id, artist.name, overview.avatarImages.firstOrNull()?.url ?: artist.coverUri,
                                overview.topTracks.map { ArtistTrackUi(it.track.toSongModel(), it.playcount) }.distinctBy { it.song.id })
                            releases += overview.popularReleases.map { release ->
                                com.music.spotui.data.entity.FollowingRelease(release.id, release.name, artist.name,
                                    release.images.firstOrNull()?.url.orEmpty(), release.releaseDate.orEmpty())
                            }
                        }
                        // Make the first artists playable while the rest of a large following list loads.
                        onUpdate(snapshot())
                    }
                }
            }
        }.awaitAll()
        snapshot()
    }

    suspend fun getArtists(): Flow<Response<List<ArtistsModel>>> = flow {
        HomeCache.artists?.let { emit(Response.Success(it)) } ?: emit(Response.Loading())
        if (!SpotifyTokenProvider.ensureToken(context)) {
            if (HomeCache.artists == null) emit(Response.Error("Spotify not authenticated — set sp_dc cookie"))
            return@flow
        }
        Spotify.topArtists(limit = 20).fold(
            onSuccess = { paging ->
                val list = paging.items.map { it.toArtistModel() }
                HomeCache.artists = list
                emit(Response.Success(list))
            },
            onFailure = {
                Log.e("Api", "getArtists failed", it)
                if (HomeCache.artists == null) emit(Response.Error(it.message ?: "error"))
            },
        )
    }

    suspend fun getSongs(): Flow<Response<List<SongsModel>>> = flow {
        emit(Response.Loading())
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        Spotify.topTracks(limit = 50).fold(
            onSuccess = { paging -> emit(Response.Success(paging.items.map { it.toSongModel() })) },
            onFailure = { Log.e("Api", "getSongs failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /**
     * Personalized Spotify home feed (the `home` GQL operation) — the real
     * landing page: "Your top mixes", "Jump back in", "Your favorite artists",
     * etc. Process-cached like the other home feeds for instant re-entry.
     */
    suspend fun getHomeFeed(): Flow<Response<HomeFeedModel>> = flow {
        val session = SpotifySession.spDc(context)
        val cached = HomeCache.home ?: com.music.spotui.data.preferences.HomeSnapshotCache.readFeed(context)
        cached?.let { HomeCache.home = it; emit(Response.Success(it)) } ?: emit(Response.Loading())
        if (!SpotifyTokenProvider.ensureToken(context)) {
            if (HomeCache.home == null) emit(Response.Error("Spotify not authenticated — set sp_dc cookie"))
            return@flow
        }
        Spotify.home(sectionItemsLimit = 20).fold(
            onSuccess = { feed ->
                if (SpotifySession.spDc(context) != session) return@fold
                val sections = feed.sections.mapNotNull { section ->
                    // The GQL feed can repeat an item inside one section (and
                    // repeat whole sections, e.g. two "New releases" rows) —
                    // that showed up as duplicated album cards on Home.
                    val items = section.items
                        .distinctBy { it.uri }
                        .mapNotNull { it.toHomeItem() }
                        .distinctBy { it::class.simpleName + "|" + it.name.lowercase() }
                    if (items.isEmpty()) null
                    else HomeSection(title = section.title ?: "", items = items)
                }.distinctBy { it.title.lowercase().ifBlank { it.hashCode().toString() } }
                val model = HomeFeedModel(greeting = feed.greeting ?: "", sections = sections)
                HomeCache.home = model
                com.music.spotui.data.preferences.HomeSnapshotCache.writeFeed(context, model, session)
                emit(Response.Success(model))
            },
            onFailure = {
                Log.e("Api", "getHomeFeed failed", it)
                if (HomeCache.home == null) emit(Response.Error(it.message ?: "error"))
            },
        )
    }

    private fun SpotifyHomeFeedItem.toHomeItem(): HomeItem? = when (this) {
        is SpotifyHomeFeedItem.Album -> HomeItem.Album(
            name = name,
            imageUrl = imageUrl ?: "",
            subtitle = artists.joinToString(", ") { it.name }.ifBlank { "Album" },
            artists = artists.joinToString(", ") { it.name },
        )
        is SpotifyHomeFeedItem.Artist -> HomeItem.Artist(
            name = name,
            imageUrl = imageUrl ?: "",
            id = id,
        )
        is SpotifyHomeFeedItem.Playlist -> HomeItem.Playlist(
            name = name,
            imageUrl = imageUrl ?: "",
            subtitle = (madeForUsername ?: ownerName)?.let { "Playlist • $it" } ?: "Playlist",
            id = id,
        )
    }

    /**
     * Live track search via Spotify's GraphQL search (not the rate-limited
     * personalized endpoints). Blank query yields an empty list.
     */
    suspend fun searchTracks(query: String): Flow<Response<List<SongsModel>>> = flow {
        emit(Response.Loading())
        if (query.isBlank()) {
            emit(Response.Success(emptyList())); return@flow
        }
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        Spotify.search(query, types = listOf("track"), limit = 30).fold(
            onSuccess = { res -> emit(Response.Success(res.tracks?.items.orEmpty().map { it.toSongModel() })) },
            onFailure = { Log.e("Api", "searchTracks failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /** Spotify catalog search; preserves Spotify's order within each category. */
    suspend fun searchEverything(query: String, offset: Int = 0): Flow<Response<SearchResults>> = flow {
        emit(Response.Loading())
        if (query.isBlank()) {
            emit(Response.Success(SearchResults())); return@flow
        }
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        val limit = 50
        Spotify.search(query.trim(), types = listOf("track", "album", "artist", "playlist"), limit = limit, offset = offset).fold(
            onSuccess = { res ->
                val more = listOf(res.tracks?.total, res.albums?.total, res.artists?.total, res.playlists?.total)
                    .any { it != null && it > offset + limit }
                val music = SearchResults(
                    songs = res.tracks?.items.orEmpty().map { it.toSongModel() },
                    albums = res.albums?.items.orEmpty().map { it.toAlbumModel() },
                    artists = res.artists?.items.orEmpty().map { it.toArtistModel() },
                    playlists = res.playlists?.items.orEmpty().filter { it.id.isNotBlank() }.distinctBy { it.id }.map { playlist ->
                        com.music.spotui.data.entity.LibraryEntry(playlist.id, playlist.name,
                            "Playlist" + (playlist.owner?.displayName?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                            playlist.images.firstOrNull()?.url.orEmpty(), true)
                    },
                    nextOffset = if (more) offset + limit else null,
                )
                // Music should appear immediately, even if podcast search is slow/unavailable.
                emit(Response.Success(music))
                if (offset == 0) {
                    val podcasts = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                        Spotify.searchPodcasts(query, limit = 12).getOrNull()
                    }
                    if (podcasts != null) emit(Response.Success(music.copy(
                        shows = podcasts.shows?.items.orEmpty().map { it.toPodcastModel() },
                        episodes = podcasts.episodes?.items.orEmpty().map { it.toEpisodeSongModel(null) },
                    )))
                }
            },
            onFailure = { Log.e("Api", "searchEverything failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /** A podcast show's episodes, as playable [SongsModel] (url = "episode:<id>"). */
    suspend fun getShowEpisodes(showId: String): Flow<Response<List<SongsModel>>> = flow {
        emit(Response.Loading())
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        val show = Spotify.show(showId).getOrNull()
        Spotify.showEpisodes(showId, limit = 50).fold(
            onSuccess = { paging ->
                emit(Response.Success(paging.items.map { it.toEpisodeSongModel(show?.name) }))
            },
            onFailure = { Log.e("Api", "getShowEpisodes failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /** Show header (name/publisher/cover) for the detail screen. */
    suspend fun getShow(showId: String): PodcastModel? {
        if (!SpotifyTokenProvider.ensureToken(context)) return null
        return Spotify.show(showId).getOrNull()?.toPodcastModel()
    }

    private fun com.metrolist.spotify.models.SpotifyShow.toPodcastModel() = PodcastModel(
        id = id,
        name = name,
        publisher = publisher,
        coverUri = images.firstOrNull()?.url ?: "",
    )

    private fun com.metrolist.spotify.models.SpotifyEpisode.toEpisodeSongModel(showName: String?): SongsModel {
        val subtitle = show?.name ?: showName ?: "Podcast"
        return SongsModel(
            id = stableId("episode:$id"),
            title = name,
            album = subtitle,
            singer = subtitle,
            coverUri = images.firstOrNull()?.url ?: (show?.images?.firstOrNull()?.url ?: ""),
            url = "episode:$id",
        )
    }

    /** Fetch each Radio influence through existing Spotify integrations, then blend locally. */
    suspend fun getRadioRecommendations(original: String, evolving: String): List<SongsModel> = kotlinx.coroutines.coroutineScope {
        val sessions=com.music.spotui.data.preferences.RadioSessions
        val mode=com.music.spotui.data.preferences.RadioTaste.mode(context,original)
        val tasteRevision=com.music.spotui.data.preferences.RadioTaste.revision.value
        val heard=com.music.spotui.data.preferences.RadioMemory.recent(context,original)
        val saved=sessions.anchors(context,original)
        val originalIds=if(saved.isEmpty())listOf(original).filter{!sessions.isMix(it)} else saved.map{sessions.resolvedAnchor(context,it).spotifyTrackId}
        val anchors=originalIds.filter(String::isNotBlank).distinct().take(5)
        val extras=listOf(evolving).filter{com.music.spotui.data.recommendation.RadioDiscoveryPolicy.evolving(mode) && it.isNotBlank() && it!=original && it !in anchors && !sessions.isMix(it)}
        val seeds=(anchors+extras).distinct()
        val requests=seeds.map { id -> async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(18_000L) {
                    val song=saved.firstOrNull { sessions.resolvedAnchor(context,it).spotifyTrackId==id }
                    var batch=getRecommendations(listOf(id),radioMode=mode)
                    if(song!=null && (batch.isEmpty() || sessions.repairDue(context,song.spotifyTrackId))) {
                        val repaired=repairRadioAnchor(song)
                        if(batch.isEmpty() && repaired!=null && repaired.spotifyTrackId!=id)batch=getRecommendations(listOf(repaired.spotifyTrackId),radioMode=mode)
                    }
                    com.music.spotui.data.recommendation.RadioMemoryPolicy.order(batch,heard,System.currentTimeMillis())
                }.orEmpty()
            } catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled
            } catch(error:Exception){emptyList<SongsModel>()}
        } }
        com.music.spotui.data.recommendation.RadioMixPolicy.blend(seeds,requests.map{it.await()},anchors.size,
            mode)
            .let { songs -> if(tasteRevision!=com.music.spotui.data.preferences.RadioTaste.revision.value)emptyList() else songs.filter{com.music.spotui.data.preferences.RadioTaste.allowed(context,it)} }
    }

    /** Refresh lookup identity only after checking recording evidence; station/queue identity stays fixed. */
    private suspend fun repairRadioAnchor(original: SongsModel): SongsModel? {
        val sessions=com.music.spotui.data.preferences.RadioSessions
        if(!sessions.repairAllowed(context,original.spotifyTrackId))return null
        sessions.checked(context,original.spotifyTrackId)
        return kotlinx.coroutines.withTimeoutOrNull(5_000L) {
            val current=sessions.resolvedAnchor(context,original)
            val live=Spotify.track(current.spotifyTrackId).getOrNull()
            if(live!=null && live.id==current.spotifyTrackId) {
                // The same immutable catalog ID is authoritative recording evidence.
                sessions.saveIsrc(context,original.spotifyTrackId,live.isrc.orEmpty())
                return@withTimeoutOrNull null
            }
            val expected=sessions.knownIsrc(context,original.spotifyTrackId)
            val found=listOfNotNull(live)+Spotify.search("${original.title} ${original.singer}",types=listOf("track"),limit=15).getOrNull()?.tracks?.items.orEmpty()
            val exact=found.firstOrNull { candidate ->
                candidate.id!=current.spotifyTrackId && com.music.spotui.data.recommendation.RadioSeedRepairPolicy.sameRecording(original,candidate.toSongModel(),expected,candidate.isrc.orEmpty())
            } ?: return@withTimeoutOrNull null
            val replacement=exact.toSongModel()
            if(sessions.repaired(context,original,replacement,expected,exact.isrc.orEmpty())) {
                com.music.spotui.data.preferences.PlaybackDiagnostics.record(context,"Radio lookup repaired","Verified same recording")
                replacement
            } else null
        }
    }

    suspend fun getRecommendations(seedTrackIds: List<String>, radioMode:String? = null): List<SongsModel> {
        val seeds = seedTrackIds.filter { it.isNotBlank() }.distinct()
        if (seeds.isEmpty()) return emptyList()
        if (!SpotifyTokenProvider.ensureToken(context)) return emptyList()
        val seedId = seeds.last()
        // Primary: the exact radio the Spotify web player queues after this track —
        // the inspiredby-mix station playlist. This IS Spotify's real queue, so the
        // continuation matches what open.spotify.com would play next.
        Spotify.trackRadio(seedId).getOrNull()
            ?.filter { it.id != seedId }
            ?.takeIf { it.isNotEmpty() }
            ?.let { radio -> return radio.map { it.toSongModel() } }
        Log.w("Api", "track radio empty — trying native recommender")
        // Fallback 1: Spotify's own recommender (the SEO "recommended tracks" GQL op).
        // Still Spotify's real backend, so it beats any local heuristic when available.
        Spotify.recommendedTracks(seedId).getOrNull()?.takeIf { it.isNotEmpty() }?.let { native ->
            return native.map { it.toSongModel() }
        }
        if(!com.music.spotui.data.recommendation.RadioDiscoveryPolicy.artistFallback(radioMode))return emptyList()
        Log.w("Api", "native recommender empty — trying artist radio")
        val seedTrack = Spotify.track(seedId).getOrNull()
            ?: return emptyList<SongsModel>().also { Log.w("Api", "getRecommendations: seed track unresolved") }

        // Fallback 2: artist radio built purely from GQL endpoints (seed artist top
        // tracks + related artists' top tracks). Unlike the heuristic engine this does
        // NOT depend on me/top/tracks|artists, which are frequently HTTP 429 rate-limited
        // with the web token — so it keeps working when the profile-based engine can't.
        artistRadio(seedTrack).takeIf { it.isNotEmpty() }?.let { radio ->
            return radio.map { it.toSongModel() }
        }

        if(!com.music.spotui.data.recommendation.RadioDiscoveryPolicy.profileFallback(radioMode))return emptyList()
        Log.w("Api", "artist radio empty — falling back to heuristic engine")
        // Fallback 3: profile-based heuristic engine (best-personalized but needs top data).
        return com.music.spotui.data.recommendation.SpotifyRecommendationEngine
            .getRecommendations(seedTrack, limit = 25)
            .map { it.toSongModel() }
            .ifEmpty { emptyList<SongsModel>().also { Log.w("Api", "getRecommendations returned no tracks") } }
    }

    /**
     * A reliable "song radio" from a seed track using only GQL endpoints (not the
     * rate-limited REST top-data ones): the seed artist's top tracks plus the top
     * tracks of related artists, deduped and shuffled. Mirrors how tapping a single
     * song on Spotify continues into similar music.
     */
    private suspend fun artistRadio(seedTrack: SpotifyTrack): List<SpotifyTrack> {
        val seedArtistId = seedTrack.artists.firstOrNull()?.id ?: return emptyList()
        val out = mutableListOf<SpotifyTrack>()
        val seen = mutableSetOf(seedTrack.id)
        fun add(tracks: List<SpotifyTrack>?, cap: Int) {
            tracks.orEmpty().asSequence()
                .filter { it.id.isNotEmpty() && seen.add(it.id) }
                .take(cap)
                .forEach { out.add(it) }
        }
        add(Spotify.artistTopTracks(seedArtistId).getOrNull()?.tracks, cap = 10)
        Spotify.artistRelatedArtists(seedArtistId).getOrNull().orEmpty().take(6).forEach { related ->
            related.id.takeIf { it.isNotEmpty() }?.let { rid ->
                add(Spotify.artistTopTracks(rid).getOrNull()?.tracks, cap = 5)
            }
        }
        return out.take(30)
    }

    /**
     * Loads the curated playlists for a Browse category/genre. Real Spotify opens
     * a genre catalogue (a page of playlists) rather than running a keyword song
     * search — we approximate that by searching playlists for the genre name and
     * presenting them as a grid the user can open. Returns LibraryEntry rows so
     * the Category screen can reuse the existing playlist row/tile rendering.
     */
    /** Discover NEW Spotify collections from local Spotui listening, independent of Spotify's Home feed. */
    private var localDiscoveryKey = ""
    private var localDiscoveryTime = 0L
    private var localDiscoveryRows = emptyList<HomeSection>()

    fun cachedHomeDiscoveries(): List<HomeSection> {
        val prefs = com.music.spotui.data.preferences.HomePersonalization
        if (!prefs.enabled(context) || prefs.strength(context) == 0) return emptyList()
        val samples = com.music.spotui.data.preferences.getListeningHistory(context).map {
            com.music.spotui.data.recommendation.LocalTasteSample(it.spotifyTrackId, it.singer, it.ts, it.listenedMs, it.source)
        }
        if (com.music.spotui.data.recommendation.LocalHomePolicy.qualifying(samples, System.currentTimeMillis(), prefs.windowDays(context)).size < 3) return emptyList()
        return com.music.spotui.data.preferences.HomeSnapshotCache.readDiscoveries(context, prefs.windowDays(context)).map { row ->
            row.copy(items = row.items.filter { (it !is HomeItem.Playlist || it.id.isNotBlank()) && (it !is HomeItem.Radio || com.music.spotui.data.preferences.RadioTaste.allowed(context, it.seed)) })
        }
    }

    suspend fun discoverHomeCollections(): List<HomeSection> {
        val prefs = com.music.spotui.data.preferences.HomePersonalization
        if (!prefs.enabled(context) || prefs.strength(context) == 0) return emptyList()
        val session = SpotifySession.spDc(context)
        val window = prefs.windowDays(context)
        val settingsRevision = prefs.revision.value
        val tasteRevision = com.music.spotui.data.preferences.RadioTaste.revision.value
        val now = System.currentTimeMillis()
        val policy = com.music.spotui.data.recommendation.LocalHomePolicy
        val samples = com.music.spotui.data.preferences.getListeningHistory(context).map {
            com.music.spotui.data.recommendation.LocalTasteSample(it.spotifyTrackId, it.singer, it.ts, it.listenedMs, it.source)
        }
        if (policy.qualifying(samples, now, window).size < 3) return emptyList()
        if (!SpotifyTokenProvider.ensureToken(context)) return cachedHomeDiscoveries()
        if (SpotifySession.spDc(context) != session) return emptyList()
        if (prefs.revision.value != settingsRevision || com.music.spotui.data.preferences.RadioTaste.revision.value != tasteRevision) return cachedHomeDiscoveries()
        val previous = cachedHomeDiscoveries()
        val tracks = policy.topTracks(samples, now, window)
        val artists = policy.topArtists(samples, now, window)
        val key = session.hashCode().toString() + "|" + tracks.joinToString() + "|" + artists.joinToString() + "|" + window + "|" + Spotify.accessToken.hashCode() + "|" + settingsRevision + "|" + tasteRevision
        if (key == localDiscoveryKey && now - localDiscoveryTime < 2L * 60_000L) return localDiscoveryRows
        val seedTracks = kotlinx.coroutines.coroutineScope {
            // More listening inputs without issuing fifteen catalog lookups simultaneously.
            val permits = kotlinx.coroutines.sync.Semaphore(4)
            tracks.map { id -> async {
                permits.withPermit { kotlinx.coroutines.withTimeoutOrNull(8_000L) { Spotify.track(id).getOrNull() } }
            } }.mapNotNull { it.await() }
        }
        val matchedArtists = artists.mapNotNull { name ->
            val knownId = seedTracks.flatMap { it.artists }.firstOrNull { it.name.equals(name, true) }?.id
            if (!knownId.isNullOrBlank()) knownId to name
            else kotlinx.coroutines.withTimeoutOrNull(8_000L) { Spotify.search(name, types = listOf("artist"), limit = 5).getOrNull() }
                ?.artists?.items.orEmpty().firstOrNull { it.name.equals(name, true) && it.id.isNotBlank() }?.let { it.id to it.name }
        }.distinctBy { it.first }.take(5)
        val overviews = kotlinx.coroutines.coroutineScope {
            matchedArtists.map { artist -> async {
                artist to kotlinx.coroutines.withTimeoutOrNull(15_000L) { Spotify.artistOverview(artist.first).getOrNull() }
            } }.map { it.await() }
        }
        val albums = linkedMapOf<String, HomeItem.Album>()
        fun addAlbum(album: com.metrolist.spotify.models.SpotifyAlbum) {
            if (album.id.isBlank() || album.name.isBlank() || album.albumType.equals("single", true)) return
            val names = album.artists.joinToString(", ") { it.name }
            albums.putIfAbsent(album.id, HomeItem.Album(album.name, album.images.firstOrNull()?.url.orEmpty(), "Album • $names", names))
        }
        seedTracks.forEach { track ->
            track.album?.let { album ->
                addAlbum(com.metrolist.spotify.models.SpotifyAlbum(id = album.id, name = album.name,
                    albumType = album.albumType, images = album.images, artists = album.artists.ifEmpty { track.artists }))
            }
        }
        overviews.forEach { (artist, overview) ->
            overview?.popularReleases?.filter { release -> release.albumType.equals("album", true) && release.artists.any { it.id == artist.first } }
                ?.take(2)?.forEach(::addAlbum)
        }
        val playlistCandidates = kotlinx.coroutines.coroutineScope {
            overviews.take(5).map { (artist, overview) -> async {
                val neighbors = overview?.relatedArtists.orEmpty()
                val genres = (overview?.popularReleases.orEmpty().flatMap { it.genres } + neighbors.flatMap { it.genres })
                    .filter { it.isNotBlank() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
                val broaderQuery = genres.firstOrNull()?.key?.let { "$it mix" }
                    ?: neighbors.firstOrNull()?.name?.let { "$it mix" } ?: "${artist.second} mix"
                tastePlaylists(artist.first, artist.second, neighbors.map { it.id }.toSet(), broaderQuery)
            } }.awaitAll().flatten().distinctBy { it.id }
        }
        val playlists = com.music.spotui.data.recommendation.HomeDiversityPolicy.spread(playlistCandidates).filterIsInstance<HomeItem.Playlist>()
        val madeFor = mutableListOf<HomeItem>()
        val localAlbums = com.music.spotui.data.recommendation.HomeDiversityPolicy.spread(albums.values.toList())
        val madeForPlaylists = playlists.drop(5)
        for (i in 0 until maxOf(madeForPlaylists.size, localAlbums.size)) {
            madeForPlaylists.getOrNull(i)?.let { madeFor += it }
            localAlbums.getOrNull(i)?.let { madeFor += it }
        }
        // Spotify exposes artist/track popularity here, not a global Radio chart.
        val knownIds = matchedArtists.map { it.first }.toSet()
        val related = overviews.flatMap { it.second?.relatedArtists.orEmpty() }.distinctBy { it.id }
            .filter { it.id !in knownIds }.sortedByDescending { it.popularity ?: 0 }.take(5)
        val popular = kotlinx.coroutines.coroutineScope {
            related.map { artist -> async {
                val overview = kotlinx.coroutines.withTimeoutOrNull(8_000L) { Spotify.artistOverview(artist.id).getOrNull() }
                overview?.topTracks?.maxByOrNull { it.playcount ?: 0L }?.let { item ->
                    (overview.monthlyListeners ?: item.playcount ?: 0L) to HomeItem.Radio(item.track.toSongModel())
                }
            } }.awaitAll().filterNotNull().sortedByDescending { it.first }.map { it.second }
        }.filter { com.music.spotui.data.preferences.RadioTaste.allowed(context, it.seed) }
        val fallbackPopular = if (popular.size < 5) kotlinx.coroutines.coroutineScope {
            val recommendations = seedTracks.take(2).map { seed -> async {
                kotlinx.coroutines.withTimeoutOrNull(8_000L) { getRecommendations(listOf(seed.id)) }.orEmpty().take(5)
            } }.awaitAll().flatten().distinctBy { it.spotifyTrackId }.filter { it.spotifyTrackId.isNotBlank() }.take(8)
            recommendations.map { song -> async {
                kotlinx.coroutines.withTimeoutOrNull(4_000L) { Spotify.track(song.spotifyTrackId).getOrNull() }
            } }.awaitAll().filterNotNull().sortedByDescending { it.popularity ?: 0 }.map { HomeItem.Radio(it.toSongModel()) }
        } else emptyList()
        val popularRadios = (popular + fallbackPopular + seedTracks.sortedByDescending { it.popularity ?: 0 }.map { HomeItem.Radio(it.toSongModel()) })
            .filter { com.music.spotui.data.preferences.RadioTaste.allowed(context, it.seed) }
            .distinctBy { it.seed.spotifyTrackId }.take(20)
        val rows = listOf(HomeSection(policy.ALBUMS, com.music.spotui.data.recommendation.HomeDiversityPolicy.spread(madeFor).take(15)),
            HomeSection(policy.PLAYLISTS, playlists.take(20)), HomeSection(policy.SONG_RADIOS, com.music.spotui.data.recommendation.HomeDiversityPolicy.spread(popularRadios).take(15)))
        // Keep the last good shelf if a catalog request fails or returns no candidates.
        if (SpotifySession.spDc(context) != session) return emptyList()
        if (prefs.revision.value != settingsRevision || com.music.spotui.data.preferences.RadioTaste.revision.value != tasteRevision) return cachedHomeDiscoveries()
        val refreshed = com.music.spotui.data.preferences.HomeSnapshotCodec.retainAvailable(rows, previous)
        if (refreshed.any { it.items.isNotEmpty() }) {
            localDiscoveryKey = key; localDiscoveryTime = now; localDiscoveryRows = refreshed
            com.music.spotui.data.preferences.HomeSnapshotCache.writeDiscoveries(context, window, refreshed, session)
        }
        return refreshed
    }

    private val homePlaylistLookups = kotlinx.coroutines.sync.Semaphore(3)
    private suspend fun tastePlaylists(artistId: String, name: String, related: Set<String>, broaderQuery: String): List<HomeItem.Playlist> = kotlinx.coroutines.coroutineScope {
        val candidates = listOf(name, broaderQuery).distinct().map { query -> async {
            homePlaylistLookups.withPermit {
                kotlinx.coroutines.withTimeoutOrNull(4_000L) {
                    Spotify.search(query, types = listOf("playlist"), limit = 4).getOrNull()?.playlists?.items.orEmpty()
                }.orEmpty().filter { it.id.isNotBlank() }.take(2)
            }
        } }.awaitAll().flatten().distinctBy { it.id }
        val allowed = related + artistId
        candidates.map { playlist -> async {
            val sample = homePlaylistLookups.withPermit {
                kotlinx.coroutines.withTimeoutOrNull(4_000L) {
                    Spotify.playlistTracks(playlist.id, limit = 30).getOrNull()?.items.orEmpty().mapNotNull { it.track }
                }.orEmpty()
            }
            val matches = sample.count { track -> track.artists.any { it.id in allowed } }
            if (com.music.spotui.data.preferences.ArtistTastePolicy.relevantPlaylist(matches, sample.size, playlist.name.contains(name, true))) {
                val profile = com.music.spotui.data.recommendation.HomeDiversityPolicy.profile(name, sample.map { it.artists.map { a -> a.name } })
                HomeItem.Playlist(playlist.name, playlist.images.firstOrNull()?.url.orEmpty(), "Spotify playlist • For your listening", playlist.id, profile.artistKey, profile.mixed)
            } else null
        } }.awaitAll().filterNotNull()
    }

    /** Catalog search is validated against playlist tracks and Spotify's related-artist IDs. */
    private suspend fun moreLikePlaylists(name: String): HomeSection? {
        val artist = Spotify.search(name, types = listOf("artist"), limit = 5).getOrNull()
            ?.artists?.items.orEmpty().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null
        val relatedIds = Spotify.artistRelatedArtists(artist.id).getOrNull().orEmpty().map { it.id }.toSet() + artist.id
        val candidates = Spotify.search(name, types = listOf("playlist"), limit = 8).getOrNull()?.playlists?.items.orEmpty()
        val validated = candidates.distinctBy { it.id }.mapNotNull { playlist ->
            if (playlist.id.isBlank()) return@mapNotNull null
            val sample = Spotify.playlistTracks(playlist.id, limit = 50).getOrNull()?.items.orEmpty().mapNotNull { it.track }
            val matches = sample.count { track -> track.artists.any { it.id in relatedIds } }
            if (!com.music.spotui.data.preferences.ArtistTastePolicy.relevantPlaylist(matches, sample.size, playlist.name.contains(name, ignoreCase = true))) return@mapNotNull null
            HomeItem.Playlist(playlist.name, playlist.images.firstOrNull()?.url.orEmpty(), "Playlist", playlist.id)
        }
        return if (validated.size >= 2) HomeSection("More like ${artist.name}", validated) else null
    }

    suspend fun getCategoryPlaylists(genre: String): Flow<Response<List<com.music.spotui.data.entity.LibraryEntry>>> = flow {
        emit(Response.Loading())
        if (genre.isBlank()) {
            emit(Response.Success(emptyList())); return@flow
        }
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        Spotify.search(genre, types = listOf("playlist"), limit = 24).fold(
            onSuccess = { res ->
                emit(Response.Success(res.playlists?.items.orEmpty().map { p ->
                    com.music.spotui.data.entity.LibraryEntry(
                        spotifyId = p.id,
                        name = p.name,
                        subtitle = "Playlist" + (p.owner?.displayName?.let { " • $it" } ?: ""),
                        coverUri = p.images.firstOrNull()?.url ?: "",
                        isPlaylist = true,
                    )
                }))
            },
            onFailure = { Log.e("Api", "getCategoryPlaylists failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /**
     * Loads an artist's top tracks. The UI navigates by artist *name*, so we
     * resolve the artist via GraphQL search, then fetch its top tracks via the
     * GraphQL queryArtistOverview endpoint (neither is rate-limited).
     */
    suspend fun getArtistSongs(artistName: String): Flow<Response<List<SongsModel>>> = flow {
        emit(Response.Loading())
        if (artistName.isBlank()) {
            emit(Response.Success(emptyList())); return@flow
        }
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        val artist = Spotify.search(artistName, types = listOf("artist"), limit = 1).getOrNull()
            ?.artists?.items?.firstOrNull()
        val artistId = artist?.id
        if (artistId.isNullOrBlank()) {
            emit(Response.Success(emptyList())); return@flow
        }
        val artistCover = artist.images.firstOrNull()?.url ?: ""
        Spotify.artistTopTracks(artistId).fold(
            onSuccess = { resp ->
                emit(Response.Success(resp.tracks.map { track ->
                    val song = track.toSongModel()
                    // GQL top-tracks often omit album art — fall back to the artist image.
                    if (song.coverUri.isBlank()) song.copy(coverUri = artistCover) else song
                }))
            },
            onFailure = { Log.e("Api", "getArtistSongs failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /**
     * Loads the full Spotify-style artist page (header, monthly listeners, bio,
     * popular tracks with play counts, discography, related artists) in one GQL
     * round-trip. When the caller knows the exact Spotify artist id it is used
     * directly; otherwise the name is resolved via search (fuzzy — a query like
     * "RAM" may resolve to "Rammstein", so ids are strongly preferred).
     */
    suspend fun getArtistOverview(artistName: String, knownArtistId: String = ""): Flow<Response<ArtistOverviewModel>> = flow {
        emit(Response.Loading())
        if (artistName.isBlank() && knownArtistId.isBlank()) {
            emit(Response.Success(ArtistOverviewModel(name = artistName))); return@flow
        }
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        val artist = if (knownArtistId.isBlank()) {
            // Prefer an EXACT name match among the top hits — the first fuzzy
            // hit for a short name like "RAM" can be a different artist entirely.
            val hits = Spotify.search(artistName, types = listOf("artist"), limit = 5).getOrNull()
                ?.artists?.items.orEmpty()
            hits.firstOrNull { it.name.equals(artistName, ignoreCase = true) } ?: hits.firstOrNull()
        } else null
        val artistId = knownArtistId.ifBlank { artist?.id.orEmpty() }
        if (artistId.isBlank()) {
            emit(Response.Error("Artist not found")); return@flow
        }
        val searchCover = artist?.images?.firstOrNull()?.url ?: ""
        Spotify.artistOverview(artistId).fold(
            onSuccess = { o ->
                val avatar = o.avatarImages.firstOrNull()?.url?.ifBlank { null } ?: searchCover
                val header = o.headerImages.firstOrNull()?.url?.ifBlank { null } ?: avatar
                emit(Response.Success(ArtistOverviewModel(
                    id = o.id,
                    name = o.name.ifBlank { artistName },
                    verified = o.verified,
                    monthlyListeners = o.monthlyListeners,
                    biography = o.biography,
                    headerImage = header,
                    avatarImage = avatar,
                    topTracks = o.topTracks.map { t ->
                        val song = t.track.toSongModel()
                        ArtistTrackUi(
                            song = if (song.coverUri.isBlank()) song.copy(coverUri = avatar) else song,
                            playcount = t.playcount,
                        )
                    },
                    popularReleases = o.popularReleases.map { it.toAlbumModel() },
                    appearsOn = o.appearsOn.map { it.toAlbumModel() },
                    relatedArtists = o.relatedArtists.map { it.toArtistModel() },
                )))
            },
            onFailure = { Log.e("Api", "getArtistOverview failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /**
     * Loads the actual track list for an album. The UI navigates by album *name*
     * (the real Spotify id is lost during mapping), so we resolve the album via
     * search, then fetch its tracks. Uses GraphQL endpoints (not rate-limited).
     */
    suspend fun getAlbumSongs(albumName: String, artist: String = ""): Flow<Response<List<SongsModel>>> = flow {
        emit(Response.Loading())
        if (albumName.isBlank()) {
            emit(Response.Success(emptyList())); return@flow
        }
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        // Two albums can share a name (different artists). Search the name and,
        // when we know the artist, pick the candidate whose artists match instead
        // of blindly taking the first (most-popular) result.
        val candidates = Spotify.search(
            if (artist.isBlank()) albumName else "$albumName $artist",
            types = listOf("album"),
            limit = 10,
        ).getOrNull()?.albums?.items.orEmpty()
        val albumId = pickAlbum(candidates, albumName, artist)?.id
        if (albumId.isNullOrBlank()) {
            emit(Response.Success(emptyList())); return@flow
        }
        Spotify.album(albumId).fold(
            onSuccess = { full -> emit(Response.Success(full.tracks?.items.orEmpty().map { it.toSongModel() })) },
            onFailure = { Log.e("Api", "getAlbumSongs failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /**
     * Loads the real track list for a playlist by its Spotify id (daily mixes,
     * Discover Weekly, personalized playlists, etc). Uses the fetchPlaylist GQL
     * endpoint directly — keyed by id, so it returns the *actual* playlist
     * content rather than a best-effort name search.
     */
    suspend fun getPlaylistSongs(playlistId: String): Flow<Response<List<SongsModel>>> = flow {
        emit(Response.Loading())
        if (playlistId.isBlank()) {
            emit(Response.Success(emptyList())); return@flow
        }
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        // First page right away, then keep paging until the whole playlist is loaded.
        var loaded = false
        fetchAllPages(onPage = { items, _ ->
            loaded = true
            emit(Response.Success(items.mapNotNull { it.track?.toSongModel()?.copy(playlistItemUid = it.uid.orEmpty()) }))
        }) { offset -> Spotify.playlistTracks(playlistId, limit = 100, offset = offset) }
        if (!loaded) emit(Response.Error("Couldn't load this playlist"))
    }

    /**
     * "Your Library" — the user's actual saved Spotify albums plus their
     * playlists (followed + created), merged into one list. Uses the libraryV3
     * GQL endpoint (not rate-limited). Process-cached for instant re-entry.
     */
    suspend fun getLibrary(): Flow<Response<List<com.music.spotui.data.entity.LibraryEntry>>> = flow {
        val retained = com.music.spotui.data.preferences.CreatedPlaylists.merge(context, HomeCache.library.orEmpty())
        if(retained.isNotEmpty())emit(Response.Success(retained))else emit(Response.Loading())
        if (!SpotifyTokenProvider.ensureToken(context)) {
            if (HomeCache.library == null) emit(Response.Error("Spotify not authenticated — set sp_dc cookie"))
            return@flow
        }
        val albums = fetchAllPages { offset -> Spotify.myAlbums(limit = 50, offset = offset) }.map { a ->
            com.music.spotui.data.entity.LibraryEntry(
                spotifyId = a.id,
                name = a.name,
                subtitle = "Album • " + a.artists.joinToString(", ") { it.name },
                coverUri = a.images.firstOrNull()?.url ?: "",
                isPlaylist = false,
                artists = a.artists.joinToString(", ") { it.name },
            )
        }
        val playlists = fetchAllPages { offset -> Spotify.myPlaylists(limit = 50, offset = offset) }.map { p ->
            com.music.spotui.data.entity.LibraryEntry(
                spotifyId = p.id,
                name = p.name,
                subtitle = "Playlist" + (p.owner?.displayName?.let { " • $it" } ?: ""),
                coverUri = p.images.firstOrNull()?.url ?: "",
                isPlaylist = true,
            )
        }
        // Pin "Liked Songs" first, exactly like the Spotify app.
        val liked = com.music.spotui.data.entity.LibraryEntry(
            spotifyId = LIKED_SONGS_ID,
            name = "Liked Songs",
            subtitle = "Playlist • Liked songs",
            coverUri = "https://misc.scdn.co/liked-songs/liked-songs-640.png",
            isPlaylist = true,
        )
        val remote = com.music.spotui.data.recommendation.LibraryIdentityPolicy.unique(listOf(liked, DOWNLOADED_ENTRY) + playlists + albums)
        val merged = com.music.spotui.data.preferences.CreatedPlaylists.merge(context, remote, authoritative = true)
        HomeCache.library = merged
        emit(Response.Success(merged))
        if(com.music.spotui.data.preferences.CreatedPlaylists.recoverLegacy(context)) {
            val recovered=com.music.spotui.data.preferences.CreatedPlaylists.merge(context,merged)
            HomeCache.library=recovered; emit(Response.Success(recovered))
        }
    }

    /**
     * The looping Spotify Canvas video URL for a track (or null). Process-cached
     * per track id — including negative results — so the player only fetches once.
     */
    suspend fun getCanvasUrl(trackId: String): String? {
        if (trackId.isBlank()) return null
        CanvasCache.map[trackId]?.let { return it.value }
        if (!SpotifyTokenProvider.ensureToken(context)) return null
        val url = runCatching { Spotify.canvasUrl(trackId) }.getOrNull()
        CanvasCache.map[trackId] = CanvasCache.Entry(url)
        return url
    }

    private object CanvasCache {
        class Entry(val value: String?)
        val map = java.util.concurrent.ConcurrentHashMap<String, Entry>()
    }

    /** The user's Spotify "Liked Songs" (saved tracks) as playable songs. */
    suspend fun getLikedSongs(): Flow<Response<List<SongsModel>>> = flow {
        emit(Response.Loading())
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated")); return@flow
        }
        // First page right away, then page through the whole library. The ids also
        // drive the local hearts, so unlikes made elsewhere on Spotify show up here.
        var loaded = false
        fetchAllPages(onPage = { items, total ->
            loaded = true
            val models = items.map { it.track.toSongModel() }
            com.music.spotui.data.preferences.syncSpotifyLikedSongIds(
                context, models.mapTo(HashSet()) { it.id.toString() }, complete = items.size >= total,
            )
            emit(Response.Success(models))
        }) { offset -> Spotify.likedSongs(limit = 50, offset = offset) }
        if (!loaded) emit(Response.Error("Couldn't load Liked Songs"))
    }

    /** The logged-in user's account (name, email, avatar, plan) for settings. */
    suspend fun getAccount(): Flow<Response<com.music.spotui.data.entity.AccountModel>> = flow {
        emit(Response.Loading())
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated")); return@flow
        }
        Spotify.me().fold(
            onSuccess = { u ->
                emit(Response.Success(com.music.spotui.data.entity.AccountModel(
                    name = u.displayName ?: u.id,
                    email = u.email ?: "",
                    imageUrl = u.images.firstOrNull()?.url ?: "",
                    plan = u.product?.replaceFirstChar { it.uppercase() } ?: "",
                )))
            },
            onFailure = { Log.e("Api", "getAccount failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /** Loads playlist metadata (name, cover, owner, track count) by id. */
    suspend fun getPlaylist(playlistId: String): Flow<Response<AlbumsModel>> = flow {
        emit(Response.Loading())
        if (playlistId.isBlank()) {
            emit(Response.Error("missing playlist id")); return@flow
        }
        if (!SpotifyTokenProvider.ensureToken(context)) {
            emit(Response.Error("Spotify not authenticated — set sp_dc cookie")); return@flow
        }
        Spotify.playlist(playlistId).fold(
            onSuccess = { p ->
                emit(Response.Success(AlbumsModel(
                    id = stableId("playlist:${p.id}"),
                    artists = p.owner?.displayName ?: "",
                    coverUri = p.images.firstOrNull()?.url ?: "",
                    name = p.name,
                    time = stripHtml(p.description),
                )))
            },
            onFailure = { Log.e("Api", "getPlaylist failed", it); emit(Response.Error(it.message ?: "error")) },
        )
    }

    /**
     * Spotify playlist descriptions come as HTML (e.g. `<a href=spotify:...>Rickey
     * F</a>, …`). Render to plain text — keep the link labels, drop the tags —
     * and decode entities so the UI doesn't show raw markup.
     */
    private fun stripHtml(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return androidx.core.text.HtmlCompat
            .fromHtml(raw, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString()
            .trim()
    }

    /**
     * From a list of same/similar-named album candidates, prefer one whose name
     * matches exactly AND whose artist matches the requested one; fall back to a
     * name match, then an artist match, then the first result.
     */
    private fun pickAlbum(
        candidates: List<com.metrolist.spotify.models.SpotifyAlbum>,
        albumName: String,
        artist: String,
    ): com.metrolist.spotify.models.SpotifyAlbum? {
        if (candidates.isEmpty()) return null
        if (artist.isBlank()) {
            return candidates.firstOrNull { it.name.equals(albumName, ignoreCase = true) }
                ?: candidates.first()
        }
        val wantArtists = artist.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        fun artistMatches(a: com.metrolist.spotify.models.SpotifyAlbum): Boolean {
            val names = a.artists.joinToString(" ") { it.name }.lowercase()
            return wantArtists.any { it.isNotBlank() && names.contains(it) }
        }
        val nameMatches = candidates.filter { it.name.equals(albumName, ignoreCase = true) }
        return nameMatches.firstOrNull { artistMatches(it) }
            ?: candidates.firstOrNull { artistMatches(it) }
            ?: nameMatches.firstOrNull()
            ?: candidates.first()
    }
}

/**
 * Follows a paged endpoint until every item is fetched, so libraries with more than
 * one page load completely. [onPage] sees the items so far after each page. A failed
 * page after the first ends the walk with what was loaded.
 */
internal suspend fun <T> fetchAllPages(
    onPage: suspend (itemsSoFar: List<T>, total: Int) -> Unit = { _, _ -> },
    fetch: suspend (offset: Int) -> kotlin.Result<com.metrolist.spotify.models.SpotifyPaging<T>>,
): List<T> {
    val first = fetch(0).getOrNull() ?: return emptyList()
    val items = first.items.toMutableList()
    onPage(items, first.total)
    var offset = first.items.size
    while (offset < first.total && first.items.isNotEmpty()) {
        val page = fetch(offset).getOrNull() ?: break
        if (page.items.isEmpty()) break
        items += page.items
        offset += page.items.size
        onPage(items, first.total)
    }
    return items
}
