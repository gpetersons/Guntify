# Sprotify 1.0.0 Beta 8 — corrected legacy Android Auto release

This is a **replacement Beta 8 APK**, rebuilt from the stable Beta 7 Android Auto media player. The previous Beta 8 introduced a Car App API 8 template that could show a source header but failed to load playback on the tested head unit. That experimental template has been fully removed.

## Only two new features

1. **Three Shuffle modes from Android Auto:** OFF (plain icon) → ON (small dot) → SMART (dot + star) → OFF. Android Auto calls the same `CurrentSongState.cycleShuffle()` as the phone, so both controls remain in sync. Radio and Downloads keep the phone's existing OFF/ON-only behavior.
2. **Playback source information:** Radio station, playlist, album and Liked Songs names are exposed through standard Media3 album metadata and Android Auto browse-root/playing-queue titles. The current song title and artist are preserved. For example `Endless radio` or `Dziesmas, kas tev patīk`. **The Android Auto legacy Now Playing header is controlled by the host:** the source name is not guaranteed to appear exactly in the top bar, despite the metadata being available.

## Restored / deliberately unchanged

- Stable **Beta 7 MediaLibraryService**, ExoPlayer and Spotify Web playback.
- Original Android Auto Now Playing UI and its built-in `Ievietot rindā` queue button **with text**.
- Radio button, Repeat, Favorites, artist/title, artwork, fast Next, queue and playback restoration.
- **No Car App API 8**, Car App Library service or related dependencies/manifest permissions.

## Installation

- `Sprotify.apk`, signed using the same key as Beta 7.
- `versionCode 8`, `versionName 1.0.0-beta.8`.
- Package `lv.guntify.player` unchanged; install over Beta 7 or the previous Beta 8 **without uninstalling**.
- Because this replaces an APK with the **same Beta 8 version number**, built-in update checks may not detect it. Download and install this corrected APK manually.

## Checks

GitHub Actions run unit tests, desktop UI checks and verify the signed APK package and version. Physical head-unit testing is still needed for source-label placement, Smart Shuffle indicator visibility and playback.

Credits, GPLv3 license and source attribution remain in `LICENSE`, `NOTICE` and `SOURCE.md`.
