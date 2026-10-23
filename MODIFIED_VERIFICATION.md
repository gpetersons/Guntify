# Build 76 checks and device limits

The release workflow must compile the Android app, pass app and Spotify-module unit tests, pass simulated browser checks and verify the existing production signing certificate, package and build before publication.

Playlist contract tests check the Web Player changes endpoint, adding real Spotify track URIs, removing a specific playlist occurrence using its UID, refusing missing UIDs, one request on HTTP 429 and rejecting error responses. Library mutations use the currently observed Web Player document hash with the former hash retained as an explicit fallback.

New screens reuse the existing Spotify authentication, repositories, player, download controls and song menu. Playlists can only be edited when the current account owns them. Library removal unfollows a playlist; it does not delete playlist contents.

No live Spotify-account mutation or physical audio-route/installer test is available in this environment. Bluetooth names, available routes and output controls depend on Android and the manufacturer. Android 14+ can open the system output switcher; older devices use supported routing or Bluetooth settings. Actual account sync and device switching must be confirmed on a device.

Desktop browser checks simulate the phone and audio. Real streaming, car controls, background survival and Wi-Fi playback require device testing.

## Primary protocol references inspected

- Spotify Web Player adapter and captured requests: https://github.com/imoonkey/openweb/blob/main/src/sites/spotify/adapters/spotify-pathfinder.ts
- Android MediaRouter2: https://developer.android.com/reference/android/media/MediaRouter2
- Android legacy MediaRouter: https://developer.android.com/reference/android/media/MediaRouter

Existing licenses and attribution notices remain with their source. No upstream source file was copied into this project.
