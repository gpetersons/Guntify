# Guntify 1.0.0 Beta 3: Android Auto stability

Based on real-world Android Auto testing, this release targets rapid Next requests, unnecessary browse refreshes, and the missing Radio action.

- **Play Radio** is now explicitly enabled and advertised in the media session custom layout, including on controller connection. Android Auto decides its exact placement; some head units may still hide custom actions.
- **Rapid Next** in Android Auto coalesces short bursts of manual skips before launching expensive remote stream lookups, while keeping the latest selected track and canceling old requests.
- **Radio queue end** now has a single-flight guard: repeated Next and end events cannot concurrently launch duplicate radio continuation requests and skip multiple songs.
- **Browse refresh** messages are only sent when Recent/Recent radios rows actually change, not each time listening history is written.
- **Upcoming streams** get a best-effort background prefetch when fresh radio recommendations are added. Remote stream availability can still cause delays.
- New unit tests cover single-flight continuation, browse-change detection and manual-skip lookup delay.

## Updating from Beta 2

The app ID stays `lv.guntify.player` with the **same Guntify signing identity** and Android `versionCode=3`. Install this release's signed `Guntify.apk` directly over Beta 2 (`versionCode=2`) without uninstalling. Back up important settings before testing a beta.

If Android rejects the update due to a signature mismatch, **do not uninstall Beta 2**: report the error first.

## Android Auto manual acceptance checks

1. Rapidly tap Next in Radio and in an album, especially while the next track is still loading. Check whether the screen remains on Now Playing or the selected album.
2. Check the native Android Auto Queue screen during radio continuation and verify tracks reappear without losing playback.
3. Look for **Play Radio** on Now Playing (possibly overflow), start it and verify the current song does not restart.
4. Pause, disconnect the car and reconnect; the existing playback position and station should restore.

**Known limits:** Android Auto owns its system playback screen, so apps cannot force the position or visibility of every action. This release reduces likely causes of screen resets but actual head-unit validation is needed. P2 Home recommendations layout is not included.

**Security note:** A full independent security audit and real-device network traffic assessment are still pending.

**Acknowledgments:** [Spotui](https://github.com/Spotui/Spotui) and [USpot](https://github.com/rare621/USpot), original GPLv3 source and license notices preserved.
