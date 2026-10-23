# Guntify 1.0.0 Beta 2: Android Auto improvements

This beta contains the P0 and P1 Android Auto improvements from Pull Request #3.

- **Start Radio** action for the currently playing song in Android Auto, with a dedicated icon. Depending on the head unit, the action may appear under the overflow menu.
- The phone and Android Auto Start Radio actions now use the same radio playback controller.
- Keep the current media item in the playback session while the next stream is being resolved, reducing unexpected Now Playing resets.
- Improved recovery when a radio track cannot be played and new recommendations are available.
- Additional tests and an Android Auto validation checklist in `ANDROID_AUTO_P0_P1_TESTPLAN.md`.

## Updating from Beta 1

Beta 2 keeps the application ID `lv.guntify.player` and uses the existing Guntify release signing identity. Its Android `versionCode` is **2** (Beta 1 was 1). Install the signed `Guntify.apk` from this release over Beta 1 rather than uninstalling Beta 1 first.

Your existing app data should normally be retained when updating in place. Back up important settings before installing a beta. If Android rejects the update due to a signature mismatch, **do not uninstall Beta 1**; report the error first.

## Scope and testing

This release includes **P0 and P1 only**. The redesigned Android Auto Home and recommendations UI (P2) is deferred.

Automated tests are not a replacement for physical Android Auto testing: check Radio, next/previous, queue refresh and unavailable tracks on your car display.

**Security note:** A full independent security audit and real-device network traffic assessment have not been completed.

**Acknowledgments:** [Spotui](https://github.com/Spotui/Spotui) and [USpot](https://github.com/rare621/USpot). Original GPLv3 copyright and attribution notices remain in LICENSE and NOTICE.
