# Pixelody for Android

I’m building Pixelody for Android alongside the Windows app. Play music on your phone, or connect to your Windows library to browse, listen and join J.A.M. (Joined Audio Mesh) sessions. Keep the Windows host running for its library and session features; hosting that library from your phone is still planned.

This repository is the open-source edition of the Android app. It is licensed under the GNU General Public License v3.0 only (see [LICENSE](LICENSE) and [NOTICE](NOTICE)). If you fork or redistribute it, read the name and logo guidance in [TRADEMARKS.md](TRADEMARKS.md). The Windows app is at [Pixelody](https://github.com/varText-dragen-develiper/Pixelody).

## Start here

- [Getting started](docs/GETTING_STARTED.md): requirements, source build and first listening session.
- [Android releases](https://github.com/varText-dragen-develiper/Pixelody-android/releases): the Android 0.2.1 debug-signed testing APK and its installation notes, under `v0.2.1`.
- [Support](SUPPORT.md) and [shared discussions](https://github.com/varText-dragen-develiper/Pixelody/discussions): report problems or share listening ideas.
- [Release preparation](docs/RELEASE_PREPARATION.md): what still needs verification before distributing a binary.

## What it does

- Pairs with a Windows host by QR code or short code, and remembers trusted hosts.
- Browses the host's library, playlists and queue.
- Plays through a Media3 playback service using authenticated streams from the host.
- Follows live host state with bounded reconnect and clear failure messages.
- Joins J.A.M. sessions: join, approval, permission actions, queue and removal.
- Installs optional modules downloaded from the Pixelody website, through Settings & Themes, then Modules. How it works is described in the Windows repository's `docs/OPTIONAL_MODULES.md`.

Not available yet: an offline library cache, WebSocket push, and hosting a library from the phone itself.

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

Want to help with a fix? Thanks for taking a look. Start with [CONTRIBUTING.md](CONTRIBUTING.md), including the Contributor License Agreement. Report security problems privately using [SECURITY.md](SECURITY.md).
