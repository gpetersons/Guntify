# Android Auto P0/P1 test plan

This PR intentionally does not change Guntify's Android Auto home/library layout (P2).

## P0: retain Now Playing during async stream resolution

- Start music in Android Auto, open a playlist or track list, then skip to a track requiring a fresh stream lookup. The previous audio must pause and a stale stream must never play, but the active media session must not report an empty player timeline simply because the lookup started.
- Refresh the active Radio queue near the end of a track. Playback and the open Android Auto view should remain stable.
- Play a track with a deliberately unavailable stream; verify backoff and skip behavior and that the Now Playing view doesn't reset to the browse root. On the final radio track, requesting new suggestions must not destroy the visible playback session.
- Repeat with an available preloaded next item, a crossfade, and web player enabled, testing pause/play, next/previous and metadata.

## P1: Start Radio from current song

- In Android Auto Now Playing, choose **Start Radio** (on devices where there isn't room for a transport button, look in the overflow menu).
- Verify station name and queue match the selected song. If the selected song is already playing, audio must not restart.
- Verify the same station and upcoming-song behavior from the phone's **Start Radio** action.
- Rapidly switch radios while a recommendation request is pending and confirm old results never replace the new queue.
- Verify the command fails harmlessly with no currently selected song.

## Automation and rollout

- GitHub Actions **Guntify - Build and test** runs on pull requests: Android debug build, unit tests and lint.
- The new `CarRadioActionPolicyTest` covers current vs stale/unplayable track selection.
- Android Auto DHU/emulator and physical head unit tests are **still required**: library unit tests cannot prove that Android Auto preserves UI navigation across MediaSession events.
- P2 recommendations and redesigned Home browser are explicitly excluded.
