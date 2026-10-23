# Sprotify 1.0.0 Beta 6 - Android Auto Now Playing controls

## What's new

- **Radio button state:** Android Auto displays Play Radio when a normal album or playlist is playing, and a Radio playing label/active icon when a station is active. Pressing Radio again while already in Radio mode no longer restarts the current track.
- **Shuffle:** the Android Auto action toggles the same playback-queue shuffle used by the Sprotify phone player. The icon/label reflects the current state.
- **Repeat:** toggle Repeat on/off from Android Auto using the existing phone player repeat state.
- **Favorite:** add or remove the currently playing track from local Liked Songs, with Spotify-account synchronization when a Spotify track ID is available.
- **Phone-to-car control synchronization:** Radio, Shuffle, Repeat and Favorite controls update when their state changes in the phone app. Custom controls are republished only when relevant state changes to reduce Android Auto navigation disruption.
- **No regression to metadata:** existing song title, artist, cover artwork, queue and Play Radio playback flows are kept.
- **Branding:** the app and release are named **Sprotify**, with a plain Latin S. Retains the current sardine icon and repository `gpetersons/Sprotify`.

## Installation

- **Signed APK:** `Sprotify.apk`
- **Version:** `1.0.0-beta.6`; **versionCode:** `6`
- **Package:** `lv.guntify.player` (unchanged)
- Same signing identity as earlier signed releases. Install over Beta 5 to preserve application data. **Do not uninstall first.**
- The built-in updater uses the Sprotify repository.

## Android Auto limitations and testing

- Android Auto decides where additional playback actions are shown. Shuffle, Repeat and Favorite may be in the overflow menu; exact positions and colored icons are not guaranteed.
- Repeat is presently on/off (repeat current song), not three-state Off/All/One.
- Playback mode and liked state should remain synchronized with the phone. Test Radio, Shuffle, Repeat and Favorite on an actual head unit, including rapid Next and reconnection.
- See `ANDROID_AUTO_CONTROLS_TESTPLAN.md` for the smoke-test procedure.

## Notes

- The release build and unit tests run in GitHub Actions. Passing CI does not replace physical Android Auto testing.
- Security hardening lives in separate pull requests and is not claimed as part of this release.
- Credits, upstream acknowledgements and GPLv3 licensing remain in `NOTICE`, `SOURCE.md` and `LICENSE`.
