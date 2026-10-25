# Using Pixelody on a phone

The current track and queue should stay easy to reach while someone browses.
Sharing, tuning and host controls need room when they're opened, without filling
the player with connection details during ordinary listening.

The August 10, 2026 review recorded the navigation at that point. At that point,
the Compose shell used Home, Search, Library and Create as its persistent
destinations. Player, Queue, Profile, Files and Tools opened in context. An older
proposal put Player in the bottom navigation instead of Create; that was an idea
to test, not the interface to silently write instructions for.

Home should make it easy to resume, find the library or choose phone files, and
see which source is connected. Before pairing, joining a host may be the useful
next action. Once connected, that setup shouldn't keep getting in the way of
listening. The proposed QR-first entry had manual invite entry as a fallback,
with the more technical address/token controls farther in.

The library and player need the same attention to what someone is trying to do.
Keep search near browsing, queue access close to playback, and titles, artist
names and artwork readable before the technical metadata. If a source fails,
put the recovery beside it. Back should return to where someone came from,
closing the newest layer rather than leaving them somewhere unrelated.

The visual direction was related to desktop Studio without squeezing a desktop
layout onto a phone: dark surfaces, warm gold for identity and selection, green
for live playback/trusted-source state and cooler accents for metadata. Small
panels and aligned rows can hold useful detail, but nested cards and a wall of
status text make the player harder to use. Keep quality labels honest; the file's
format isn't a promise about Android's output path.

Phone files, paired-host music and temporary phone hosting are different uses.
The older Files and Technical proposals included local import/playback,
pairing, trusted devices and J.A.M. controls. Remote routes, relay choices and
portable hosting in those proposals weren't a claim that every option worked.
Use the installed build's labels and current code when writing setup steps.

For a change to this interface, try it while actually browsing and listening.
Check large text, narrow screens, touch targets, contrast, screen-reader names
and motion preferences, including a clear way out of advanced controls. A panel
can scroll when its content needs it; hiding normal playback controls behind
unnecessary scrolling isn't the intended result. Networking diagnostics should
help with a connection problem, and private tokens and paths shouldn't appear in
ordinary status text.

Familiar music apps are useful references for ergonomics. Their brand language,
icons and commercial artwork aren't assets to copy into Pixelody. The older screen-by-screen proposal remains in Git history.
