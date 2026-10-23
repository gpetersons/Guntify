# ŠprotiFy 1.0.0 Beta 4: new name, sardine icon and update checker

## What's new
- Guntify becomes **ŠprotiFy**, with the approved green-and-black three-sardine icon on Android launcher, splash screen and desktop.
- Updated visible product naming in settings, Radio labels and documentation.
- Fixed update detection for prereleases: read published GitHub releases and select the highest supported build number, including Beta N.
- Retains strict APK download URL, size and SHA-256 digest checks, plus installed package/signature checks.
- Includes Android Auto Radio and personalized Home improvements from Beta 3.

## Installation and compatibility
- **versionCode 4**, **versionName 1.0.0-beta.4**, **applicationId lv.guntify.player**.
- Signed with the existing Guntify signing key for an in-place update. **Do not uninstall** the old app; back up important data first.
- Download **SprotiFy.apk** from this release and install it over Guntify Beta 3. Older beta builds cannot detect prereleases correctly, so this first upgrade requires manual installation. Future compatible beta releases can be found in ŠprotiFy's Updates screen.
- GitHub repository URL remains gpetersons/Guntify to preserve release links.

## Known limitations
- Icon rendering and Android Auto changes still require real-device and in-car validation.
- Desktop Mode security PR #5 is separate and **not included**. Use Desktop Mode only on a trusted network and never expose its port to the internet.
- A complete independent security audit has not yet been completed.

**Thanks to [Spotui](https://github.com/Spotui/Spotui) and [USpot](https://github.com/rare621/USpot).** GPLv3 licensing and attribution notices remain.
