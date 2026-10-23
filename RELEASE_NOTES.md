# Sprotify 1.0.0 Beta 8 - Android Auto Smart Shuffle and context

## New
- Android Auto Shuffle cycles **OFF -> ON -> SMART -> OFF** using the same `CurrentSongState.cycleShuffle()` as the phone. For Radio and downloaded-local collections it follows the phone's two-state OFF/ON policy.
- The Shuffle button has a plain crossed-arrows icon for OFF, a dot for ON, and **dot + small star** for Smart Shuffle. Labels update when Shuffle is changed from either phone or car.
- Added an **optional Car App Library MediaPlaybackTemplate** experience for Car App API 8+ Android Auto hosts. Its header reads the actual current playback source (playlist name, album title, Liked Songs or source-track radio), and the top-right queue action has **only an icon, without a visible title**. Switching playback context on the phone updates the template title.
- The Car App Library queue screen uses the current shared phone/player queue. It shows song names and performers.
- Existing **MediaLibraryService** remains available as a fallback for hosts that do not support the newer media template.

## Installation
- Signed asset: `Sprotify.apk`
- Package: `lv.guntify.player` (unchanged).
- `versionName=1.0.0-beta.8`, `versionCode=8`.
- Same signing identity, so install **over Beta 7** to retain saved user data. Do not uninstall first.

## Limitations & tests
- On legacy Android Auto media templates, Google controls the header title and built-in queue button label. **Custom source header and icon-only queue require a host with Car App API 8+ support.** They cannot be guaranteed on all car systems.
- A new templated-media experience is experimental and has separate Play Store distribution rules. Verify it on the actual head unit; the legacy media player is still supported.
- Keep testing the existing transport buttons, radio continuation, steering wheel Next, queue and app resume in the car. The Car App Library-specific queue is currently read-only.
- Physical display and user interaction have not been tested by the CI runner.

Credits and licensing remain in `LICENSE`, `NOTICE` and `SOURCE.md`.
