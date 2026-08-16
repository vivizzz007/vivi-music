# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Desktop-specific changes are marked with `[DE]`. Desktop releases use a combined
version `<mobile>_DE-<desktop>` (e.g. `6.0.5_DE-1.0.0`), where the desktop part is
the program's own SemVer. `[APK]` marks mobile-only changes.

## [Unreleased]

## [6.4.3_DE-1.33.13] - 2026-08-16

### Fixed

- [DE] Apostrophes now render correctly everywhere instead of showing a
  literal `\'`. The desktop localization generator now decodes Android's
  `\'` resource escape (plus `\n`/`\t`/`\"`/`\\`) into real characters before
  re-encoding them as Kotlin string literals.

## [6.4.3_DE-1.33.12] - 2026-08-16

### Changed

- [DE] GitHub release titles now use the `Vivi Music <mobile>_DE <desktop>`
  format (e.g. `Vivi Music 6.4.3_DE 1.33.12`) instead of `Vivi Music DE
  <mobile>_DE-<desktop>`. Tags remain unchanged.

## [6.4.3_DE-1.33.11] - 2026-08-16

### Fixed

- [DE] Crash when changing the update-check interval: selecting an option in
  the frequency dropdown no longer throws "layouts are not part of the same
  hierarchy". The popup is now dismissed before the interval state (which
  reflows that row) is applied.

## [6.4.3_DE-1.33.10] - 2026-08-16

### Fixed

- [DE+APK] Device-sync regression: a transient network drop (or a socket
  reconnecting) no longer tears down a healthy pairing. Both relays now wait a
  15-second grace period for the device to reconnect before unpairing, and only
  the device's live socket triggers the unpair. Closing an app still un-pairs
  the other side (after the grace period).

### Note

- The cloud relay needs a redeploy of `sync-server/server.js` for this to apply
  over `wss://`; the LAN relay is fixed immediately.

## [6.4.3_DE-1.33.9] - 2026-08-16

### Added

- [DE] Backup & restore sub-menu in Settings: export the desktop settings to a
  `.backup` file (native save dialog) and import them back (native open
  dialog). Importing preserves the device id and first-launch date, drops any
  stale pairing, and prompts a restart to apply.

## [6.4.3_DE-1.33.8] - 2026-08-16

### Changed

- [DE] The GitHub logo in About → Community is now a vector icon (ported from
  the mobile app) instead of a static PNG, so it tints with the accent color
  and adapts to dark/light mode.

## [6.4.3_DE-1.33.7] - 2026-08-16

### Changed

- [DE+APK] Closing either app (mobile or desktop) now un-pairs both devices.
  The relays (cloud `sync-server` and the desktop LAN relay) detect the socket
  close, clear the pair, and tell the still-open peer it is no longer paired,
  so it stops showing "paired" for a peer that is gone.

### Note

- The cloud relay needs a redeploy of `sync-server/server.js` for this to take
  effect over `wss://`; the LAN relay works immediately.

## [6.4.3_DE-1.33.6] - 2026-08-16

### Fixed

- [DE+APK] Volume now syncs as the **system** volume: raising/lowering the
  Android volume (rocker or player bar) drives the desktop volume and vice
  versa, and the change is pushed immediately (no longer only on the periodic
  re-sync tick). This also fixes the volume bar position not following on the
  other device while the audible level did.

## [6.4.2_DE-1.33.6] - 2026-08-16

### Added

- [DE+APK] The desktop QR code now embeds the relay address **and** the current
  6-digit pairing code (`vivimusic://pair?addr=…&code=…`). Scanning it on the
  phone auto-fills both the server URL and the code, so you only verify the code
  and tap Pair. Plain `ws://` URLs still work for manual entry.

## [6.4.1_DE-1.33.5] - 2026-08-16

### Added

- [DE] The player now shows its load state while a track starts: "Resolving
  audio…" then "Downloading…" with a spinner, in both the full player and the
  mini-player, so you can tell it's working instead of appearing frozen.

## [6.4.1_DE-1.33.4] - 2026-08-16

### Fixed

- [DE] Playback now retries automatically: when the stream fails to resolve or
  download (e.g. a stale googlevideo 403), the player rotates the guest
  identity, re-resolves a fresh stream URL and retries up to 3 attempts before
  surfacing the error.

## [6.4.1_DE-1.33.3] - 2026-08-16

### Changed

- [DE] Completed the translations for the playlist and song-menu strings
  (rename / delete playlist / confirmation / empty / not-found / song count /
  like / library / share, …) across all 47 supported languages. The
  delete-playlist confirmation now shows the desktop message instead of the
  Android "Really delete … %s" template.

## [6.4.1_DE-1.33.2] - 2026-08-16

### Added

- [DE] Full song context menu (⋮): like / unlike, add to / remove from library,
  add to playlist and share (copies the YouTube Music link to the clipboard).
  Like and library actions use the signed-in YouTube account; a non-invasive
  snackbar confirms clipboard copies.

## [6.4.1_DE-1.33.1] - 2026-08-16

### Added

- [DE] Drag-to-reorder inside the playlist detail screen (drag the ⠿ handle);
  the new order is saved and synced like any other playlist edit.

## [6.4.1_DE-1.33.0] - 2026-08-16

### Added

- [DE] Local playlist system: create / rename / delete playlists, add songs to
  a playlist from any song row (Home, Search, Library, Album, Artist, Playlist
  and History), and a per-playlist detail screen with play-all and per-song
  remove.
- [DE+APK] Playlists now sync between the desktop and the phone over the
  device-sync channel: the full playlist (name + ordered songs) plus a
  per-playlist edit timestamp is shared, and edits are merged with
  last-write-wins. The most recently updated copy of each playlist wins,
  deletions propagate as tombstones, and a change made on either device appears
  on the other.

### Changed

- [DE] The sidebar gains a "Playlists" entry that opens the local playlist list.

## [6.4.0_DE-1.32.2] - 2026-08-16

### Fixed

- [DE] The player's seek slider could only land on the start or the end: the
  track duration was reported as 0 because YouTube's fragmented MP4 has an empty
  `mdhd` (jcodec returns `totalDuration == 0`). The duration is now derived from
  the decoded AAC sample count, so the slider spans the whole track and seeking
  works anywhere.
- [DE] Seeking while paused now stays paused instead of forcing playback to
  resume.
- [DE] Stale or truncated cached audio files are now detected and re-downloaded,
  so a leftover `.m4a` from an interrupted download no longer decodes into
  silence.

## [6.4.0_DE-1.32.1] - 2026-08-16

### Fixed

- [DE] "Start LAN server" now retries the pairing-code request until the relay
  answers, so the 6-digit code is generated automatically and reliably right
  after starting the server.

## [6.4.0_DE-1.32.0] - 2026-08-16

### Added

- [DE] Developer options "Title bar only" display mode (stats only in the
  window title, no overlay or separate window).
- [DE] Queue entry in the sidebar (opens the Queue screen directly).
- [DE] "View changelog" button in Settings → Updates.
- [DE+APK] Volume sync: the volume slider now syncs between the two devices.

### Changed

- [DE] Player layout is now two columns: a smaller artwork on the left and the
  seek bar + controls + volume on the right.
- [DE] Clicking the mini-player toggles the full player (open / go back).
- [DE] The About screen's GitHub row now uses the correct GitHub logo.
- [DE] README: smaller logo and @PiBOH added to Special Thanks.

### Fixed

- [DE] Stream resolution is more resilient: it retries transient failures and
  throttles guest-session refreshes so a track can start even with no paired
  device.

## [6.3.0_DE-1.31.0] - 2026-08-15

### Added

- [DE+APK] Periodic re-sync tick: while a track is playing, the position is
  re-pushed every 5 seconds so the paired device auto-corrects drift
  (buffering / clock skew) instead of waiting for the next seek/play/track
  event. A 250 ms tolerance skips near-no-op seeks so the correction doesn't
  glitch the audio.

## [6.2.5_DE-1.30.3] - 2026-08-15

### Changed

- [DE] The Changelog screen now uses a vertical version selector (left list of
  version buttons, like the mobile chips but top-to-bottom) with the selected
  version's details in a pane on the right, instead of stacking every version
  in one long scroll.

## [6.2.5_DE-1.30.2] - 2026-08-15

### Changed

- [DE] The About screen now shows real thumbnails: the developer's avatar
  (`author.png`) instead of the `< >` icon, and the GitHub logo on the
  GitHub Repository row (bundled from `[DE]_images/`).

## [6.2.5_DE-1.30.1] - 2026-08-15

### Changed

- [APK] The Devices entry in Settings now uses the same phone+monitor
  "devices" icon as the desktop edition, instead of the circular sync arrows.

## [6.2.4_DE-1.30.1] - 2026-08-15

### Fixed

- [DE+APK] Both sides now show the paired device's name: the Android Devices
  screen displays the desktop's machine name, and the desktop Device sync
  section displays the phone's make/model. The desktop now advertises its real
  hostname instead of the generic "Desktop", and the peer name is restored from
  the peer's snapshot on reconnect (not just at pairing time).

## [6.2.3_DE-1.30.0] - 2026-08-15

### Added

- [DE] Developer options improvements: the "Developer options" entry is now
  always visible in Settings (showing "Disabled" until enabled), and can be
  unlocked either by tapping the About "Version code" seven times or from that
  screen. Once unlocked, a non-invasive banner points to the settings screen.
  New options: a display profile (Full vs Performance — CPU/RAM/GPU only), a
  "movable overlay" toggle (drag the overlay anywhere on the main window,
  on by default), and a "show in title bar" toggle that puts live CPU/RAM
  usage in the window title.

### Fixed

- [DE] Starting the LAN server now reliably auto-generates the pairing code:
  the code request waits for the local relay connection to be established
  instead of racing the relay startup (which could leave the code ungenerated).

## [6.2.3_DE-1.29.0] - 2026-08-15

### Added

- [DE+APK] Precise, instant player sync between the desktop and the phone.
  Seeking now pushes the new position immediately (both directions) and the
  receiver applies it as a lightweight in-place seek instead of restarting the
  stream, so the two players stay aligned to the second. Playback positions
  now carry a timestamp and both devices estimate their clock offset to the
  relay (PING/PONG), so the live position is extrapolated during playback
  without clock-skew drift.

## [6.2.2_DE-1.28.6] - 2026-08-15

### Fixed

- [DE] Installing both the MSI and the Inno Setup EXE no longer leaves two
  "VIVI Music" entries in "Apps & features": the Inno Setup installer now
  uninstalls any previously-installed jpackage MSI of the app before copying
  its files, so a single uninstall entry remains.

## [6.2.2_DE-1.28.5] - 2026-08-15

### Fixed

- [DE] **Critical:** the packaged app still showed "Failed to launch JVM" at
  startup even after bundling the management modules, because `SystemMonitor`
  read a field before its initializer ran (a Kotlin forward-reference) and
  threw a NullPointerException during `DeveloperOptions.load()`. Fixed the
  declaration order and made dev-tools initialization non-fatal.

## [6.2.2_DE-1.28.4] - 2026-08-15

### Fixed

- [APK] The Android APK now reports version `6.2.2` (it was still showing
  `6.2.1`): `app/build.gradle.kts` `versionName`/`versionCode` were out of
  sync with `version.txt`.

## [6.2.2_DE-1.28.3] - 2026-08-15

### Fixed

- [DE] **Critical:** the packaged app no longer fails to start with
  "Failed to launch JVM". The dev tools (CPU/RAM/thread stats) use
  `java.lang.management` and `com.sun.management`, but jlink was bundling only
  the default modules; those two modules are now declared so the packaged
  runtime includes them.

## [6.2.2_DE-1.28.2] - 2026-08-15

### Fixed

- [DE] The Changelog screen now lists every version vertically (newest first)
  in a single scrollable list, so older versions are reachable with the mouse
  wheel — the previous horizontally-scrolling version chips were unusable on
  desktop.

## [6.2.2_DE-1.28.1] - 2026-08-15

### Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the
  desktop now keeps a fresh guest `visitorData` (like the Android app) and
  rotates it once when YouTube flags the request as a bot, so the googlevideo
  CDN stops rejecting the resolved stream URLs.

## [6.2.2_DE-1.28.0] - 2026-08-15

### Added

- [DE] The Updates screen now checks for updates automatically every time it
  is opened, and in the background at a configurable interval (manual only,
  6 hours, 12 hours, 24 hours, 3 days or 7 days), selectable in
  Settings → Updates.

### Fixed

- [DE] "Download" in the Updates screen no longer opens the browser instead of
  downloading in-app: the updater now picks the newest release that actually
  ships an installer for your OS (skipping releases whose build for your
  platform is missing) and only falls back to the release page when no
  installer exists. Nightly/alpha/beta/rc builds now include pre-releases by
  default so they can see updates without toggling the option.

## [6.2.2_DE-1.27.1] - 2026-08-15

### Fixed

- [DE] Content no longer gets clipped when the window is resized smaller: the
  Album/Artist/Playlist headers now shrink their title/artist text (ellipsis)
  instead of overflowing, and the LAN pairing screen ellipsizes and constrains
  the relay address next to the QR code.

## [6.2.2_DE-1.27.0] - 2026-08-15

### Added

- [DE] Developer options: tap the About "Version code" seven times to enable
  them. Once enabled, a new "Developer options" settings screen lets you show
  live CPU, RAM and network stats (download/upload speed + total traffic), the
  GPU device, thread count, uptime, OS/Java info and the paired phone
  name/model — either as a non-invasive collapsible overlay in the main window
  or in a dedicated window.

## [6.2.2_DE-1.26.4] - 2026-08-15

### Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the
  desktop resolver stopped using the `WEB`/`WEB_REMIX` clients (their
  googlevideo URLs require a PoToken the desktop cannot generate) and now uses
  only PoToken-free clients (added `VISIONOS` and `IOS_MUSIC`). It also only
  applies the n-parameter throttle transform to web clients, so
  Android/iOS/VisionOS stream URLs are no longer corrupted into a 403.

## [6.2.2_DE-1.26.3] - 2026-08-15

### Fixed

- [DE] The About screen "Version code" was derived from the SemVer
  (`1.26.0` → `12600`), which looked like a huge number. It is now an explicit
  monotonic counter stored in `version.txt` (line 4) that tracks the number of
  DE releases (currently 57).

## [6.2.2_DE-1.26.2] - 2026-08-15

### Changed

- [DE] Starting the LAN server now automatically generates a pairing code, and
  the code + "Generate code" button are shown to the right of the QR code
  instead of below it.

## [6.2.2_DE-1.26.1] - 2026-08-15

### Fixed

- [DE+APK] Device pairing is now kept in sync across both devices: unpairing
  from the phone or the desktop unpairs the other side, and stopping the LAN
  server notifies the phone to unpair too. Reconnecting to a relay that no
  longer knows the pair (for example after the desktop is restarted) now clears
  the stale "paired" state instead of leaving it stuck.

## [6.2.1_DE-1.26.0] - 2026-08-15

### Added

- [DE] Redesigned the About screen to mirror the mobile layout: centered
  title + version/channel badge, a Developer section (PiBOH — lead developer
  of the DE edition — with website link), a Community section (GitHub repo +
  Telegram), and an App info section showing the first-launch date (not the
  last-update install date), the numeric version code and the GPL-3.0 license
  link.

## [6.2.1_DE-1.25.0] - 2026-08-15

### Added

- [DE+APK] Library sync over the device-sync channel: the mobile app now
  observes its library (liked songs, bookmarked albums/artists/playlists) and
  pushes a `LibrarySnapshot` whenever it changes; the desktop receives,
  persists and exposes it (and pushes its own).

## [6.2.1_DE-1.24.2] - 2026-08-15

### Fixed

- [DE] "HTTP 403 downloading audio" is now far more robust: the resolver
  returns an ordered list of candidate stream URLs (NewPipe plus every
  innerTube client), and the player tries them in order — retrying without the
  `Range` header when a request is refused — instead of giving up after the
  first URL. NewPipe URLs now use the decrypted `getUrl()` result with the
  n-param transform applied.

## [6.2.1_DE-1.24.1] - 2026-08-15

### Changed

- [DE] The update notification now offers "Open installer" (instead of
  downloading again) when the installer for that version is already present
  in the downloads folder.

## [6.2.1_DE-1.24.0] - 2026-08-15

### Added

- [DE] A non-invasive banner now appears when a newer release is available,
  with "Install now" (downloads and launches the installer) and a dismiss
  button. It is shown once per new version.

## [6.2.1_DE-1.23.0] - 2026-08-15

### Added

- [DE] The settings sub-screens now carry real functionality instead of being
  empty shells:
  - **Appearance**: pure black background toggle (true black in dark mode).
  - **Player & audio**: audio quality (Auto / High 256kbps / Low 128kbps,
    wired to the itag 141/140 picker), "remember shuffle and repeat" across
    restarts, and "persistent queue" (the queue is saved and restored between
    sessions).
  - **Lyrics**: adjustable lyrics text size.

## [6.2.1_DE-1.22.1] - 2026-08-15

### Fixed

- [DE] Audio download no longer fails with "HTTP 403 downloading audio".
  googlevideo ties a stream URL to the client that requested it, so the player
  now downloads with the same User-Agent used to resolve the URL (and a
  `Range: bytes=0-` header), instead of a fixed browser UA that YouTube
  rejected.

## [6.2.1_DE-1.22.0] - 2026-08-15

### Added

- [DE] Search now has mobile-style filter chips (All / Songs / Videos / Albums /
  Artists / Playlists) backed by the innerTube filtered search, plus live query
  suggestions as you type.
- [DE] Library tabs now use Material 3 filter chips and the Songs tab gains a
  "Shuffle all" action.
- [DE] Album and Playlist screens gain a "Shuffle" action next to "Play all".

## [6.2.1_DE-1.21.0] - 2026-08-15

### Added

- [DE] Settings are now organized into mobile-style sub-screens (Language,
  Updates, Appearance, Player & audio, Account, Devices, Content, Lyrics,
  Privacy, Storage, About) instead of one long scrollable page.
- [DE] New Content sub-screen: pick the YouTube content language and region
  (innerTube `hl`/`gl`), applied live and persisted across restarts.
- [DE] New Lyrics sub-screen with a "synced lyrics" toggle that enables or
  disables line-by-line highlighting.
- [DE] New Privacy sub-screen to clear the session, cache and downloaded
  installers.

## [6.2.1_DE-1.20.1] - 2026-08-15

### Changed

- [DE] The auto-release no longer builds the APK itself: it waits for the
  existing "CI & Debug Build" (`build.yml`) run on the same commit and attaches
  its debug APK artifact to the desktop release. The dedicated
  `release-mobile.yml` build workflow was removed.

## [6.2.1_DE-1.20.0] - 2026-08-15

### Fixed

- [DE] Audio playback now works. YouTube serves its `audio/mp4` streams as
  fragmented MP4 (DASH fMP4, `ftyp` brand "dash"), whose samples live in
  `moof`/`trun` boxes instead of the `moov` sample table — which `jaad`'s
  `MP4Container` demuxer does not understand, so every track failed with
  "No audio frames to decode". The player now walks the fragments directly
  with jcodec and decodes with the bundled jaad AAC decoder.
- [DE] The stream resolver now prefers AAC-LC (codec `mp4a.40.2`, itag 140/141)
  over HE-AAC/SBR (`mp4a.40.5`, itag 139), which jaad cannot decode ("FIL
  element overread").
- [DE] Language sync with the mobile app now maps the differing locale codes
  (mobile `no`/`pt-PT`/`zh-CN`/`zh-TW` ↔ desktop `nb`/`pt`/`zh-rCN`/`zh-rTW`),
  so changing the language on one device is correctly reflected on the other.

### Added

- [DE] All text (errors, options, settings, LAN server details, etc.) is now
  selectable and copyable across the whole desktop app.

## [6.2.1_DE-1.19.3] - 2026-08-15

### Changed

- [DE] APK delivery for desktop releases: a dedicated workflow
  (`release-mobile.yml`, a copy of the mobile CI adapted for `vivi-music-de`)
  builds and signs the GMS + FOSS APKs on this branch and attaches them to the
  release created by `auto-release.yml`. The in-pipeline `build-android` job was
  removed from `auto-release.yml` to avoid building the APK twice.

## [6.2.1_DE-1.19.2] - 2026-08-15

### Fixed

- [DE] Content now scales with the window: the Player artwork resizes with the
  window width (180–360dp) instead of being fixed at 300dp, and Library cards
  fill their adaptive grid cells instead of a fixed 140dp width.

## [6.2.1_DE-1.19.1] - 2026-08-15

### Changed

- [APK] Bumped the mobile version to 6.2.1 (versionCode 77).
- The desktop auto-release now builds and attaches the Android APK directly from
  this branch instead of trying to download it from the mobile CI release
  (`release.yml` runs on `main`, which had fallen out of sync), so the APK now
  reliably appears among the release assets.

## [6.2.0_DE-1.19.1] - 2026-08-15

### Changed

- [APK] The pairing-code field on Android now opens the numeric keypad and only
  accepts the 6 digits of the code shown by the desktop.

## [6.2.0_DE-1.19.0] - 2026-08-15

### Changed

- [DE] The About → Changelog screen now matches the mobile app: a horizontally
  scrollable row of version chips on top, and, for the selected version, a bold
  primary title plus its Added/Fixed/Changed sections rendered as bullet items
  (instead of a flat markdown dump of the whole file).

## [6.2.0_DE-1.18.0] - 2026-08-15

### Added

- [DE] The desktop pairing code now shows a live countdown until it expires
  (5 minutes) and offers a "Generate new code" button to mint a fresh code.

### Changed

- [DE] The desktop Device sync section is now generate-only: it no longer shows
  an "enter code" field. The desktop generates the 6-digit code, and the phone
  enters it.
- [APK] The Android Devices screen is now insert-only: the "Generate code"
  button was removed, leaving just the "enter code" + Pair flow that reads the
  code shown by the desktop.

## [6.2.0_DE-1.17.1] - 2026-08-15

### Fixed

- [DE] Fixed desktop audio not playing: the player reused a stale cache file when
  switching tracks, silently swallowed every decode/download/audio-device error
  (so playback stopped with no message), and did not verify the downloaded file.
  Failures now surface a clear reason in the player, and the decode pipeline is
  more robust.
- [DE] The app now closes itself right after launching an update installer so the
  installer can replace the running files (updates could not install otherwise).

### Changed

- [DE] The About → Changelog screen now fetches `CHANGELOG.md` live from the
  repository (falling back to the bundled copy when offline), so it always shows
  the current changelog without waiting for a new build.

## [6.2.0_DE-1.17.0] - 2026-08-15

### Added

- [DE] Ported the Home screen to the Android app's style: filter chips, mobile-style
  section headers (label + bold primary title, "Play all" button, chevron), songs-only
  sections rendered as horizontal song lists (Quick picks style), mixed sections as
  card carousels, and a Mood & genres section whose buttons open a new generic
  Browse screen.
- [DE] The QR code in Device sync is now always rendered on a solid white card so it
  scans reliably in both light and dark themes.

## [6.2.0_DE-1.16.2] - 2026-08-15

### Fixed

- [DE] Fixed the changelog being unclear about desktop versions: releases are
  now versioned as `<mobile>_DE-<de>` sections in `CHANGELOG.md` (instead of
  everything accumulating under "Unreleased"), and the changelog screen shows
  the current DE version and channel at the top.

## [6.2.0_DE-1.16.1] - 2026-08-15

### Fixed

- [DE] Fixed the language selector always showing English: the desktop string
  table now ships real translations for 45 languages (generated from the
  Android app's `strings.xml` / `vivi_strings.xml` via
  `scripts/generate_desktop_localization.py`). Keys without a translation
  still fall back to English.

## [6.2.0_DE-1.16.0] - 2026-08-15

### Added

- [DE] Started the pixel-perfect UI port from the Android app: the theme now
  uses the same seed-based Material 3 palette (materialKolor TonalSpot, seed =
  accent color) as the mobile app, and the fixed text sidebar was replaced with
  a collapsible/expandable sidebar with Material icons + labels (persisted).

## [6.2.0_DE-1.15.1] - 2026-08-15

### Fixed

- [DE] Fixed the update check picking the wrong release: it now selects the
  desktop release with the highest `_DE-<version>` tag instead of the first /
  "latest" entry (GitHub orders releases by publish date, not by version), so
  an older tag no longer masks a newer one. The release list window was also
  raised to 100 and the changelog notes follow the same highest-version rule.

## [6.2.0_DE-1.15.0] - 2026-08-15

### Added

- [DE] Wired device sync end-to-end: the desktop now pushes its playback
  (track, queue, position, play/pause) and settings, and applies incoming
  snapshots — remote playback starts on the desktop player and language /
  theme / accent follow the phone. A persistent `DesktopSyncManager` owns the
  client + LAN relay for the whole app lifetime (no more state loss when
  leaving Settings), with echo suppression to avoid ping-pong loops.
- [APK] Android now pushes its playback to the desktop and applies incoming
  playback snapshots (desktop → phone), so starting a song on either device
  resumes on the other.

## [6.2.0_DE-1.14.3] - 2026-08-15

### Fixed

- [DE] Fixed most text not adapting to the dark/light theme: Material 3's
  `MaterialTheme` does not set `LocalContentColor`, so text without an explicit
  color fell back to black. The app root now provides `LocalContentColor =
  onBackground`, so titles, headers and other uncolored text follow the theme.

## [6.2.0_DE-1.14.2] - 2026-08-15

### Changed

- [DE] Debounced the Lyrics screen position updates: the highlighted line is
  now polled ~5×/s and only recomposed when it changes, instead of recomposing
  the list on every decoded-frame position update (~40×/s).

## [6.2.0_DE-1.14.1] - 2026-08-15

### Fixed

- [DE] Fixed the "Open installer" button not launching the downloaded
  installer: opening now falls back to the OS's native opener (`cmd /c start`
  on Windows, `open` on macOS, `xdg-open` on Linux) when `Desktop.open()`
  fails, and reports an error instead of failing silently.

## [6.2.0_DE-1.14.0] - 2026-08-15

### Added

- [DE] Added optional manual `DATASYNC_ID` / `VISITOR_DATA` fields to the
  desktop login screen as a fallback for when the automatic extraction from
  the music.youtube.com shell fails.

## [6.2.0_DE-1.13.0] - 2026-08-15

### Added

- [DE] Rebuilt the Player screen as a full Material 3 player: a seek slider
  with elapsed/total time, a volume slider, shuffle and repeat (off/all/one),
  proper Material icons and a large artwork presentation. Playback now reports
  the track duration and supports seeking (the stream is cached locally), the
  volume is adjustable, and the mini-player shows a progress bar.

## [6.2.0_DE-1.12.1] - 2026-08-15

### Fixed

- [DE] Fixed dark mode not repainting the page background: the app root now
  paints the theme's `background` color, so switching to dark converts the
  whole window instead of leaving the native light background showing through.

## [6.2.0_DE-1.12.0] - 2026-08-15

### Added

- [DE] Added the animated canvas to the Player: a blurred, slowly-zooming
  (Ken Burns) artwork background behind the track. Canvas artwork is resolved
  from the same providers as the Android app (Apple Music / Tidal / VIVI Music
  canvas); animated GIF/WebP URLs play via Coil, while video canvases
  (MP4/HLS) fall back to static art + zoom.

## [6.2.0_DE-1.11.0] - 2026-08-15

### Added

- [DE] Added synced lyrics: the Lyrics screen now parses LRC timestamps,
  highlights the current line and auto-scrolls to it as the song plays
  (falls back to plain text when lyrics aren't synced).

## [6.2.0_DE-1.10.0] - 2026-08-15

### Added

- [DE] Added drag-to-reorder to the Queue screen (drag the ⠿ handle), reusing
  the same `sh.calvin.reorderable` library as the Android app.

## [6.2.0_DE-1.9.0] - 2026-08-15

### Added

- [DE] Added YouTube login on desktop: paste the music.youtube.com `Cookie`
  header (no WebView needed), which auto-extracts the account's
  `DATASYNC_ID`/`VISITOR_DATA`, validates the session and persists it locally.
  History now works when signed in, and Library gained Songs / Albums / Artists
  / Playlists tabs (liked songs, albums, artists and playlists).

## [6.2.0_DE-1.8.0] - 2026-08-14

### Added

- [DE] Added an Updates section in Settings with in-app downloads: it detects
  the right installer for the host OS/arch (MSI/AppImage/DMG with EXE/DEB/PKG
  fallback), downloads it with progress % + speed, opens it, and can delete
  downloaded installers.
- [DE] Added Player & audio settings (autoplay next track) and a Storage
  section (cache size + clear cache) to Settings.
- [DE] Added a changelog screen (About → Changelog) showing the bundled
  `CHANGELOG.md` plus the latest GitHub release notes.

## [6.2.0_DE-1.7.0] - 2026-08-14

### Added

- [DE] Added a full playback queue: "add to queue" on every song row, "Play
  all" on albums/playlists, next/previous, auto-advance, and a Queue screen
  (jump / remove / clear).
- [DE] Added a History screen (sidebar) listing the user's listening history.

## [6.2.0_DE-1.6.0] - 2026-08-14

### Added

- [DE] Added a light/dark/system theme with a selectable accent color palette
  (Settings → Appearance), applied across the whole desktop app.

## [6.2.0_DE-1.5.1] - 2026-08-14

### Fixed

- [DE] Fixed the desktop player showing "could not resolve the audio stream":
  the stream resolver now uses the same multi-client fallback chain as the
  mobile app (ANDROID_VR + 11 fallback clients, n-param deobfuscation and URL
  validation) instead of a single ANDROID_VR attempt that YouTube often answers
  with `LOGIN_REQUIRED`.

## [6.2.0_DE-1.5.0] - 2026-08-14

### Added

- [APK] Added LAN discovery to the Android Devices screen: "Find desktop"
  (mDNS/NSD `_vivimusic._tcp`) and "Scan QR code" auto-fill the relay server
  URL when pairing with VIVI Music DE over the same Wi-Fi. (Mobile version
  bumped 6.1.0 → 6.2.0.)

## [6.1.0_DE-1.5.0] - 2026-08-14

### Added

- [DE] Added LAN discovery aids to the desktop Device sync section: a QR code
  encoding the local relay address, and mDNS service registration
  (`_vivimusic._tcp`) so the Android app can discover/scan the desktop.

## [6.1.0_DE-1.4.0] - 2026-08-14

### Added

- [DE] Added offline LAN (same Wi-Fi) device pairing: the desktop can start a
  local WebSocket relay from Settings → Device sync, so the Android app can
  pair directly without the cloud relay.

## [6.1.0_DE-1.3.1] - 2026-08-14

### Changed

- [DE] The auto-release now attaches the mobile APKs by downloading them from
  the mobile CI release (tag `v<mobile>`) instead of rebuilding them in a
  separate Android job, making desktop releases much faster.

## [6.1.0_DE-1.3.0] - 2026-08-14

### Added

- [DE] Added an Updates section in Settings: an automatic update check on
  startup plus a manual "Check for updates" button, an opt-in toggle to include
  pre-releases, and a download link when a newer desktop release is available.

## [6.1.0_DE-1.2.1] - 2026-08-14

### Fixed

- [DE] Fixed the auto-release workflow's invalid YAML: `continue-on-error` is not
  allowed on a job that calls a reusable workflow, so the Android APK build is
  now made optional with per-step `continue-on-error` inside `build-android.yml`
  instead (the desktop release no longer requires the APK to succeed).

## [6.1.0_DE-1.2.0] - 2026-08-14

### Added

- [DE] Integrated self-contained audio playback (Phase 4): AAC stream
  resolution (NewPipe + ANDROID_VR) and a pure-Java AAC decoder (`jaad`)
  played through Java Sound — no external player or native codec required.
  The mini-player and Player screen now actually play/pause/resume songs and
  show the playback position.

## [6.1.0_DE-1.1.2] - 2026-08-14

### Changed

- [DE] Release notes now collapse the commit list into an expandable section
  when there are more than 7 commits.

## [6.1.0_DE-1.1.1] - 2026-08-14

### Changed

- [DE] The Android APK build in the auto-release is now optional (per-step
  best-effort), so a missing signing secret or failed APK build no longer
  blocks the desktop release.

## [6.1.0_DE-1.1.0] - 2026-08-14

### Added

- [DE] Full desktop UI: sidebar navigation with Home, Search, Album, Artist,
  Playlist, Library, Player, Lyrics and Settings screens, artwork thumbnails,
  and an Apple Music–style mini-player. (Library is a placeholder pending
  login; audio playback and the animated canvas are deferred to later phases.)

## [6.1.0_DE-1.0.4] - 2026-08-14

### Changed

- [DE] The auto-release now also builds and attaches the Android APKs
  (GMS + FOSS) to the same release, so each release ships desktop + mobile
  assets together.

## [6.1.0_DE-1.0.3] - 2026-08-14

### Fixed

- [DE] Fixed desktop device pairing: the desktop client now actually connects
  (the `connect()` call was unreachable) and defaults to the same relay URL as
  the Android app instead of the local `wss://localhost:8080` placeholder, so
  "Generate code" produces a code.

## [6.1.0_DE-1.0.2] - 2026-08-14

### Changed

- [DE] Split the Linux build into independent DEB and AppImage jobs so a
  failure in the AppImage step no longer blocks the DEB package (or the
  release).

## [6.1.0_DE-1.0.1] - 2026-08-14

### Changed

- [DE] The Windows build now produces both an Inno Setup installer and a
  jpackage MSI.
- [DE] Removed the Inno Setup wizard images that could make the installer open
  and immediately close on some systems.

## [6.1.0_DE-1.0.0] - 2026-08-14

### Added

- [APK] Added a "Devices" section in the Android Settings to pair the phone with
  VIVI Music DE (relay server URL, generate/join pairing code, unpair).

## [6.0.5_DE-1.0.0] - 2026-08-14

### Added

- [DE] Compose Multiplatform desktop target (`desktop` module) reusing the
  pure-JVM network modules.
- [DE] Native desktop icons (Windows `.ico`, macOS `.icns`, Linux `.png`) using
  the VIVI Music DE logo.
- [DE] Per-OS GitHub Actions builds (MSI/EXE, DEB/AppImage, DMG/PKG) and an
  auto-release workflow.
- [DE] Cross-device sync foundation: shared `sync` module, Node.js WebSocket
  relay (`sync-server/`), Android `DeviceSyncManager`, and desktop pairing UI.

### Changed

- Converted `innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`,
  `jiosaavn`, and `lyricsProvider` to pure-JVM Kotlin modules so they can be
  shared between Android and desktop.
- Desktop releases now use a combined `<mobile>_DE-<desktop>` version
  (e.g. `6.0.5_DE-1.0.0`): `version.txt` line 1 = mobile version, line 2 = DE
  version, line 3 = channel. The About screen shows the full version + channel;
  desktop changelog entries are marked `[DE]`.
- The release channel is now read from line 3 of `version.txt`: `stable` (or
  empty) publishes a stable release; any other value (`rc`/`beta`/`alpha`/
  `nightly`) publishes a pre-release.
- Release tags no longer carry a `v` prefix; non-stable releases append the
  channel to the tag (e.g. `6.0.5_DE-1.0.0-nightly`).
- [DE] The desktop UI is now English-first with a 49-language picker (first
  launch + Language menu); non-English strings fall back to English until
  translated.
- [DE] The Windows installer now performs a machine-wide install into
  `C:\Program Files\VIVIMusic` (requires admin rights) instead of a per-user
  install into `%LOCALAPPDATA%`.
- [DE] The Windows installer is now a branded Inno Setup wizard and shows a
  "successfully uninstalled" confirmation message after removal.

### Fixed

- [DE] Made `gradlew` executable in the repository and in the desktop build
  workflows (fixes `./gradlew: Permission denied` on Linux/macOS runners).
- [DE] Replaced the retired `macos-13` runner with `macos-15-intel` for the
  Intel macOS build.

### Security

- Cross-device sync traffic is currently TLS-only; end-to-end encryption is
  planned for a future release.
