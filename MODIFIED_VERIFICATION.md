# Build 75 validation and limits

## Passed on GitHub Actions

[Build run](https://github.com/rare621/USpot/actions/runs/39876036994), source commit `753104a2071efd1bce97a87a46bea0b9eb44cc41`:

- Android debug APK assembled successfully.
- App unit tests and Spotify module unit tests passed.
- Simulated browser interaction checks passed.
- Update-policy tests cover numeric builds, stable releases, trusted repository/asset URLs, required checksums, size bounds, and package/signature compatibility.
- Spotify Web Player session tests cover host restrictions, header validation, expiry and clearing the session.
- Production certificate, APK v2 signature, package `com.music.spotui.modified` and versionCode 75 verified before publication.
- Public APK and source ZIP were downloaded after publication; sizes, GitHub SHA-256 digests and ZIP integrity matched. The source ZIP contains build 75 and the updater code.

## Device/account testing still required

The Android update dialog and installer have not been exercised on a physical phone in this environment. Install build 75 once; later releases can trigger its update prompt. Android must approve installation, and may require allowing installs from USpot once.

Spotify writes now reuse a client-session header captured from the connected Web Player when available. No live Spotify account creation/removal test was performed. A successful compile does not establish that every upstream rejection is resolved.

Real YouTube playback, browser audio/codecs, Wi-Fi transport, background survival, restore, Bluetooth and Android Auto still need device testing. Browser checks use simulated phone/audio behavior. Desktop & Browser remains experimental.

Browser listening counts locally after 30 seconds of audible time. The browser queue, volume and EQ are independent of the phone player. Refresh may be needed to see data changed on the other screen.
