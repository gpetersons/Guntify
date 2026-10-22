# USpot

A Spotify-inspired Android music player built from [Spotui](https://github.com/Spotui/Spotui), with listening-based personalization, continuous radio, offline downloads and Android Auto support.

**USpot 1.1 · build 70** · Android 8.0+

[Download APK](https://github.com/rare621/USpot/releases/latest/download/USpot.apk) · [Latest release](https://github.com/rare621/USpot/releases/latest) · [Complete source ZIP](https://github.com/rare621/USpot/releases/latest/download/USpot-source.zip) · [Build status](https://github.com/rare621/USpot/actions/workflows/build-spotui-modified.yml)

Downloads appear after a successful build, unit-test run and release publication. The files in this repository are the current integrated source; old packages remain available through Git history.

## Your music, your Home

- Spotify catalog search with filters for songs, albums, artists and playlists.
- A compact Home with eight collection shortcuts, separate recent songs, personalized albums and recommended Spotify playlists.
- Spotify content provides the starting point for new users. Qualified listening in USpot gradually informs local recommendations, with configurable personalization strength up to 100%.
- Local taste selection uses up to **15 top songs and five top artists**, with a default 30-day window and extra weight on the last week. A play must reach **30 seconds of audible listening** to qualify.
- Following brings together releases and popular tracks from followed artists. Stats and History show local listening activity with playable tracks.
- Cached Home content helps preserve your personalized layout while fresh content loads.

Catalog coverage, available account data and recommendation results vary. Personalization changes the selection and ranking of content; it does not create new Spotify-owned mixes or train Spotify's recommendation system.

## USpot Radio and Radio Mix

Create a station from one song, or blend **up to five songs** into a Radio Mix. Saved stations keep their identity, playback progress and queue state locally.

- Continuous recommendation-based queues, radio-only refresh and Stay close / Balanced / Explore more tuning.
- Original-song context combined with changing playback context as a station develops.
- Recent-play weighting reduces repeated sequences without permanently banning familiar songs; radio memory is bounded to 90 days.
- Queue controls, song actions, shuffle and relevant Smart Shuffle additions.
- Radio creation supports Spotify song search and selection from your playlists and Liked Songs.

**USpot Radio is different from a Spotify radio playlist.** Spotify catalog radio entries open as collections of tracks; USpot stations generate and extend their own queues using the recommendation sources available to this app. They are not Spotify's official live radio service.

## Library, playback and offline listening

- Spotify account/profile information, library browsing, playlist creation and saved-song actions through the existing integrations, where supported by the account and service.
- Manual queue additions play ahead of the remaining automatic queue, with an added-to-queue message.
- Stream preparation for upcoming tracks, bounded lookup retries, recovery backoff and local playback diagnostics.
- Offline downloads and **optional Smart Downloads**, disabled by default. When enabled, the default target is 20 songs and can be changed in Settings.
- Equalizer presets and manual adjustment, plus existing audio settings.
- Full-screen player, animated playing indicators and playback-context highlighting.
- Android Auto browsing, recent-radio entries and media search/voice-request handling. Bluetooth resume depends on Android, the vehicle and the app's service state.

## Appearance and your data

Follow the device theme or choose light/dark appearance manually. Settings also include personalization controls, downloads, backup export/import and diagnostics.

Local listening records are kept on the device. The app can read authorized Spotify history and use Spotify catalog queries for recommendations; it does **not submit local USpot plays to Spotify to influence Spotify recommendations**. Signing in and using online features still sends the requests required by the respective services.

## Install or update

1. Open the [latest release](https://github.com/rare621/USpot/releases/latest) and download **USpot.apk**.
2. Allow installation from your browser or file manager when Android asks.
3. Install over your existing USpot installation when possible.

The application ID remains `com.music.spotui.modified`. An in-place update requires the **same signing key** and a higher internal versionCode. This build uses versionCode 70. Back up your data in Settings before changing installations; uninstalling can erase local history and settings. Do not replace the saved signing key to resolve a build error.

## Build from source

Use Java 17, Android SDK platform 34 and build-tools 35.0.0. The Gradle wrapper is included.

```sh
chmod +x gradlew
./gradlew :app:assembleDebug --no-daemon --max-workers=2
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2
```

The [GitHub Actions workflow](.github/workflows/build-spotui-modified.yml) builds the checked-out source on pushes to `main` or a manual run, runs unit tests and publishes **USpot.apk** with the exact corresponding **USpot-source.zip**, license and notices. No versioned source ZIP needs to be uploaded for future code changes.

Official updates require the repository's existing signing secrets. Local debug builds normally use your local debug key and may not update an officially signed installation. See [SOURCE.md](SOURCE.md).

## Status and reporting problems

This is a community fork. Features listed above are present in the source; a successful release confirms APK assembly and unit tests, not every account/device scenario. See [validation notes](MODIFIED_VERIFICATION.md) and [current release notes](RELEASE_NOTES.md).

Intermittent stream-lookup failures remain under investigation. A network reconnection or restart can change the behavior, but the root cause has not been conclusively identified. Spotify account mutations, YouTube source availability, Bluetooth recovery and Android Auto voice routing need testing with the actual account and device. Voice commands must target an app Android recognizes; USpot cannot impersonate Spotify.

When [reporting an issue](https://github.com/rare621/USpot/issues), include the app build, Android version, what you were playing and sanitized playback diagnostics. Do not post credentials, tokens, signing keys or private stream URLs.

## Credits and license

USpot is a modified fork of **Spotui**, credited to its original authors and contributors, distributed under [GNU GPL version 3](LICENSE). Credits for Meld, Neptune, SpotiFLAC, SimpMusic and imported dependencies are retained in [NOTICE](NOTICE) and the source files. Each APK release includes corresponding source and license information.

USpot is not affiliated with or endorsed by Spotify or YouTube. Their names, services and content remain subject to their respective rights and terms. The source-code license does not grant rights to music or third-party trademarks.
