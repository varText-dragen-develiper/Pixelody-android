# Pixelody for Android

Pixelody for Android is a native companion to Pixelody for Windows. It connects to your Windows Pixelody host over your local network, lets you browse the library, controls playback, and takes part in J.A.M. (Joined Audio Mesh) sessions. It is not a standalone library host: the Windows app owns the music library.

This repository is the open-source edition of the Android app. It is licensed under the GNU General Public License v3.0 only (see [LICENSE](LICENSE) and [NOTICE](NOTICE)). Official paid builds are also sold in the Android and Windows stores under the Pixelody name; see [TRADEMARKS.md](TRADEMARKS.md) for what that means if you fork or redistribute this code. The Windows app is at [Pixelody](https://github.com/varText-dragen-develiper/Pixelody).

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

Pull requests are welcome. Contributions need a signed Contributor License Agreement, which a bot checks on each pull request; see [CONTRIBUTING.md](CONTRIBUTING.md). Report security problems privately as described in [SECURITY.md](SECURITY.md), not in a public issue.
