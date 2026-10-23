# Android development notes

This summarizes the Android plan and the device work recorded through July 21,
2026. Later releases may have changed the app; start with the current README and
release notes when installing or contributing. The full earlier development
sequence remains in Git history.

I want the phone app to be useful on its own and alongside the desktop player.
The work recorded here concentrated on pairing with a Windows host, browsing
its library and playing authenticated streams through Android's media system.
Portable hosting was a later idea, with battery, network and background limits
to work out before treating a phone like an always-on server.

## What was working at that checkpoint

By July 21, the Kotlin/Compose client had QR and short-code pairing, protected
saved credentials, revision-aware paged browsing and polling, and Media3 playback
with a `MediaSessionService`. It also displayed the single-host J.A.M. session,
join requests and permitted queue/playback actions. J.A.M. means Joined Audio Mesh.

The July device work used a Samsung SM-S721U on Android 16. The Sequence 4 run
covered pairing, browsing, authenticated artwork and FLAC/WAV/MP3 playback and
seeking. It also covered background and lock-screen behavior, process restore,
host and Wi-Fi recovery, address changes, missing media, revocation, credential
and private-path checks, audio-focus interruptions and Bluetooth loss/recovery.
Physical wired-headphone disconnect was deferred because the accessory wasn't
available; it wasn't counted as tested.

The connected Sequence 5 run then covered joining J.A.M. and Windows approval,
denied actions under restricted permissions, and host-track queue and playback
actions after permissions were expanded. The queue kept who added an item and
which host owned it, including visible missing-track information. Participant
removal, Windows credential revocation, explicit stop and an empty host restart
were also recorded.

That run still wasn't closed. The final Android revoked-state capture and
device UI/log inspection weren't recorded, and removal of the temporary Windows
firewall rule still needed administrator verification. Those limits belong with
the result rather than disappearing behind a completed-looking phase list.

## Keeping the app understandable

Someone browsing music shouldn't have to work through a network dashboard.
Keep the current track, source and queue easy to reach, with connection details
nearby when they explain why something won't play. Lost Wi-Fi, a sleeping host,
revoked access and an unavailable source need different, useful responses.
The [navigation notes](ANDROID_MOBILE_UX_NAVIGATION.md) describe the intended feel
and the difference between older proposals and the implemented shell.

Playback belongs to Media3 and its service, including focus, route changes,
notifications, lock-screen controls and errors. Networking handles host addresses,
authentication and reconnects; repositories and state reduction keep those facts
out of individual screens. Shared `/api/v1` models need to agree with the host.
Don't expose host file paths or give a guest import, delete, metadata-edit,
export or device-management access by accident.

## What was still planned

Room-backed offline storage/cache, WebSocket updates and production portable
hosting were not complete at this checkpoint. Cached music would need explicit
selection, storage/removal controls and host permission. Phone hosting would
need a visible foreground service, a persistent notification and a one-tap stop,
along with tests for sleep, battery saver, Wi-Fi changes and hotspots.

Federated J.A.M. and libraries from several hosts also remained later work.
Preserving `addedByDeviceId`, `sourceDeviceId` and `sourceLibraryId` in the
single-host queue kept the source information available for that work; it didn't
make federation an implemented feature.

For contributors, use the Gradle checks and setup instructions in
[Getting started](GETTING_STARTED.md). Code and fixture checks are useful before
a device is connected, but they don't establish playback, notification, routing
or recovery results on a phone. Keep the APK and device with any result you leave.
Android bit-perfect output and desktop-equivalent audio correction weren't
established by these tests.

The Windows [hosting notes](https://github.com/varText-dragen-develiper/Pixelody/blob/main/docs/networking/LOCAL_NETWORK_JAMS_AND_DEVICE_HOSTING.md)
and [portable-host plan](https://github.com/varText-dragen-develiper/Pixelody/blob/main/docs/networking/ANDROID_PERSONAL_SERVER_PLAN.md)
cover the other side of those connections.
