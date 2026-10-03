# Pixelody for Android

Pixelody for Android is a native companion to Pixelody for Windows. It connects to your Windows Pixelody host over your local network, lets you browse the library, controls playback, and takes part in J.A.M. (Joined Audio Mesh) sessions. It is not a standalone library host: the Windows app owns the music library.

This repository is the open-source edition of the Android app. It is licensed under the GNU General Public License v3.0 only (see [LICENSE](LICENSE) and [NOTICE](NOTICE)). The Pixelody name and logo have separate trademark guidance; see [TRADEMARKS.md](TRADEMARKS.md) for what that means if you fork or redistribute this code. The Windows app is at [Pixelody](https://github.com/varText-dragen-develiper/Pixelody).

## Start here

- [Getting started](docs/GETTING_STARTED.md): requirements, source build and first listening session.
- [Android releases](https://github.com/varText-dragen-develiper/Pixelody-android/releases): exact binaries and release notes when published. No GitHub release was published as of October 2, 2026.
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
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Installing on a device or running the instrumented tests also needs a connected device or emulator (`adb devices`). Pairing needs a Pixelody for Windows host on the same network.

The shared API fixtures that both apps test against are in [docs/api-contract-fixtures/](docs/api-contract-fixtures/).

## Contributing and security

Pull requests are welcome. Follow the Contributor License Agreement requirements in [CONTRIBUTING.md](CONTRIBUTING.md). Report security problems privately as described in [SECURITY.md](SECURITY.md), not in a public issue.
