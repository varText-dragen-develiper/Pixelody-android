# Finding your way around the Android app

The app is Kotlin with Compose screens and a Media3 playback service. The service matters: leaving a screen should not mean starting a different queue, or losing control of the music from the notification.

## Start with the screen, then follow the action

`app/src/main/java/com/pixelody/app/MainActivity.kt` is the activity entry point. Under the same package, `feature/baselayer/PixelodyBaseShell.kt` chooses the screen and passes its actions through. `BasePlaybackHost.kt` connects the UI to playback, local data and the desktop host.

These files are substantial. For a broken button, follow its callback into the host before changing how the screen draws. A queue can look correct while the player is still holding the old order. That is a particularly unhelpful kind of correct.

## Playback has to agree everywhere

`core/playback/PixelodyPlaybackService.kt` owns the Media3 player and session. Notification, headset and screen controls need to act on that same playback state. Queue changes go to the player and are read back; avoid maintaining another authoritative queue in a composable.

Restoring a session also needs a listening check. Restoring the saved position is useful; surprising someone with audio merely because they opened the app is not.

## Local data and desktop connections

Look under `data/storage/` for saved settings and playback data. Use a separate test profile or emulator for destructive recovery tests. Do not clear an existing library just to make a reproduction simpler.

`data/network/PixelodyHostApiClient.kt` makes blocking host requests. `data/repository/HostRepository.kt` runs network work on the IO dispatcher and handles candidate host addresses. Keep new requests on that route so a connection problem does not freeze the interface.

The [shared API fixtures](api-contract-fixtures/) describe the responses both apps test against. A host API change needs checking in the [Windows repository](https://github.com/varText-dragen-develiper/Pixelody) too. Phone-local playback and host streaming are different paths: test the one you changed, and both if the queue logic is shared.

## What counts as checked

Unit tests cover rules without proving that sound comes out of a phone. Compilation does not test permission prompts, background playback or a return from the lock screen. When reporting a fix, include the device or emulator, Android version, build and the steps you tried. Leave anything you could not check explicit.

[CONTRIBUTING.md](../CONTRIBUTING.md) lists the build commands and contribution terms. The release page identifies the downloadable APK; a source change here does not replace that file by itself.
