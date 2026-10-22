# Build 74 validation and limits

## Completed locally before publication

- Debug Desktop Preview APK assembled successfully from the build 74 runtime source.
- All 319 app unit tests across 44 suites passed, with zero failures, errors or skips.
- Simulated browser/phone interaction checks passed, including queue navigation, likes and mutation errors, Radio tuning without restart, 30-second listening qualification, failed-preload recovery and retry for ambiguous browser media error code 4.
- New proof-binding regression tests check distinct per-video player proofs within one session and failure without substituting the session proof.
- Preview package/version, v2 signature, retained preview signing certificate and bundled browser/source equality were verified.

## Required by the public release workflow

- Rebuild regular USpot with the existing production signing key; do not use the preview package suffix.
- Run app unit tests and browser interaction checks again on GitHub Actions.
- Verify the APK signature and compare its certificate with the previous public USpot release.
- Verify application ID `com.music.spotui.modified` and versionCode 74 before creating the latest release.
- Package corresponding source from the exact build commit, with LICENSE, NOTICE and SOURCE.md.

## Device testing still required

Real YouTube/account playback, browser audio and codecs, phone-to-PC Wi-Fi transport, background survival, actual restore operations, Bluetooth and Android Auto were not tested on physical devices in this environment. The browser interface remains experimental. Automated checks do not establish that every upstream HTTP 403 error or every Spotify account operation is resolved.

Browser listening writes to the hosting app's local history after 30 seconds of audible time. Data belongs to that Android installation; the regular and Desktop Preview apps have separate data. Browser queue, volume and EQ remain independent of the phone player. Refresh may be needed to see data changed on the other screen.
