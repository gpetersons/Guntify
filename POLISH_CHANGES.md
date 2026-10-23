# Interface polish · USpot build 82

Published in the regular USpot app, package `com.music.spotui.modified`. Builds 78–81 were separate private test builds. The following records implementation and validation limits.

## Install and use

Install **USpot.apk** over regular USpot to retain its local data. Desktop & Browser uses the normal port 8765. If you tested the separate preview, export/import a backup to transfer any preview history you want to keep; credentials and audio downloads remain app-specific.

Spotify actions affect the signed-in Spotify account. Desktop listening history is stored in the phone app; the existing browser playback queue stays independent of the phone queue.

## Included changes

| Area | Android | Desktop browser |
| --- | --- | --- |
| Playback feedback | Connecting, buffering and retry states; unknown duration shown distinctly | Clearer connection/retry messages and unknown duration |
| Visual consistency | Shared green accents, theme-aware Material controls, aligned filter pills | Unified surfaces, spacing, focus states and light/dark contrast |
| Bottom controls | Opaque navigation background; measured mini-player inset; no forced player navigation at track end | Stable footer, safe-area spacing and responsive layout |
| Home | Up to four nonempty shelves, artist variety, fewer repeated visible cards; shelf titles and item types retained | Same shared Home policy; compact shelves with See all |
| Search | Pinned bar, back/clear, saved position/query, prominent top song, grouped results, local Radio filter, Radio/Mix creation shortcuts | Top result with Play/Open, grouped results, recent queries, clear/filter controls and Radio shortcuts |
| Song actions | Existing menus and likes retained; playing bars and stronger contrast in Search | Per-row likes, Radio/queue/menu actions; song context menus stay song-specific |
| Library | Persistent pins in rows and grid; saved Radios can be pinned; recent order without extra sort controls | Pins use the phone backend, including collection menus |
| Radio | Shorter summary and expandable station details/mix ingredients | Shorter summary and collapsible mix ingredients; live tuning retained |
| Settings | Grouped compact rows, useful summaries, Advanced troubleshooting/reset confirmation | Collapsible functional groups; existing settings controls retained |
| Feedback | Unified snackbar feedback; Undo for Home hiding and clearing recent searches; destructive Spotify confirmations retained | Consistent messages, Home Undo, existing queue Undo and Spotify confirmations retained |
| Loading/navigation | Shared row placeholders; bottom spacing and saved Search state | Row placeholders, restored view positions and reduced-motion support |
| Accessibility | Clearer control labels, larger key touch targets, theme contrast and scalable filter pills | Keyboard focus rings, labels and responsive controls |
| Downloads | Storage usage, bounded persistent failure records, Retry/Retry all, per-task Cancel; cancellation cleanup | Live backend progress, storage usage, Retry/Retry all, Dismiss and Cancel |

Existing Spotify authentication, catalog search, playback resolvers, Radio generation, local personalization and download transfer implementation are reused. Playback resolver behavior is not replaced by this design pass. Smart Downloads remains opt-in.

New pins and download retry records are included in data exports. Failed download records are bounded to 50 entries. Cancelled downloads are not recorded as failures.

## Verification

- Local clean Android assembly and app/Spotify JVM unit tests passed (365 tests: 345 app, 20 Spotify).
- Browser DOM/interaction tests passed with simulated phone responses and audio; coverage exercises shared pins, download Retry/Cancel, Radio search filtering and collapsed Settings.
- Chromium visual/navigation checks passed at 1440×1000 and 412×915, including light/dark Search, Home, Library, Downloads and Settings. No page errors or horizontal document overflow were found in these fixtures.
- APK application ID, display name, build number and APK v2 signature verified. The production workflow must verify certificate SHA-256 `72feb004ee3aa78eca4b954c6d448b774a41166af77a35b2d4b59cdb9a2752ed` before publishing.

These checks do not verify real Spotify account writes, YouTube streaming availability, Bluetooth, Android Auto, Android audio effects, background survival or Android UI rendering on a physical phone. Those require physical-device and live-account testing. Existing platform-specific limitations remain; the browser does not become a native Android player or device-output controller.

## Test checklist

- Compare light/dark Home, Search, Library, player and Settings on your phone.
- Start a song; open and close tabs; confirm no unexpected navigation and no content hidden by the mini-player.
- Pin/unpin a playlist, album and Radio. Check ordering after restart and in the browser.
- Start a download, cancel one, retry a failed one, and check storage usage.
- Create/open a Radio Mix and change tuning while listening without a restart.
- Import a regular USpot backup and confirm history/radios appear in the separate preview.
- Confirm regular USpot remains installed with its original data.

The manual workflow builds an artifact only. Future preview APK updates must reuse the preview signing key; production signing credentials are not included in this source.

Thanks to Spotui's original creators and contributors. Original GPL and third-party notices remain in LICENSE and NOTICE.

## Preview build 79
- Removed Library Recent/Name/Creator controls on phone and browser; default order keeps pins first.
- Full player uses the source/quality line for Connecting, Buffering and retry status, then restores the original source/quality text. Removed the separate status row above the slider.

## Preview build 80
- Animated player entrance/exit, play/pause crossfade and artwork atmosphere transitions.
- Menu content reveal, animated sheet dismissal and subtle press scaling without gray ripples.
- Matching browser dialog and artwork transitions, with reduced-motion support.
- Android assembly and unit tests passed; browser interaction checks passed. Physical-device animation smoothness remains to be tested.

## Preview build 81
- Consistent short fade/slide transitions across navigation routes, Home Music/Following/Stats, Library filters/layout, and every Settings category. Settings titles and selected tabs transition too.
- Canvas now renders through a TextureView so it participates in player enter/exit transforms; cover artwork remains underneath before the first video frame. It still uses the existing muted, cached player and releases on disposal.
- Isolated 300 ms seek/timer updates in a small composable instead of invalidating the whole player. Source/quality refreshes are scoped to the info area; animated background color is read in the drawing phase.
- Shortened secondary menu reveals to avoid stacking long motion on top of Material sheet movement.
- Browser route reveals and animated Settings expansion/collapse, including rapid-toggle cancellation and reduced-motion behavior.
- Chromium checks passed for dialog animation endpoints, Settings toggles, reduced motion, desktop/mobile layouts and light/dark themes.
- Phone performance and the video minimizing behavior still require a physical-device check; no phone/emulator was attached.

Build 81 validation: clean Android assembly succeeded; all 365 app/Spotify unit tests passed, as did browser interaction checks.
