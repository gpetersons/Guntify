# Corresponding source: Sprotify

Repository: https://github.com/gpetersons/Sprotify
Releases: https://github.com/gpetersons/Sprotify/releases
Upstream fork: https://github.com/rare621/USpot
Original: https://github.com/Spotui/Spotui

Sprotify is an independent GPLv3 modification of USpot / Spotui. Preserve LICENSE, NOTICE and third-party attributions when distributing.

Each GitHub release publishes the signed `Sprotify.apk`, matching `Sprotify-source.zip`, LICENSE, NOTICE, SOURCE.md, and SHA-256 checksums. The source archive is produced from the same checked-out Git commit as the APK.

## Build
Java 17, Android SDK 34, Build Tools 35.0.0 and included Gradle wrapper:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :spotify:test --no-daemon --max-workers=2
```

## Independent CI
`.github/workflows/sprotify-build.yml` checks builds on pull requests and pushes, and uploads an unsigned-for-release debug APK artifact.
`.github/workflows/sprotify-release.yml` publishes a signed beta after a reviewed version increment is merged to **main**, and can also be triggered manually from main. It requires signing secrets named `GUNTIFY_KEYSTORE_BASE64`, `GUNTIFY_STORE_PASSWORD`, `GUNTIFY_KEY_ALIAS` and `GUNTIFY_KEY_PASSWORD`, containing the existing Sprotify signing key (originally configured for Guntify). Back up that keystore securely for all future releases. No USpot signing key or deployment is used.

Releases use `v1.0.0-beta.N` prerelease tags. The in-app updater recognizes signed beta releases and also supports stable `v<versionName>-build-<versionCode>` tags.

This release does not claim to include separate Desktop Mode security-hardening work. No independent complete network security audit is claimed.

Historical Beta 4 release files retain their published names: `SprotiFy.apk` and `SprotiFy-source.zip`. The Android ID and the `GUNTIFY_*` signing secret identifiers are intentionally retained for upgrade compatibility.
