# USpot 1.1 · build 70

- Consolidates the repository around the latest integrated Android source; builds no longer extract an old source ZIP.
- Removes personal maintainer credits and the personal GitHub profile/source link from the About page and bundled notice. Original Spotui and third-party credits remain.
- Retains build 69 playback, Radio, personalization, queue, Library, Android Auto and recovery behavior.
- Keeps the same application ID and existing signing key configuration for in-place updates.

## Downloads

- **USpot.apk** — Android application, signed using the repository's existing saved key.
- **USpot-source.zip** — complete source from the commit used to build the APK.
- **LICENSE**, **NOTICE**, **SOURCE.md** — license, attribution and corresponding-source information.

The publishing workflow requires APK assembly and unit tests to pass. These checks do not establish that every account-dependent feature or Android Auto device has been tested. Stream availability and the previously reported intermittent lookup failure remain subject to device/network verification.

Install as an update without uninstalling. Source and signing details are in SOURCE.md.
