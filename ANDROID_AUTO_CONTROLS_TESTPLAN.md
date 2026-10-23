# Sprotify Android Auto Now Playing controls — PR test plan

Beta 6 includes this Android Auto change set. Install the signed `1.0.0-beta.6` APK over Beta 5; retain the `lv.guntify.player` application ID.

## Existing media display

- Keep title, artist and cover artwork sourced from the current Media3 MediaItem.
- Verify title/artist and correct cover after manual Next, radio continuation, seek, preloading, and reconnect.
- Android Auto owns the visual Now Playing template; it decides button positions and overflow.

## New media actions

- With an album/playlist playing, verify **Play Radio** is available on Now Playing or overflow. Press it once; the current audio should not restart and phone Radio should be active.
- Once Radio is active, look for the active radio icon / **Radio playing** label. Press it again: no unintended song restart or queue reset.
- Toggle **Shuffle** twice. Verify the car control and phone's shuffle state agree and that disabling shuffle restores the list order.
- Toggle **Repeat** twice. Verify the phone repeat state agrees and the current track repeats when Repeat is enabled.
- Toggle **Add to Liked Songs** on a Spotify track; confirm local phone Liked Songs and Spotify account are updated. Toggle it off and confirm removal. Test a local track without a Spotify catalog ID: it should save locally without trying to sync a blank ID.
- Change Radio, Shuffle, Repeat, or Like in the phone app while connected to Android Auto; confirm the car updates its button label/icon. The Like action listens for the same preferences changes used by the phone.
- If the head unit only has space for one or two actions, check the Android Auto overflow menu for remaining actions. Do not assume four simultaneous onscreen icons.

## Regression

- Rapid Next in an album and Radio, using both car controls and steering wheel: no unexpected navigation back to browse root.
- Open the Android Auto Queue, play to the final radio track and let it extend. No empty library or lost Now Playing.
- Pause/resume, disconnect car, force-close phone UI and reconnect; session/position must recover.
- Verify GitHub Actions Android debug build, unit tests, release lint, and signed-release verification.

## Not included

- This document is a post-release Android Auto smoke-test plan; Beta 6 is a signed update.
- No Car App Library UI rewrite, Artists browser, streaming codec/bitrate text, or personalized Home redesign.
