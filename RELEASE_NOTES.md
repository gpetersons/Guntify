# Sprotify 1.0.0 Beta 9 — Android Auto Queue and Search

## New in Beta 9

### Show the real playback queue
- Android Auto's MediaSession is given a **read-only timeline projection** of Sprotify's in-app queue instead of just the one resolved ExoPlayer stream.
- Queue rows include the song title, performer, cover art and current playlist/radio context. The active index is taken from the same queue and song ID used on the phone.
- Changing or expanding the queue notifies Android Auto without reloading unrelated album/radio browse screens. Selecting a different queued track uses Sprotify's established stream-resolution flow, not an expiring URL.
- The familiar Android Auto **Ievietot rindā** button and stable Beta 8 media player are retained. Android Auto chooses the exact **Now Playing / Next From** headings and queue scroll layout.

### Search: Play now or Add to queue
- Tapping a result returned by **Android Auto's browsable search** opens two actions: **Atskaņot tagad** and **Pievienot rindai**.
- **Pievienot rindai** calls the same `CurrentSongState.addToQueue(song)` path as the phone. It inserts a track after the current item, without replacing the playlist or intentionally starting the selected song.
- **Atskaņot tagad** preserves immediate playback for people who explicitly choose it.
- Voice-command **Play from search** still starts playback, as expected when the driver asks to play a track.
- Some Android Auto hosts may not present all browser search result/action affordances; verify on the head unit.

## Unchanged
- Existing Beta 8 player, Radio, Shuffle OFF/ON/SMART, Repeat, Favorite, covers, current title/artist, stream preparation, playback restoration, and Android Auto browse tree.
- The queue button text is unchanged because its label belongs to Android Auto.
- **No Car App API 8**, new templated media screen, or additional playback engine.

## Install and verification
- Signed `Sprotify.apk`, package `lv.guntify.player`.
- Version `1.0.0-beta.9` / `versionCode=9`.
- Install over the existing signed Beta 8 without uninstalling, to preserve user data.
- CI validates Kotlin compilation, unit tests, signed release identity and APK metadata.
- Test the **real car**: open queue during playback, check multiple tracks, current/next indication, select a later queue item, use search > Add, and verify the current song does not restart.

**Limitation:** The MediaSession timeline projection primarily covers the usual ExoPlayer audio path. Web-player-specific rendering and the host's section titles/menus remain Android Auto-dependent; a successful build is not a physical compatibility guarantee.
