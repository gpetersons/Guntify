# Android Auto P1 + P2 acceptance tests

These changes are based on the working signed Beta 2. Do not use the old closed Beta 3 PR #6.

## P1 Radio information and playback
1. Start a song on the phone; select Start Radio without restarting the current audio.
2. Open Sprotify in Android Auto. The first Home folder should say **Playing: [station] Radio**.
3. Now Playing must keep the actual song title and artist; the Media3 album field should include the Radio name when a new stream is prepared (Android Auto may hide that field).
4. Browse **Playing: [station] Radio** and verify the tracks match the phone's live queue. Skip several songs and return, then start a different station on the phone and reopen the folder. It must show the new queue, not the previous station.
5. Run with weak network and at end of radio queue. Prefetch must not interrupt audio or cause duplicate playback requests; no promise of zero silence if stream resolution is slow.

## P2 personalized browsing
1. Browse **Continue listening**. The current song is first, followed by distinct actual listening-history tracks. Starting a track should play it.
2. Browse **Recommended for you**. The content should originate from the same authenticated/cached Spotify home data as the phone. Open albums, playlists and artists and play a track.
3. Browse **Made for you**, reflecting Spotify Mix / Made for you sections where they exist. If Spotify doesn't provide these sections, it may be empty.
4. Browse **Recently played** and **Your radios**. Saved local Radio stations should be browseable even if not yet listened to for 30 seconds.
5. Confirm **Liked Songs** and **Library** still work.
6. Return to Home after changing the active Radio on the phone; label and live queue should reflect the current station.

## Regression checks
- Rapid Next from album and native Now Playing must not pop navigation back to root.
- Native Android Auto Queue (the built-in "Ievietot rindā" panel) must continue to work as in Beta 2.
- Radio action must remain visible in native Now Playing and start Radio without replaying the track.
- Turning car/Sprotify off and back on should resume the saved track/position.
- On slow/offline Spotify, recently cached Home data should remain available; no hanging browse request.

P1/P2 are not replacements for the Media3/Android Auto native Now Playing UI. Android Auto may choose which standard metadata to display.
