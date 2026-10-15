# USpot

USpot is rare621’s modified version of Spotui for Android, built with Jetpack Compose.

Original app credit: [Spotui creators and contributors](https://github.com/Spotui/Spotui). USpot is a modified fork released under **GNU GPL version 3**. See [LICENSE](LICENSE), [NOTICE](NOTICE) and [SOURCE.md](SOURCE.md) for the license, dated changes, credits and source/build information. See MODIFIED_VERIFICATION.md for testing limitations and update-signing requirements.

It connects to your real Spotify account and mirrors the Spotify experience:

- 🎵 **Playlists** — browse, play, add/remove tracks, create new playlists, all synced with your Spotify account
- 📝 **Lyrics** — Spotify's own synced lyrics, with a live preview on the player and a full-screen view
- 📻 **Spotify recommendations** — USpot Radio continues with recommendations from the existing Spotify integration; ordinary collections loop, with optional Smart Shuffle additions
- ❤️ Liked songs, followed artists, listening history and downloads (including lossless FLAC)

## Screenshot

<img src="assets/screenshot-player.png" width="360" alt="Now playing screen"/>

## Credits

spotui builds on the work of several open-source projects:

- [Meld](https://github.com/) — Spotify metadata + YouTube streaming layer
- [Neptune](https://github.com/navneet851/spotify-clone-jetpack-compose) — the original Jetpack Compose Spotify clone this app started from
- [SpotiFLAC](https://github.com/spotbye/SpotiFLAC) — lossless (FLAC) track resolving
- [SimpMusic](https://github.com/maxrave-dev/SimpMusic) — crossfade / DJ-style audio filter processing

## Disclaimer

This project is for educational purposes only. Spotify is a trademark of Spotify AB.

## Sharing an APK

Distribute the matching source ZIP with every APK, keep all notices and license files, and provide a clear source link on the download page. The GitHub workflow packages APK, source, LICENSE, NOTICE and SOURCE.md together. Preserve these files when publishing a release; do not rely solely on expiring Actions artifacts.

## USpot build 42

Adds a configurable top-song Smart Downloads set (20 by default), a local Stats tab, existing-playlist/album discoveries for Made For, popularity-based relevant Radio seeds, queue Undo, Radio tuning/exclusions and diamond Smart Shuffle. Smart Shuffle changes the session queue, not the saved Spotify playlist. USpot Radio is a continuous generated queue; Spotify Radio collections retain their playlist behavior.

Smart Downloads updates while the process is available and resumes on launch. Manual downloads are protected; old automatic files remain until replacements are ready. See the matching build notes for validation and account/device testing limits.
