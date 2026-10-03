# Getting started with Pixelody for Android

This public app is a native companion to [Pixelody for Windows](https://github.com/varText-dragen-develiper/Pixelody). Its documented primary library and J.A.M. workflows need a running Windows host on a supported local network. Android portable hosting and an offline host-library cache are not released capabilities.

## Choose source or a published build

Check [Android Releases](https://github.com/varText-dragen-develiper/Pixelody-android/releases) for an APK and its installation instructions. No GitHub release was published when checked on October 2, 2026. The steps below produce a developer debug APK; they do not prove release signing or an update path from another installation.

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

Start the Windows host and put both devices on its supported private network. Open the Android pairing flow, use the host's QR or short code, and confirm the host identity. Browse one album, start an authorized stream, check the queue and reopen the app. Host sleep, Wi-Fi changes and revoked permissions can interrupt host-dependent playback; report those separately from phone-local behavior.

The public README describes host-based features. Phone-local playback may exist in the code, but this guide does not claim a verified standalone listening experience without a build-specific device receipt.

## Modules and help

Settings & Themes → Modules — import or manage → Import downloaded module opens the system document picker in compatible builds. Supported modules configure existing tools; they do not execute arbitrary downloaded code. Read the [module guide](https://github.com/varText-dragen-develiper/Pixelody/blob/main/docs/OPTIONAL_MODULES.md).

Use [SUPPORT.md](../SUPPORT.md) for bug reports and the [shared forum](https://github.com/varText-dragen-develiper/Pixelody/discussions) for ideas. See [Release preparation](RELEASE_PREPARATION.md) before distributing an APK.
