# Source and distribution information — USpot 1.1, build 41

USpot is a modified Spotui fork distributed under GNU GPL version 3. The
original license text is retained unchanged in LICENSE. See NOTICE for credits,
the dated modification notice and warranty statement.

The source matching this build is **USpot-build-41-source.zip**:
https://github.com/rare621/Spotui/blob/main/USpot-build-41-source.zip

The build workflow also includes this exact input ZIP, LICENSE, NOTICE and this
file in the same downloadable artifact as the APK. Keep the source alongside
every APK you share; include a clear source link on each release/download page
and keep the matching source available for recipients. A link to the original
Spotui project alone is not the source of a modified USpot APK. GitHub Actions
artifacts expire, so use a durable release/download location for public releases
and include both APK and matching source there.

## Build a modified copy

1. Extract the ZIP and open the Spotui-Modified directory.
2. Install Java 17, Android SDK platform 34 and build-tools 35.0.0. Gradle's
   wrapper obtains Gradle 8.11.1. Gradle resolves the declared libraries from
   the repositories configured in settings.gradle.kts.
3. For your own build, use your own Android signing key or the normal local
   debug key. Do not request or publish USpot's private update-signing key.
   To update an existing installed USpot APK in place, its original signing
   key is required; otherwise a differently signed copy needs a different
   application ID or separate installation.
4. Run `./gradlew :app:assembleDebug --no-daemon --max-workers=2`.
5. Run `./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2`.

The source includes the app, Spotify and InnerTube modules, Gradle wrapper,
resources and build configuration. Existing third-party notices are retained;
this notice addition is not a full dependency, media-rights or service-terms
audit and does not certify blanket legal compliance.

Original Spotui: https://github.com/Spotui/Spotui
GNU GPL v3: https://www.gnu.org/licenses/gpl-3.0.html
