# Android Mobile UX And Navigation Philosophy

These notes translate Pixelody's desktop North Star into a native Android surface.
They define product intent, not a blanket implementation claim. As of 2026-08-10,
the current Compose shell uses Home/Search/Library/Create as its persistent set and
opens Player, Queue, Profile, Files, and Tools contextually. The older target below
is therefore a hypothesis to test, not permission to silently change the release
navigation.


## Mobile Thesis

Pixelody Android should feel like a calm personal music companion: immediately playable like familiar streaming apps, but visibly local-first, source-aware, and comfortable with advanced sharing/J.A.M. controls when the user asks for them.

Use Spotify, Pandora, and similar mature music apps as ergonomics references only:

- Fast return to current playback.
- A small number of stable bottom destinations.
- Album art and track identity as the emotional center.
- Search/browse controls close to the library.
- Sharing and device setup available from a profile/technical control without making the whole app feel like a settings panel.

Do not copy brand language, icons, layouts, palettes, or commercial assets.

## Primary Navigation

The permanent model must be chosen from matched journey evidence. The historical
music-first target was:

```text
Home     - resume playback, source shelves, quick actions
Search   - find host and phone music in one place
Library  - paired host library browsing
Player   - current playback, queue, and source quality
```

The implemented control is:

```text
Home     - resume playback, source shelves, quick actions
Search   - find host and phone music in one place
Library  - paired host library browsing
Create   - add phone music or start a portable source
```

Player remains continuously reachable through the persistent mini-player. The
release experiment `X-001` compares both sets with a third option: Home/Search/
Library plus a distinct contextual Add/Create action. Retain the current control
unless a challenger produces a clear P0 journey improvement before T-14.

Queue, phone files, pairing details, QR scanning, trusted-device controls, hosting,
and diagnostics are important, but should remain contextual unless evidence shows
that a primary placement materially improves ordinary listening. They remain easy
to reach and must preserve a predictable return path.

Technical/profile control:

```text
Technical - manual connection, hosting, trusted devices, route mode, J.A.M. diagnostics
```

## Dynamic Simplicity

Dynamic simplicity is not the removal of depth. It is the controlled appearance of
depth when content, state, or intent makes it relevant.

- Before connection, pairing may lead; after connection, listening leads.
- During playback, current track and transport gain priority; acquisition recedes.
- When a source fails, recovery appears beside that source instead of in a generic
  diagnostic wall.
- On compact windows, one job owns the pane; on expanded windows, supporting
  listening or detail may appear without replacing route truth.
- Advanced quality, EQ, permission, hosting, and J.A.M. information remains close,
  but does not compete with title, artwork, and transport at first glance.
- Back removes the newest layer of depth and returns to the originating context.

## Pixelody Studio Mobile

The Android default should feel related to the Windows Pixelody Studio theme without copying the desktop layout onto a phone.

- Use warm gold as Pixelody identity and selection.
- Use soft green for live playback, trusted signal, and active source state.
- Use cool metadata accents for codec, sample rate, bit depth, bitrate, and source labels.
- Keep off-black surfaces, compact 8dp panels, and precise row alignment.
- Album art and track identity should lead every music surface.
- Technical controls belong behind the profile/status control unless they explain why music cannot play.
- Host pairing should be QR-first on Home; pasted invites and split URL/token entry are fallback paths.

## Home Rules

Home should answer four questions at a glance:

- What am I connected to?
- What is playing or ready to play?
- Where is my music coming from?
- What is the next useful action?

Home should expose quick actions for Library, Search, Player, phone files, and joining a host. It should not become a marketing page or a wall of settings.

Pairing from Home should make the QR scanner the central action. Manual invite paste can expand as a fallback, while split URL/token controls belong in Technical.

## Library Rules

Library should privilege playable music over diagnostics:

- Search stays near the top.
- Playlists and queue state are visible.
- Track rows emphasize title, artist, album, artwork, and source quality.
- Missing/unavailable tracks are clear but not alarming.

Advanced host details belong in Share or diagnostics unless they explain why music cannot play.

## Player Rules

Player is the emotional center:

- Artwork gets the largest stable area.
- Title/artist/album are readable before technical metadata.
- Controls are large and predictable.
- Queue access is nearby.
- Source quality is honest and visible without crowding playback.

## Files Rules

Files is for local phone audio:

- Import/select audio.
- Play selected phone files natively.
- Start temporary phone hosting only after files are selected.
- Explain phone hosting as temporary and opt-in.

Files should not pretend Android is an always-on desktop server.

## Technical Rules

Technical is the networking and J.A.M. control surface behind the profile/status control:

- Join/Pair host.
- Host this phone.
- Start or inspect J.A.M. state later.
- Show route mode: Auto, Local only, Direct remote, My relay, Relay fallback.
- Show trusted devices and revoke when permissions allow.
- Show diagnostics in human terms.

Normal users should see "Auto" and clear status only when they choose to open this area. Advanced users can open deeper controls later.

## Comfort Principles

- Keep bottom navigation stable and small.
- Use short labels with familiar music-app meanings.
- Prefer progressive disclosure over exposing every network detail at once.
- Keep every screen vertically scrollable.
- Avoid nested cards and dense developer dashboards.
- Use state chips for status, not paragraphs of explanation.
- Preserve local-first language: host, trusted device, phone files, J.A.M., source.
- Use J.A.M. for shared listening: Joined Audio Mesh.

## Debuggability Without Dev-App Feel

Diagnostics should be present, but not dominant. The Technical surface should summarize:

```text
connection state
network mode
phone host state
live revision / poll interval
permissions
trusted-device status
```

Deeper logs, raw route candidates, token previews, and failure codes can live behind future advanced diagnostics.
