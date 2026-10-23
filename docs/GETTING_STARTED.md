# Getting started with Pixelody for Android

Connect to [Pixelody for Windows](https://github.com/varText-dragen-develiper/Pixelody) for its library and J.A.M. sessions. Keep the host running on a supported local network. Phone hosting and an offline copy of the host library are still planned.

## Choose source or a published build

The [Android 0.2.1 preview](https://github.com/varText-dragen-develiper/Pixelody-android/releases/tag/v0.2.1) is available as a debug-signed APK for Android 8 or newer. Compare it with the release’s checksum file and read the update instructions. If Android reports a signing-key conflict, stop and report it before uninstalling; uninstalling can remove local data. The [website](https://pixelody-web.pixelody101.workers.dev/testing) has setup help and a listening checklist. The source-build steps below make a developer debug APK; they do not establish production signing or compatibility with every existing installation.

## Build from source

Use Android Studio/Android SDK with platform 35 and a compatible JDK; current CI uses JDK 21. The app's minimum SDK is 26 (Android 8.0). Set **JAVA_HOME** and **ANDROID_HOME** to your installed tools when building from a terminal.

~~~powershell
git clone https://github.com/varText-dragen-develiper/Pixelody-android.git
cd Pixelody-android
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
~~~

On Linux/macOS, run the wrapper with **bash ./gradlew**. The output is **app/build/outputs/apk/debug/app-debug.apk**. Installing it over an existing app can fail if signing identities differ. Back up important state and read the release/update instructions before changing an installation; do not clear user data to bypass an unexplained error.

## First companion session

Start the Windows host and put both devices on its supported private network. Open the Android pairing flow, use the host's QR or short code, and confirm the host identity. Browse one album, start an authorized stream, check the queue and reopen the app. If host playback stops after Windows sleeps, Wi-Fi changes, or permissions are revoked, include that in your report so I can follow what happened. Keep it separate from phone-local playback results.

For a first phone-local session, import music you have permission to use, try play, pause, seek and skip, then close and reopen Pixelody. Check that your library and settings are still there.

## Modules and help

Settings & Themes → Modules — import or manage → Import downloaded module opens the system document picker in compatible builds. Supported modules configure existing tools; they do not execute arbitrary downloaded code. Read the [module guide](https://github.com/varText-dragen-develiper/Pixelody/blob/main/docs/OPTIONAL_MODULES.md).

Use [SUPPORT.md](../SUPPORT.md) for bug reports and the [shared forum](https://github.com/varText-dragen-develiper/Pixelody/discussions) for ideas. See [Release preparation](RELEASE_PREPARATION.md) before distributing an APK.
