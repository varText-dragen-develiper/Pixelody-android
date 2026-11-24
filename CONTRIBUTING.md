# Contributing to Pixelody-android

Bug reports, documentation, design ideas and fixes are all welcome. If you're
changing something substantial, open an issue first so we can work out what it
needs before you spend a lot of time on it.

## Working on a change

The [code tour](docs/CODE_TOUR.md) points to screens, playback and host requests. With a JDK and Android SDK available, run these from the repository root:

```sh
bash ./gradlew assembleDebug
bash ./gradlew testDebugUnitTest
bash ./gradlew lintDebug
```

On Windows, use `gradlew.bat` with the same task names. For a manual test, record the APK, Android version and device. Check playback after leaving the app and returning, and distinguish phone-local tracks from host streams. Host tests also need a running Windows app. Use a disposable emulator or test profile for anything that deletes saved data.

## Describing the change

A useful title names the problem: “Keep queue order after reopening” tells us more than “Update source.” In the description, explain what happened before, what happens now and what you checked. Include a remaining failure if there is one; finding it later is not an improvement.

Comments should explain a constraint or a surprising choice. There is no need to narrate an assignment or repeat the function name in a sentence. If the reason matters to the next person changing it, write that down.

## Contributor License Agreement

pixelody-android is released under GPL-3.0-only. The owner also ships official free and
paid builds (including through app stores) under other terms, so every
contribution needs a signed Contributor License Agreement (CLA).

- A CLA Assistant check runs on each pull request. Sign once, with your GitHub
  account, when the bot asks.
- You keep the copyright in your work. The CLA grants the project the right to
  distribute it under the GPL and under other licenses, including the
  proprietary terms of official store builds.
- A pull request cannot be merged until the CLA check passes.
- If you contribute work created for an employer or client, you are responsible
  for getting their permission first.

## Before sending it

- Original work, or work compatible with GPL-3.0-only.
- No copied commercial code, artwork, screenshots, music, fonts, sounds, trade
  dress, secrets, personal data or credentials.
- Third-party material only when its license permits the use and its source,
  license text and any modifications are recorded.
- Tests and documentation proportionate to the change.

The names and logos "Pixelody" are covered by TRADEMARKS.md, not by your
contribution grant. Report security problems privately as described in
SECURITY.md, not in a public issue.

I'll review the change and decide what to include.
