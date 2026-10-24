# Sprotify 1.0.0 Beta 12

## Android Auto

- Four browsable top-level tabs: Home, Recents, Browse and Library, with tab icons. Existing recommendations, mixes, radios, liked songs, playlists, albums and downloads remain reachable.
- Home's Playing queue shows the current song followed by upcoming songs, with Playing now / Up next group hints. It refreshes when the current track, queue metadata or playback source changes. Selecting a song still uses the existing stream resolver and shared phone queue.
- Collection artwork uses grid hints and songs use list hints. The car host decides the final presentation.
- Track metadata contains the actual album instead of the playlist or radio name, including preloaded upcoming tracks. Playback source is supplied separately as playlist metadata and the Playing queue subtitle.
- Keeps the existing Add to queue command for clients that support media-item actions. Its visibility depends on the host.

## Scope and verification

Uses the existing MediaLibraryService with Media3 1.5.1; no Car App API 8 dependency. The native host Queue still reflects the audio engine's limited timeline. This release improves the browse queue; a full logical-session queue adapter and shared transition refactor are deferred.

The build workflow runs Android unit tests, Spotify tests, desktop UI tests, debug assembly and release lint. The publish workflow repeats tests, builds the signed APK and verifies its signature and package/version before publishing. DHU and BYD playback have not been tested for this release.

Manual checks: open all four tabs, play a playlist and radio, advance tracks with Playing queue open, add a search result to the queue without interrupting audio, verify different albums across a preloaded transition, then reconnect Android Auto. Also verify phone and Spotify web playback.

## Installation

`versionCode 12`, `versionName 1.0.0-beta.12`, package `lv.guntify.player`. The existing signing identity is retained, allowing an in-place update from build 11.

Release assets include signed `Sprotify.apk`, source archive, checksums, license and attribution. See `LICENSE`, `NOTICE` and `SOURCE.md`.
