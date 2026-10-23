# Sprotify 1.0.0 Beta 7 — Android Auto artwork and artist subtitle

## What's new

- **Artist under the song title:** Media3 now includes the artist in `artist`, `subtitle` and `description` and explicitly sets `displayTitle`. Applied to the ExoPlayer stream, Spotify Web player and Android Auto browsed songs. Some head units may still choose which metadata lines to show.
- **Favorite button:** outlined plus in a circle for unsaved songs; a filled circle with an outlined-out checkmark cutout when saved to Liked Songs. Actual tint is supplied by Android Auto.
- **Radio button:** compact broadcast/signal-wave icon. Removed the unreliable active/inactive Radio icon distinction; playback mode and station remain available through the playing queue and the phone UI. Tapping Play Radio while already in Radio remains non-destructive.
- **Shuffle:** crossing arrows icon, with a small indicator for the enabled state.
- **Repeat:** rounded-square loop arrow icon, with an enabled-state indicator dot.
- **Stability:** retains Beta 6's functional Radio, Shuffle, Repeat, Favorite, queue and audio flow; only metadata and transport action artwork were modified.

## Installation

- Package: `lv.guntify.player` (unchanged).
- Version: `1.0.0-beta.7` / `versionCode=7`.
- Signed release asset: **Sprotify.apk**. Source bundle: **Sprotify-source.zip**.
- Install **over Beta 6** with the same signing identity. Do not uninstall your current version; keep local data.

## Verification and limitations

- GitHub Actions verifies Android build, unit tests and the APK signing identity.
- Physical Android Auto testing is still required to verify the head unit renders the new icons and artist subtitle, including Spotify Web playback, next-track transitions, Radio, Shuffle, Repeat, Favorites and reconnects.
- Android Auto controls the layout, placement and tint of custom buttons; pixel-exact visuals cannot be guaranteed.
- **Sprotify** uses a plain S, not Š.

Credits, licensing and source disclosures remain in `LICENSE`, `NOTICE`, and `SOURCE.md`.
