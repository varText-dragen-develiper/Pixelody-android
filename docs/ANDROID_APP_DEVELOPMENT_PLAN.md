# Android App Development Plan

These notes describe the Android-side development plan for Pixelody. They should be read together with [LOCAL_NETWORK_JAMS_AND_DEVICE_HOSTING.md](https://github.com/varText-dragen-develiper/pixelody/blob/main/docs/networking/LOCAL_NETWORK_JAMS_AND_DEVICE_HOSTING.md) and [ANDROID_PERSONAL_SERVER_PLAN.md](https://github.com/varText-dragen-develiper/pixelody/blob/main/docs/networking/ANDROID_PERSONAL_SERVER_PLAN.md).

For mobile interaction structure, read `ANDROID_MOBILE_UX_NAVIGATION.md`.

This remains the target architecture, not a blanket claim of implemented behavior. As of 2026-07-21, the repository has a native Compose client with QR/short-code pairing, protected saved credentials, revision-aware paged browsing/polling, authenticated Media3/MediaSession playback, and an Android-visible single-host J.A.M. session/join/permission-action contract. The Sequence 4 APK and physical Samsung SM-S721U/Android 16 private-LAN matrix verified pairing, browse, artwork, FLAC/WAV/MP3 playback and seeking, background/lock-screen behavior, process restoration, host and Wi-Fi recovery, stale-address recovery, missing media, revocation, leak safety, real transient/sustained audio-focus handling, and physical Bluetooth route loss/recovery. Wired disconnect is deferred to the release matrix. A connected Sequence 5 run on the same phone verified J.A.M. pairing/join/Windows approval, restricted denials, expanded host-track queue and playback actions, source-aware queue provenance, visible missing-track diagnostics, participant removal, Windows credential revocation, explicit stop, and empty host restart. Sequence 5 remains active only because the privileged-tool quota prevented the final Android-side revoked-state capture and device UI/log leak scan, and the temporary port-scoped Windows test firewall rule still requires administrator removal verification. Offline Room/cache, WebSocket sync, and production portable hosting remain planned.

Use **J.A.M.** for shared listening features: **Joined Audio Mesh**. Avoid the unexpanded plain word "Jam" as the feature name in Android UI, docs, API models, and future marketing copy.

## Android North Star

The Android app should be a native Pixelody companion first, then a limited portable host later.

Its first jobs are:

- Browse and play a paired Pixelody library from a Windows or Linux host.
- Act as a controller for playback, queue, favorites, and J.A.M.
- Play music reliably through Android's native media stack.
- Preserve Pixelody's local-first, owned-library identity.
- Eventually host a small local library from the phone with clear battery, network, and background limits.

Do not treat Android as a full desktop replacement in the first pass. Avoid desktop-heavy work such as broad metadata repair, advanced audio calibration, theme authoring, or admin-grade library surgery until the client, playback, and network foundations are stable.

## Product Priorities

The Android app should prioritize:

- Pairing with a trusted host through QR code, short code, or manual address.
- Browsing playlists, albums, artists, favorites, recently played, and search.
- Showing honest source metadata: format, codec, bit depth, sample rate, bitrate, ReplayGain, and channels.
- Loading artwork and playlist backgrounds from the host with local caching.
- Streaming from authenticated host URLs with seek support.
- Playback controls: play, pause, seek, next, previous, shuffle, repeat, favorite.
- Queue view and queue editing.
- Connected, disconnected, offline, revoked, and unavailable-source states.
- J.A.M. participation with visible roles and diagnostics.

The app should use the same conceptual labels as other Pixelody surfaces: Host, Join, Pair, Trusted Device, Guest, Local Network, Revoke, Stop Hosting.

## Recommended Stack

- Kotlin.
- Jetpack Compose for UI.
- Media3 / ExoPlayer for playback.
- Foreground `MediaSessionService` for active playback and notification controls.
- Room for cached library data, queue state, and connection profiles.
- DataStore for small preferences.
- Retrofit or Ktor client for HTTP APIs.
- OkHttp WebSocket or Ktor WebSockets for live state.
- Coil for artwork loading.
- Hilt only if constructor wiring becomes noisy.

## Proposed Source Structure

```text
android/app/src/main/java/com/pixelody/app/
  PixelodyApp.kt
  MainActivity.kt
  core/
    config/
    network/
    playback/
    storage/
    sync/
    hosting/
    theme/
  data/
    api/
    db/
    model/
    repository/
    localmedia/
    hostapi/
  feature/
    connection/
    sharing/
    library/
    collection/
    nowplaying/
    queue/
    settings/
  ui/
    components/
    navigation/
    theme/
```

Keep feature modules thin. Domain behavior should live in repositories, playback services, sync reducers, and host/client adapters so the UI is easy to replace or reshape.

## Architecture Boundaries

### `core/network`

Owns host discovery, base URLs, auth headers, API client setup, retry rules, and WebSocket connection creation.

### `core/sync`

Owns live state reduction:

- `library.updated`
- `playback.state`
- `queue.updated`
- `track.updated`
- `favorite.updated`
- `jam.updated`
- `source.availability`
- `server.shutdown`
- `auth.revoked`

Sync should be designed around unreliable networks. Disconnects, sleeping hosts, Wi-Fi changes, revoked tokens, and unavailable source devices are normal states.

### `core/playback`

Owns Media3, audio focus, headset events, notification controls, lock-screen metadata, playback errors, and route changes.

Android playback must not claim bit-perfect output or desktop-equivalent audio correction.

### `data/api`

Owns DTOs for the shared `/api/v1` contract. Important DTO families:

- Host capability.
- Device identity and permission.
- Track.
- Playlist.
- Queue.
- Playback state.
- J.A.M. state.
- Federated queue item.
- Diagnostics.

### `data/db`

Owns local cache through Room:

- Paired hosts.
- Auth/device profile metadata.
- Cached tracks.
- Cached playlists and collections.
- Cached artwork metadata.
- Queue snapshot.
- J.A.M./device state.
- Offline cache records later.

### `data/repository`

Combines API, database, sync state, and playback command behavior into stable app operations.

### `feature/sharing`

Owns host/join/pair UI, device permissions, active devices, revoke controls, foreground-hosting status, and Stop Hosting.

### `core/hosting`

Reserved for Android portable hosting after the client is stable. It should own foreground service lifecycle, local HTTP server binding, host notification actions, and Android-local network limits.

## Development Phases

### Phase 1: Client Skeleton

Build the Android app shell without depending on complete desktop server behavior.

- Create Gradle project and Compose shell.
- Add navigation structure.
- Add Pixelody mobile theme tokens.
- Build connection, pairing, and saved-host screens.
- Store server profiles and tokens in private app storage.
- Add mocked API fixtures for library and playback state.
- Add basic connected, disconnected, offline, and revoked states.

### Phase 2: Library Browsing

Consume the real Windows/Linux host API once available.

- Fetch host capabilities.
- Fetch library snapshot.
- Cache tracks, playlists, albums, artists, favorites, and recently played.
- Build library, playlist, album, artist, search, and track-detail screens.
- Load and cache artwork through Coil.
- Show missing/unavailable states without exposing desktop file paths.
- Keep metadata labels honest and readable on mobile.

### Phase 3: Playback MVP

Make the app feel real.

- Implement Media3 playback from authenticated stream URLs.
- Support HTTP range seeking.
- Add foreground `MediaSessionService`.
- Add notification and lock-screen controls.
- Handle audio focus, headset unplug, Bluetooth route changes, and playback errors.
- Implement play, pause, seek, next, previous, shuffle, repeat.
- Show now-playing artwork, metadata, progress, and source quality.
- Survive disconnect/reconnect without losing app state.

### Phase 4: Queue, Controller, And Live State

Turn Android into a useful controller.

- Connect WebSocket live updates to Room and UI reducers.
- Add queue display and queue editing.
- Add favorite/unfavorite writes.
- Add remote playback commands where permissions allow.
- Handle token revocation and server shutdown gracefully.
- Add per-device permission display: browse, stream, cache, remote control, write favorites/playlists, admin.

### Phase 5: J.A.M. Foundations

Build J.A.M. V1 as a stepping stone while preserving the Type 2/federated model.

- Add J.A.M. session screen.
- Show roles: Library Host, J.A.M. Coordinator, Playback Device, Controller, Guest.
- Add shared queue for one host-owned library.
- Add guest permission states.
- Add diagnostics for missing files, sleeping hosts, revoked access, stream retrying, buffer health, and unavailable sources.
- Store queue records with future-ready fields such as `addedByDeviceId`, `sourceDeviceId`, and `sourceLibraryId`.

Single-host J.A.M. V1 should not flatten away source ownership. Type 2/federated J.A.M. sessions are the long-term destination.

### Phase 6: Offline Cache

Add explicit, user-controlled offline support.

- Cache selected tracks or playlists.
- Add storage limit controls.
- Add remove-download and clear-cache actions.
- Show offline availability badges.
- Respect host permissions: some devices may browse or stream but not cache.
- Keep cached media local to the Android device.

### Phase 7: Android Portable Hosting

Add Android hosting only after client playback and sync are stable.

- Add foreground hosting service.
- Show persistent "Pixelody hosting active" notification.
- Add one-tap Stop Hosting.
- Index Android-local files through MediaStore or Storage Access Framework.
- Serve Android-local library snapshot, artwork, stream, and playback state routes.
- Restrict or warn during battery saver, cellular-only mode, unstable Wi-Fi, or background limits.
- Test phone-as-host to tablet/PC client on same Wi-Fi and hotspot.

Android hosting should be described as portable/temporary hosting, not always-on server hosting.

## First Android Milestone

The first concrete Android milestone is:

```text
Pair with a Windows Pixelody host, browse the sanitized library, stream one track with artwork and lock-screen controls, then survive disconnect/reconnect without losing app state.
```

That milestone proves the essential Android backbone:

- Pairing and trusted-device auth.
- Host capability discovery.
- Library snapshot mapping.
- Artwork loading.
- Authenticated stream playback.
- Media3 foreground playback.
- Basic network resilience.

After that works, queue sync, J.A.M., offline cache, Linux host parity, and Android portable hosting can stack on top.

## Build Strategy

Most Android work can be written and unit-tested on a desktop workstation without a device. Treat Android as a normal repo-owned codebase with an extra runtime verification loop.

Without a device, contributors can create and maintain:

- Gradle project files.
- Kotlin source.
- Compose UI.
- Android manifest and resources.
- Media3 playback service code.
- Room schema and migrations.
- API clients, DTOs, repositories, and reducers.
- Unit tests, fixture data, and fake-host tests.
- Build scripts and developer documentation.

Most of the app can be written without an Android device, but real Android behavior must be verified with either an emulator or a physical device before it is treated as done.

### Expected Development Loop

1. Build code in small testable layers.
2. Verify locally with Gradle compile checks and unit tests.
3. Use mock Pixelody host fixtures before the real server is complete.
4. Use a real Windows/Linux Pixelody host once `/api/v1` is available.
5. Use an emulator or connected Android phone for runtime behaviors.
6. Capture logs, screenshots, or reproduction notes for anything device-only.
7. Fix from code plus logs, then repeat.

Avoid large speculative rewrites. Build one vertical slice at a time: connection, library snapshot, artwork, one playable stream, then live updates.

### Desktop-Only Verification

These checks can be run without a phone once the Android toolchain exists:

- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew lint`
- DTO mapping tests.
- Repository reducer tests.
- Room migration tests.
- Fake-host API tests.
- Compose preview/static checks where available.

These are necessary but not sufficient. Passing desktop-side checks does not prove Android playback, notifications, foreground services, background behavior, Bluetooth routing, QR scanning, or local-network permissions.

### Runtime Verification Options

Preferred options:

- Android Studio emulator on this Windows machine.
- A physical Android phone connected with USB debugging and `adb`.
- Manual user testing with logs/screenshots if no device can be attached to the workspace.

Useful device commands once Android tooling is installed:

```powershell
adb devices
adb logcat
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

If a physical device is used, enable Developer Options and USB debugging. If the app needs to connect to a local Windows host server, confirm the phone and host are on the same network or use the appropriate emulator host address.

### Fake Host Requirement

The Android app should include fixture-driven development support so UI and data logic can progress before the real server is finished.

Create test fixtures for:

- Host capabilities.
- Library snapshot.
- Track list.
- Playlist list.
- Artwork URLs or local placeholder artwork.
- Playback state.
- Queue state.
- J.A.M. state.
- Source availability events.
- Auth revoked and server shutdown events.

The fake host should allow the Android app to exercise:

- Pairing success/failure.
- Browse/search.
- Track detail metadata.
- Queue changes.
- Playback command reducers.
- Disconnect/reconnect.
- Missing track and unavailable source states.

### What Needs Real Android Testing

Do not mark these complete from code inspection alone:

- Media3 streaming and seek behavior.
- Lock-screen controls.
- Foreground `MediaSessionService` notification behavior.
- Audio focus.
- Headphone unplug and Bluetooth route changes.
- Background playback.
- App process death and restoration.
- QR scanner flow.
- Local-network connection prompts and failures.
- Android portable hosting.
- Battery saver and sleep behavior.
- Hotspot and Wi-Fi switching.

## Verification Targets

Android checks:

- Unit tests for DTO mapping and repository reducers.
- Room migration tests.
- Playback command reducer tests.
- Instrumented tests for pairing, browse, search, play, pause, seek, next, favorite, disconnect, reconnect.
- Real-device LAN test with screen locked.
- Real-device test with app backgrounded during playback.
- Bluetooth/headphone route-change test.
- Token revocation test.
- Missing media and unavailable-source test.
- Later: Android host test with foreground notification, device sleep, Wi-Fi change, hotspot, and Stop Hosting.

Cross-device checks:

- Android pairs with Windows host.
- Android pairs with Linux host after Linux parity exists.
- Windows/Linux host can revoke Android token.
- Android handles host shutdown without stale playback controls.
- Android does not reveal host file paths in normal UI.
- Android does not expose admin controls to guest devices.

## Non-Negotiables

- Preserve local-first ownership.
- Make sharing opt-in and visibly active.
- Keep trusted-device pairing and revocation central.
- Never expose import, delete, metadata edit, export, or device-management controls to guests by accident.
- Do not require a central Pixelody server for normal local playback.
- Build diagnostics with network features, not after them.
- Treat Type 2/federated J.A.M. as the long-term structure.
- Keep public language conservative around cross-user streaming until legal review.
