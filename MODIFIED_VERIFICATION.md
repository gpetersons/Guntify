# Validation — USpot 1.1, build 70

The starting source is the build-69 ZIP already present in the repository. Its Git blob SHA was checked against the local package before editing. The old loose repository source was the original app and did not represent the ZIP-based builds.

This change consolidates the current integrated source into the repository root, removes superseded source ZIPs/obsolete workflows from the current file list, replaces the outdated documentation and removes personal maintainer attribution/profile links from the app. Internal repository endpoints used for update checks remain configured to the hosting repository. The original GPL license and third-party notices are retained.

Runtime changes are limited to About-page text/link removal and bundled notice text. Package ID, data formats, signing secrets, icons, stream engines and feature behavior remain as in build 69. versionName remains 1.1; versionCode increases to 70.

Local verification covers source/archive completeness, a Kotlin syntax parse of the changed settings file, and workflow/version/signing/notice consistency. No new behavioral tests are added for the credit text changes. Build 69's 103 isolated checks and partial Kotlin/Android/Media3/Compose compilation are prior evidence, not a complete build-70 APK test.

The CI workflow builds the current source with `:app:assembleDebug`, runs `:app:testDebugUnitTest`, creates corresponding source from that commit and only then publishes an APK/source release. A successful release therefore confirms those CI checks passed; device playback, account mutations and actual car/voice routing still require device testing. The intermittent all-streams-failing incident reported previously has not been conclusively reproduced or identified.

The Git history retains old source commits. The cleanup removes old packages from the default-branch file list without rewriting history. The first release is versioned v1.1-build-70 and marked latest after successful publication.
