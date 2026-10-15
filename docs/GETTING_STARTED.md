# Getting started with Pixelody for Android

Connect to [Pixelody for Windows](https://github.com/varText-dragen-develiper/Pixelody) for its library and J.A.M. sessions. Keep the host running on a supported local network. Phone hosting and an offline copy of the host library are still planned.

## Choose source or a published build

Check [Android Releases](https://github.com/varText-dragen-develiper/Pixelody-android/releases) for an APK and its installation instructions. No Android GitHub release is published as of October 10, 2026. The steps below produce a developer debug APK; they do not prove release signing or an update path from another installation.

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

For phone-local playback, use your build's instructions and device test results.

## Modules and help

Settings & Themes → Modules — import or manage → Import downloaded module opens the system document picker in compatible builds. Supported modules configure existing tools; they do not execute arbitrary downloaded code. Read the [module guide](https://github.com/varText-dragen-develiper/Pixelody/blob/main/docs/OPTIONAL_MODULES.md).

Use [SUPPORT.md](../SUPPORT.md) for bug reports and the [shared forum](https://github.com/varText-dragen-develiper/Pixelody/discussions) for ideas. See [Release preparation](RELEASE_PREPARATION.md) before distributing an APK.
