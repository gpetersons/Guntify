# Guntify 1.0.0 Beta 3: Android Auto Radio + personalized Home

Built from the **working Beta 2 baseline**, not the abandoned/closed Beta 3 Pull Request #6.

## P1: Radio improvements
- Show the active Radio station in the Android Auto browse Home heading: **Playing: [station name]**.
- Attach the current station to standard media **album metadata** for newly resolved streams, while keeping the song title and artist unchanged. Android Auto determines whether that metadata is visible in its native Now Playing view.
- Browse a live, synchronized playing queue sourced directly from Guntify's phone-side queue. Starting another saved Radio on the phone changes what this folder shows.
- Warm the first upcoming recommended Radio song in the background when safe, to help reduce next-track loading delays. Remote source availability may still cause silence.

## P2: personalized Android Auto Home
- **Playing queue**, **Continue listening**, **Recommended for you**, **Made for you**, **Recently played**, and **Your radios** folders.
- The same actual personalized Spotify Home collections and authenticated snapshot cache used by the phone, including opening albums, playlists and artists.
- Playback history drives Continue listening and saved radio stations appear in Your radios.
- MediaLibrary paging and narrow playing-queue refresh events to protect Beta 2's album and Now Playing navigation stability.
- Liked Songs, playlists, albums, downloads, and the existing Play Radio action remain available.

## Update installation
- Signed release: **versionCode 3**, `versionName 1.0.0-beta.3`, `applicationId lv.guntify.player`.
- Uses the existing Guntify signing identity, so install `Guntify.apk` directly over **Beta 2**, without uninstalling first, to retain local data.
- Back up important settings before trying a new beta. If Android rejects the installation because of signature mismatch, **do not uninstall Beta 2**; report the error.

## Test expectations
- GitHub CI must pass Android tests/build/lint before release.
- Android Auto physical-car validation is still needed for exact personalized folder rendering and Radio status placement. Follow `ANDROID_AUTO_P1_P2_TESTPLAN.md`.
- A full independent security audit has not yet been completed.

**Acknowledgements:** [Spotui](https://github.com/Spotui/Spotui) and [USpot](https://github.com/rare621/USpot). GPLv3 licensing and existing attribution notices are retained.
