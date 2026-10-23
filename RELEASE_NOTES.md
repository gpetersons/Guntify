# Sprotify 1.0.0 Beta 5 - final naming and repository migration

## What's new
- Official product name is now **Sprotify** (without the Latvian accent) across the Android launcher, Android Auto, Radio, settings, notifications and Desktop Mode.
- Updated logo resource paths and README to match the renamed GitHub repository: [gpetersons/Sprotify](https://github.com/gpetersons/Sprotify).
- The in-app update checker now reads the **new GitHub repository** and continues accepting historic `Guntify.apk` and `SprotiFy.apk` asset names as well as the new `Sprotify.apk`.
- Updated CI workflow names, attribution documents and playback diagnostics.
- Keeps Beta 4's Radio and personalized Android Auto features and the verified-update checks.

## Installation and compatibility
- Signed APK: **Sprotify.apk**. Source archive: **Sprotify-source.zip**.
- Version: **1.0.0-beta.5**, **versionCode 5**, **applicationId lv.guntify.player**.
- Uses the same signing identity as Beta 4 for a compatible update. **Install over Beta 4; do not uninstall your current app.** Back up important local data before updating.
- A successful installation changes the displayed application name to **Sprotify**, without creating a separate Android package.
- Older signed releases remain available under their published filenames.

## Notes
- Verify the name and sardine icon on the phone and the Android Auto display.
- Desktop Mode should only be used on a trusted local network. Security hardening under separate PRs is not claimed as part of this release.
- Full independent security and network audit not completed.

**Credits:** [Spotui](https://github.com/Spotui/Spotui), [USpot](https://github.com/rare621/USpot) and contributors acknowledged in [NOTICE](NOTICE). GPLv3 and upstream acknowledgements are retained.
