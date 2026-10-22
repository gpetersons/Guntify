<p align="center">
  <img src="app/src/main/res/drawable-nodpi/uspot_logo.png" width="140" alt="USpot logo" />
</p>

<h1 align="center">USpot</h1>

<p align="center">🎶 Your music. Your listening habits. Your radio.</p>

<p align="center">
  <strong><a href="https://github.com/rare621/USpot/releases/latest/download/USpot.apk">⬇️ Download USpot for Android</a></strong>
  · <a href="https://github.com/rare621/USpot/releases/latest">What's new</a>
  · <a href="https://github.com/rare621/USpot/releases/latest/download/USpot-source.zip">Source code</a>
</p>

**Version 1.1 · build 74 · Android 8.0 or newer**

USpot builds on [Spotui](https://github.com/Spotui/Spotui), adding a Home that learns from your listening, saved radio stations, radio mixes and more ways to make the player your own. Spotify supplies catalog and account information; music playback uses the existing YouTube and alternative-source integrations.

## 📸 Take a look around

A little discovery, a little radio, and a whole lot of your music. 🎧

<table>
  <tr>
    <td align="center"><strong>🏠 Your Home</strong><br /><sub>Your rotation, recent plays and discoveries.</sub><br /><br /><img src="assets/screenshots/home.png" width="240" alt="USpot Home with rotation shortcuts and recently played songs; profile image removed" /></td>
    <td align="center"><strong>🔎 Find your next favorite</strong><br /><sub>Search the catalog or browse a mood.</sub><br /><br /><img src="assets/screenshots/search.png" width="240" alt="USpot Search and music browse categories" /></td>
  </tr>
  <tr>
    <td align="center"><strong>📚 Keep it all together</strong><br /><sub>Playlists, radios, likes and downloads.</sub><br /><br /><img src="assets/screenshots/library.png" width="240" alt="USpot Library with saved collections; personal name and profile image removed" /></td>
    <td align="center"><strong>🎵 Let it play</strong><br /><sub>Full-screen artwork and your radio controls.</sub><br /><br /><img src="assets/screenshots/player.png" width="240" alt="USpot full-screen player playing from a saved radio" /></td>
  </tr>
  <tr>
    <td colspan="2" align="center"><strong>⚙️ Make it yours</strong><br /><sub>Sound, appearance, Smart Downloads and backups.</sub><br /><br /><img src="assets/screenshots/settings.png" width="240" alt="USpot Settings menu; personal name and profile image removed" /></td>
  </tr>
</table>

<sub>Screenshots edited to remove personal names and profile pictures for privacy.</sub>

## 🚀 Get started

1. **Download and install** the APK using the button above. Android may ask you to allow installation from your browser.
2. **Sign in to Spotify** to load the account content available to the app.
3. **Play your music.** Home starts with available Spotify content and gradually learns what you enjoy in USpot.

Already using USpot? Install the new APK **over your current app**. Export a backup in Settings first, and avoid uninstalling if you want to keep your local data. Updates require the same signing key; official builds keep using the repository's existing key.

Downloads become available when the build and unit tests pass and the release is published.

## ✨ What USpot adds to Spotui

These are additions and refinements in this fork. Spotify search, playlists, likes, lyrics, followed artists, history, downloads and recommendation-based autoplay already formed part of Spotui's foundation.

| Area | What's added or improved in USpot |
| --- | --- |
| **Personalized Home** | Listening-based albums and recommended Spotify playlists, a personalized Made for section, artist variety and saved Home content that appears while fresh recommendations load. |
| **Your rotation** | Eight collection shortcuts for playlists, albums and radios, with separate recently played songs and local Home removal controls. |
| **USpot Radio** | Saved stations with their own name, artwork, queue and playback progress; continuous queue filling and a radio-only refresh control. |
| **Radio Mix** | Blend up to **five songs** into one station. Choose songs together using Spotify search, your playlists or Liked Songs. |
| **Radio tuning** | **Stay close**, **Balanced** and **Explore more**, plus **Less like this** and **Don't play this artist** controls for Radio. |
| **Less repetition** | Recent-play weighting and saved station sessions help avoid replaying the same sequences, while still allowing familiar songs to return. |
| **Following tab** | A dedicated place for new releases, popular tracks and a combined selection from artists you follow. |
| **Stats and History** | Local listening statistics, top songs and artists, listening history and playable tracks. |
| **Smarter shuffle** | Playback-context indicators and a diamond Smart Shuffle option that can add relevant songs to a collection's queue. |
| **Queue controls** | Manually queued songs go ahead of the remaining automatic queue, an added-to-queue message, per-song actions and an animated now-playing indicator. |
| **Library refinements** | Swipe-to-remove confirmations, playlist creation and song suggestions, and artwork collages for playlist covers. Spotify operations remain subject to account/service support. |
| **Smart Downloads** | Optional automatic downloads based on your listening. **Off by default**; starts with a target of **20 songs** when you enable it, adjustable in Settings. |
| **Equalizer** | On/off controls, presets and manual sound adjustment connected to the player's audio session. |
| **Appearance** | USpot branding, refreshed Home/Library/player/menu designs, playing-context highlights, and light/dark appearance that can follow your device. |
| **Desktop & Browser** | Optional Wi-Fi browser interface: browse and listen on your computer using the phone as the backend. Shares local history and saved data; browser queue, volume and EQ stay separate. |
| **Backup and restore** | Export and import local app data from Settings. |
| **Playback resilience** | Upcoming-stream preparation, retry/backoff handling, recovery controls and sanitized local playback diagnostics. |
| **Android Auto** | Expanded Recent and Recent radios browsing, plus media-search/voice-request handling. Availability depends on Android, the car and the assistant. |
| **Project updates** | Current source directly in the repository, one build workflow, and APK releases accompanied by their matching source and notices. |

These features are present in the source. Build/test status and account/device limitations are explained below; this list is not a claim that every feature has been tested on every device.

## 🧠 A Home that gets to know you

A song needs **30 seconds of audible listening** before it influences local listening features. Skipping quickly through tracks does not train Home the same way as listening to them.

Home uses up to **15 top songs and five top artists**. The default window is **30 days**, with extra weight on the last week, so changing preferences can show up over time. Settings let you adjust personalization strength up to **100%**. Catalog availability and variety rules also affect the cards you see.

New users start with the Spotify content available to their account. As qualified local history grows, USpot uses it to find and rank relevant catalog content. USpot's listening history does **not** train Spotify's own mixes or recommendations.

## 📻 Find your radio vibe

**Spotify radio entries** are catalog playlists: open one and you get its track collection. Spotui already included Spotify recommendation-based autoplay.

**USpot Radio** is a saved station whose queue the app generates and extends. Start with one song, or choose up to five for **Radio Mix**. The station can combine its original songs with changing playback context as you listen. Stay close limits exploration; Balanced and Explore more allow broader influence. Similarity is not a guarantee that every song stays in one genre.

Radio memory is bounded to **90 days**. Recent songs are less likely to return immediately, rather than being permanently blocked.

## 🖥️ Listen from your computer

<p align="center">
  <img src="assets/screenshots/desktop-browser.png" width="100%" alt="Early USpot desktop browser preview showing Home, Your rotation, the player and upcoming queue" />
</p>

<sub>Early desktop preview supplied by a user. The interface is still being refined; some layout details differ in the current build.</sub>

Open **Settings → Desktop & Browser → Start desktop mode** on your phone, then enter the displayed `http://...:8765/` address in your computer's browser. Keep both devices on the same trusted router; Ethernet on the computer works too.

Audio plays on the computer. Qualified browser listening updates the connected app's local history, Stats and personalization. Likes and saved radio data use the phone's integrations; playback queues, volume, EQ and some browser preferences remain separate. Refresh a page to pick up data changed on the other screen.

This is an **optional experimental interface**. The phone must stay connected and running in the background. Anyone on the same Wi-Fi can connect while desktop mode is running, so use trusted networks and stop it when finished. Don't expose the address or port to the internet. Some formats depend on browser support, and desktop layouts are still being refined.

Regular USpot and USpot Desktop Preview are separate Android installations. Browser changes belong to whichever installation is hosting the connection; they do not automatically cross between the two apps.

## 🔒 Your data stays yours

Local listening records and personalization are stored on your device. The app can read authorized Spotify history and make catalog/recommendation requests, but it **does not upload local USpot plays to influence Spotify's listening profile**. Online features still send the requests needed by Spotify, YouTube and any selected stream providers.

Use **Settings → export/import** to keep a backup. Uninstalling can erase local data; retaining the application ID and signing key is important for in-place updates.

## 🛠️ Status and help

A published build confirms that APK assembly and unit tests passed. It does not confirm every Spotify account operation, Bluetooth device or Android Auto setup. Intermittent stream-lookup failures remain under investigation; a restart improving playback does not establish their root cause.

For a problem report, include the build number, Android version, what you were playing and sanitized playback diagnostics. Never share passwords, tokens, signing keys or private stream URLs.

[Release notes](RELEASE_NOTES.md) · [Validation details](MODIFIED_VERIFICATION.md) · [Build status](https://github.com/rare621/USpot/actions/workflows/build-spotui-modified.yml)

<details>
<summary><strong>For developers: build USpot</strong></summary>

The repository contains the current source directly. You do not need to upload or extract an old source ZIP to build it.

Use **Java 17**, **Android SDK platform 34** and **build-tools 35.0.0**, with the included Gradle wrapper:

```sh
chmod +x gradlew
./gradlew :app:assembleDebug --no-daemon --max-workers=2
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2
```

The [workflow](.github/workflows/build-spotui-modified.yml) runs on pushes to `main` and manual runs. After successful checks it publishes **USpot.apk**, **USpot-source.zip**, **LICENSE**, **NOTICE** and **SOURCE.md**. Source archives come from the same commit used for the APK.

The application ID remains `com.music.spotui.modified`. Existing installations need the same signing key and a higher versionCode to update; build 74 uses versionName `1.1` and versionCode `74`. Never commit private signing material or replace the official key just to make a build succeed. Local debug builds may use a different key.

See [SOURCE.md](SOURCE.md) for signing configuration and corresponding-source information. Older source versions remain in Git history.

</details>

## 💚 Thank you, Spotui

**Thank you to Spotui's original creators and contributors for making such a good app and sharing it as open source.** Your work gave USpot its foundation: the player, Spotify integrations, lyrics, downloads and the Compose interface that made these additions possible.

USpot also retains credits for **Meld**, **Neptune**, **SpotiFLAC**, **SimpMusic** and the other projects and dependencies acknowledged in [NOTICE](NOTICE) and the source files.

USpot is a modified fork distributed under [GNU GPL version 3](LICENSE). Keep the license, attribution notices and corresponding source with redistributed builds. USpot is not affiliated with or endorsed by Spotify or YouTube; the code license does not grant rights to their trademarks, services or music.
