# 🎶 USpot 1.1 · build 74

A smoother start for your music, plus a new way to listen at your desk. 🎧

## 🔧 Playback tune-up

- Corrected YouTube playback-verification token handling.
- Removed duplicate player background lookups and gave verification setup enough time within the existing overall deadline.
- Improved browser retries when a preloaded stream fails or playback disconnects.
- Added more useful playback diagnostics without including credentials or private stream URLs.

## 🖥️ Meet Desktop & Browser

Open **Settings → Desktop & Browser**, start desktop mode, and type the displayed address into a computer browser on the same trusted Wi-Fi/router. Your phone handles the connection; the music plays on your computer.

Browser listening counts toward the connected app's local history, Stats and personalization after **30 seconds of audible playback**. Likes and saved radio data use the phone's app integrations. The browser has its own playback queue, volume and EQ.

This interface is optional and experimental. Keep your phone connected and running in the background. Anyone on the same Wi-Fi can connect while desktop mode is on, so use a trusted network and stop it when you're finished. Layouts and device compatibility are still being refined.

## ⬆️ Update without starting over

Download **USpot.apk** below and install it over your existing **regular USpot** app. Export a backup in Settings first. **Don't uninstall or clear app data.** The release build keeps the existing application ID and signing key, with a higher build number.

USpot Desktop Preview is a separate app. Installing this regular release does not replace the preview or merge its data automatically; use export/import if you want to move local data between them.

The matching source ZIP and license notices are included. You only need the APK to install. 👍

## 💚 Thanks, Spotui

Thank you to [Spotui's creators and contributors](https://github.com/Spotui/Spotui) for the excellent open-source foundation that makes USpot possible.

Automated build and test checks run before publication. Real streams, account operations and car controls still depend on the services and devices you use; this release is not a guarantee that every upstream stream rejection is resolved. If a track still fails, include this build number and sanitized playback diagnostics in your report.
