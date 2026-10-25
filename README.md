# Pixelody for Android

Pixelody plays music on your phone and connects to [Pixelody for Windows](https://github.com/varText-dragen-develiper/Pixelody) for its library and J.A.M. (Joined Audio Mesh) sessions. You can listen locally without a host. When you're using the Windows library, that app needs to stay running.

I'm building both apps so the library, playback and sound controls fit together, while leaving room to set the player up how you like it.

This repository is the open-source edition of the Android app. It is licensed under the GNU General Public License v3.0 only (see [LICENSE](LICENSE) and [NOTICE](NOTICE)). If you fork or redistribute it, read the name and logo guidance in [TRADEMARKS.md](TRADEMARKS.md). The Windows app is at [Pixelody](https://github.com/varText-dragen-develiper/Pixelody).

## Download or build it

- [Getting started](docs/GETTING_STARTED.md): requirements, source build and first listening session.
- [Android releases](https://github.com/varText-dragen-develiper/Pixelody-android/releases): the Android 0.2.3 debug-signed testing APK and its installation notes, under `v0.2.3`.
- [Support](SUPPORT.md) and [shared discussions](https://github.com/varText-dragen-develiper/Pixelody/discussions): report problems or share listening ideas.
- [Release preparation](docs/RELEASE_PREPARATION.md): what still needs verification before distributing a binary.

## In the app

- Pairs with a Windows host by QR code or short code, and remembers trusted hosts.
- Browses the host's library, playlists and queue.
- Plays through a Media3 playback service using authenticated streams from the host.
- Reconnects when the host connection drops and reports connection problems.
- Joins J.A.M. sessions: join, approval, permission actions, queue and removal.
- Installs optional modules downloaded from the Pixelody website, through Settings & Themes, then Modules. How it works is described in the Windows repository's `docs/OPTIONAL_MODULES.md`.

An offline copy of the host library, WebSocket updates and phone hosting are still planned.

## Build

You need the Android SDK and a JDK. From this directory:

```sh
bash ./gradlew assembleDebug
bash ./gradlew testDebugUnitTest
bash ./gradlew lintDebug
```

Installing on a device or running the instrumented tests also needs a connected device or emulator (`adb devices`). Pairing needs a Pixelody for Windows host on the same network.

The shared API fixtures that both apps test against are in [docs/api-contract-fixtures/](docs/api-contract-fixtures/).

## Contributing and security

Read [CONTRIBUTING.md](CONTRIBUTING.md), including the CLA, before sending a contribution. Send security reports [privately](SECURITY.md).
