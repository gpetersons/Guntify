# Sprotify Beta 8 Android Auto smoke tests

1. Check `versionCode 8` and `versionName 1.0.0-beta.8`; Sprotify installs over Beta 7 without losing local saved music.
2. In an ordinary playlist, tap Shuffle: OFF -> ON (dot) -> Smart Shuffle (dot + star) -> OFF. A Smart Shuffle selection should request recommendations as the phone does. Check the phone indicator changes too.
3. Change Shuffle on the phone and return to Android Auto; the car button should reflect all three modes. In Radio and Downloads the phone intentionally permits only OFF/ON.
4. Verify the current song/artist/artwork, Radio action, Favorite, Repeat and Next. Quickly tap Next without losing the browse view.
5. On a Car App API 8+ host, open the new MediaPlaybackTemplate: top bar shows the current station/playlist/album/Liked Songs source, not the playing song. The top-right queue action has **an icon only**.
6. Change Radio/playlist from the phone: the title in the template header updates. Open its queue to read the same shared player list.
7. On a host using the legacy Android Auto media screen, verify playback still works. Google may retain its built-in 'Ievietot rindā' label and original Now Playing header because those are not controllable with Media3.
8. Disconnect/reconnect and confirm playback position/session recovery.
