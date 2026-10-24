# Sprotify 1.0.0 Beta 9 — update build 10

This reissues the Beta 9 display version with Android version code 10. The higher code lets phones that installed the withdrawn Beta 9 update in place, while the app continues to show `1.0.0-beta.9`.

## Android Auto queue fix

- Android Auto's native `Ievietot rindā` action now follows the media-session add-item path and adds the selected track to the shared in-app queue after the current track. It does not replace the queue or start the selected track immediately.
- Selecting a track to play still uses the set-item path and starts that track.
- Multiple manual additions retain their order ahead of the remaining queue.

## Deliberately unchanged

- Same package, signing identity, player and update path.
- Other Android Auto controls, browse screens, metadata and playback behavior.

## Installation

- `Sprotify.apk`, signed with the same key as earlier Sprotify betas.
- `versionCode 10`, `versionName 1.0.0-beta.9`.
- Package `lv.guntify.player` unchanged; install over Beta 8 or the withdrawn Beta 9 **without uninstalling**. The higher build code enables the in-app updater to offer this build.

## Checks

GitHub Actions run unit tests, desktop UI checks and verify the signed APK package and version. Please confirm on Android Auto that `Ievietot rindā` leaves the current song playing and places the selected song next in the shared queue.

Credits, GPLv3 license and source attribution remain in `LICENSE`, `NOTICE` and `SOURCE.md`.
