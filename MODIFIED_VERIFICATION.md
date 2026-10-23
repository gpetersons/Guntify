# USpot build 82 validation

The production package is `com.music.spotui.modified`, version 1.1, code 82. The release workflow assembles and tests Android and browser code, verifies the existing production certificate and APK v2 signature, and publishes only after all release assets are uploaded.

The changes were compiled and checked in the separate preview before promotion. See [POLISH_CHANGES.md](POLISH_CHANGES.md) for implementation detail and test limitations. A local build of the production identity is required as well; automated tests and browser checks cannot establish real-device animation frame rates, streaming, Spotify-account access, Bluetooth or Android Auto behavior.

History, accounts and settings stay in the regular app’s existing data directory. Backup imports retain compatibility with USpot family IDs. Original integrations, licenses and notices are preserved.
