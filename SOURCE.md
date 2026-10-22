# Corresponding source — USpot 1.1, build 71

This repository contains the current source directly. The workflow builds the checkout on pushes to main or when run manually from Actions; no versioned ZIP extraction is needed.

Each published release includes **USpot.apk**, **USpot-source.zip**, **LICENSE**, **NOTICE** and this file. The source ZIP is created with `git archive` from the same commit that built the APK. Keep that exact source with each APK you redistribute.

Repository: https://github.com/rare621/USpot
Matching release: https://github.com/rare621/USpot/releases/tag/v1.1-build-71

## Build requirements

- Java 17.
- Android SDK platform 34 and build-tools 35.0.0.
- Gradle 8.11.1 from the included wrapper; dependencies resolve from the configured repositories.

Run `./gradlew :app:assembleDebug --no-daemon --max-workers=2` and `./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2`.

The CI workflow restores the existing private key from `USPOT_SIGNING_KEYSTORE_BASE64`. The existing optional `USPOT_STORE_PASSWORD`, `USPOT_KEY_ALIAS` and `USPOT_KEY_PASSWORD` secrets retain their previous meaning/defaults. Do not generate a replacement key for an update to existing installations. Local builds use the usual debug key unless configured otherwise.

The application ID remains `com.music.spotui.modified`, with versionName `1.1` and versionCode `71`. New releases must increase versionCode. The workflow publishes a versioned release only after assembly and unit tests pass; an existing release tag is retained rather than overwritten.

The original GPL version 3 text and third-party source notices are retained. Original project: https://github.com/Spotui/Spotui .
