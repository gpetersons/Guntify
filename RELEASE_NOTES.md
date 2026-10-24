# Sprotify 1.0.0 Beta 9 — update build 11

This reissues the Beta 9 display version with Android version code 11. Phones with earlier Beta 9 builds can update in place, while the app continues to show `1.0.0-beta.9`.

## Android Auto playback and queue

- Restores the proven track-selection path through `SongPlayer`, so selecting a track in Android Auto uses the app's normal stream resolver and starts playback.
- Adds an explicit **Ievietot rindā** action to playable track rows. It adds that song to the shared in-app queue without interrupting the current track.
- Keeps Android Auto's regular track selection as **play now**.

## Installation

- Signed `Sprotify.apk`, same package and signing identity as earlier betas.
- `versionCode 11`, `versionName 1.0.0-beta.9`.
- Package `lv.guntify.player` unchanged; install over Beta 8 or Beta 9 without uninstalling. The higher build code enables the in-app updater to offer this build.

## Checks

GitHub Actions builds the signed APK and runs the repository's unit, Spotify and desktop UI checks. Please verify phone audio first, then confirm in Android Auto that **Ievietot rindā** leaves the current song playing and places the selected song next.

Credits, GPLv3 license and source attribution remain in `LICENSE`, `NOTICE` and `SOURCE.md`.
