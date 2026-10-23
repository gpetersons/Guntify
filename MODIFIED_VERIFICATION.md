# Build 77 verification

## Required before publication

GitHub Actions must assemble the Android APK, pass app and Spotify-module unit tests, pass simulated browser checks, and verify the production certificate, application ID and versionCode 77. The release is published only after its files are uploaded.

## Changes and meaningful checks

- Playlist library mutations read a revisioned rootlist, change only the requested playlist URI references, and read back membership before confirming success. They never send a contents-delete request to the playlist itself. Tests cover preserving folder references, no duplicate save, save/unfollow, unconfirmed results, changed pagination revisions, wrong URI types, and HTTP 429 cooldown without write retries.
- Playlist creation returns its real Spotify ID separately from saving it to the library. Android and browser callers persist that ID before trying the save. Failed save retries never create another playlist. Durable account-scoped records are distinct from the replaceable library snapshot and included in backup exports.
- Ledger tests cover restart serialization, remote omission, account separation, remote metadata confirmation, ID collisions and explicit removal.
- The existing signed-in WebView can supply session headers from regional Spotify endpoints. The first write waits briefly for the session; it never fabricates client credentials.
- Fresh-process update checks revalidate GitHub releases even if the preceding process checked less than an hour ago. Warm resumes are throttled. Existing GitHub backoff and download/signature verification remain. Tests cover cold starts, warm resumes and clock rollback.

No live Spotify account or Android device is available here. Rootlist protocol behavior, account write access, process-restart UI, real streaming and installer prompts still require device confirmation. Retaining a playlist record is not proof of Spotify library synchronization; pending state is shown explicitly. Legacy IDs can be recovered only where the earlier app retained their generated-cover metadata and Spotify confirms ownership.

## Primary references inspected

- https://github.com/mirrorfm/spotify-private-api/blob/main/app/app.go — rootlist read/change endpoint and baseRevision envelope.
- https://github.com/librespot-org/librespot/blob/dev/protocol/proto/playlist4_external.proto — ADD/REM reference operations and list schema.
- https://github.com/imoonkey/openweb/blob/main/src/sites/spotify/adapters/spotify-pathfinder.ts — observed Web Player creation request and session headers.

The new implementation was written for this project; no upstream source file was copied. Existing licenses and attribution remain.
