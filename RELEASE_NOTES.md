# Sprotify 1.0.0 Beta 13

## Android Auto

- Adds icons to Home and Browse categories and the library rows that previously showed missing-artwork placeholders.
- Library now lists Playlists, Albums and Artists vertically. Liked Songs and Downloads remain available under Playlists. Followed artists open by their Spotify ID.
- Publishes the shared logical song queue through a stable Media3 session player. The native queue can receive current and upcoming songs instead of only the engine's one or two resolved streams. Selecting a queue item uses the existing SongPlayer resolver; adding songs does not start playback. The host still controls headings and the queue button label.
- Updates selected-track metadata immediately while its stream resolves, rather than exposing the previous stream's artwork and position. Session artwork loading uses two workers, HTTP timeouts and a 512-pixel output bound.
- Handles dashboard suggested/recent root requests with playable listening-history items and retains a small restart-safe suggestion catalog. Cached song IDs can also be recovered from the active queue/history.
- Remote voice search no longer holds the serial player-command future open. Pause, Stop or a newer selection cancel the pending search. Browse flow work moves off the session thread, has a timeout, and cancels when its request is cancelled.

## Validation and limits

Regression tests cover the production session adapter's timeline/events, indexed queue selection, immediate metadata changes, engine swaps, audio-focus suppression and errors, plus suggestion persistence and browse policy. CI also runs the existing Android, Spotify and desktop UI suites and release lint.

No DHU or BYD device test has been performed here. These changes address code paths implicated by the reports; the exact cause of the reported UI freeze has not been reproduced. Please verify Search responsiveness/Back, changing artwork, dashboard suggestions, manual Add to queue, native queue selection, radio continuation and crossfade on the phone and car. Native host presentation may differ from Spotify.

## Installation

Version code 13, version name `1.0.0-beta.13`, package `lv.guntify.player`, existing signing identity. Install over Beta 12 without uninstalling. No Car App API 8 dependency is added.

Signed APK, source archive, checksums and license/attribution files are included. See `LICENSE`, `NOTICE` and `SOURCE.md`.
