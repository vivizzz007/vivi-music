# AGENTS.md

Instructions for AI coding agents working in this repository.

## 1. Project overview

**VIVI Music** is an open-source (GPL-3.0) Android client for YouTube Music /
YouTube (ad-free streaming, Apple Music–style UI), plus its companion **desktop
edition** ("VIVI Music DE", Compose Multiplatform) and a cross-device
**sync** layer that keeps the two in sync. It is a fork of the
ViMusic/InnerTune/SimpMusic family.

- **Language/build**: Kotlin 2.x, Java 21 toolchain, Gradle Kotlin DSL.
- **Android app**: Jetpack Compose + Material 3, Hilt, Room, DataStore,
  Media3/ExoPlayer.
- **Desktop app**: Compose Multiplatform (native Windows / Linux / macOS).
- **Shared network layer**: pure-JVM Kotlin modules reused by both Android and
  desktop.

## 2. Architecture and module structure

`settings.gradle.kts` declares `rootProject.name = "vivimusic"` and these modules:

| Module | Type | Role |
|---|---|---|
| `app` | Android (`com.android.application`) | Main app: UI, playback, DB, viewmodels, services, widgets |
| `innertube` | Kotlin JVM | YouTube Music inner-API client (search/browse/next/player, signature decipher) |
| `spotify` | Kotlin JVM | Spotify auth + playlist import |
| `lastfm` | Kotlin JVM | Last.fm scrobbling |
| `kizzy` | Kotlin JVM | Discord Rich Presence (WebSocket gateway) |
| `shazamkit` | Kotlin JVM | Shazam-style song recognition |
| `jiosaavn` | Kotlin JVM | JioSaavn streaming provider (CDN link decryption) |
| `lyricsProvider` | Kotlin JVM | Lyrics providers (KuGou, LrcLib, Musixmatch, PaxSenix, …) |
| `sync` | Kotlin JVM | Cross-device sync: data model + WebSocket client (pairing, push/pull) |
| `desktop` | Kotlin JVM + Compose Multiplatform | Desktop app (reuses the JVM modules above) |
| `canvas`, `artistvideo`, `applecanvas`, `vivimusiccanvas` | Android | Animated canvases / visualizers |
| `sync-server` | Node.js (not a Gradle module) | WebSocket relay for Android↔Desktop pairing + mailbox |

Key paths:

- `app/src/main/kotlin/com/music/vivi/` — app code (see `ui/`, `playback/`,
  `viewmodels/`, `db/`, `constants/`, `di/`, `utils/`, `devicesync/`,
  `listentogether/`).
- `app/src/main/res/values*/strings.xml` — Android string resources
  (localization, see §6).
- `app/src/main/kotlin/com/music/vivi/constants/PreferenceKeys.kt` — all
  DataStore preference keys + `LanguageCodeToName` map.
- `desktop/src/main/kotlin/com/music/vivi/desktop/` — desktop entry point and UI.
- `.github/workflows/` — CI (per-OS desktop builds + auto-release).

The `innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`, `lyricsProvider`,
`jiosaavn`, and `sync` modules are **pure JVM**: do not introduce Android
dependencies there, or you break the desktop build.

## 3. Code conventions and development guidelines

- **Branch**: `vivi-music-de`.
- **Commit style**: Conventional Commits (`feat:`, `fix:`, `ci:`, `refactor:`,
  `docs:`, `chore:`, `perf:`, …) with an optional scope, e.g.
  `feat(sync): …`.
- **Short commit titles**: keep the subject line as short as possible (aim for
  ~50 characters) and put the rest — what changed, why, and any extra notes —
  in the commit **body** (a blank line after the subject, then one or more
  lines/bullets). Do not cram the whole summary into the title.
- Commit and push after making changes, when asked (and per the project's
  standing rule to commit+push after every modification).
- **Release-triggering commits (`v` prefix)**: any change to program code or to
  anything that affects the release assets (the `desktop` module,
  `.github/workflows/`, `installer/`, `version.txt`, `desktop/build.gradle.kts`,
  icons, the shared JVM modules) MUST be committed and
  pushed with a commit message starting with `v` (e.g. `v6.0.5_DE-1.0.0`,
  optionally followed by a short description after `:`), so the auto-release
  runs and the result can be verified. The `sync-server/` relay is deployed
  **separately** (Render Blueprint `render.yaml`) and does **not** trigger the
  auto-release. Documentation-only changes (README, AGENTS.md, CHANGELOG.md,
  TODO.md) do **not** need the `v` prefix.
- Do not commit unrelated files (stray artifacts, debug dumps) unless relevant.
- **Keep `TODO.md` up to date**: every time you change the program (feature,
  fix, ported screen, workflow change), reflect it in `TODO.md` — mark done
  items `[x]`, in-progress `[~]`, and add new items as needed. Do not leave
  `TODO.md` stale after a change.
- Match the existing conventions of the file you edit (naming, formatting,
  KDoc style). Do not reformat untouched code.
- Kotlin formatting: keep to the project's existing style; do not run a global
  formatter that rewrites unrelated lines.
- Verify non-trivial changes by compiling the affected module
  (`./gradlew :module:compileKotlin`, `:app:compileUniversalFossDebugKotlin`,
  `:desktop:compileKotlin`) before committing.

### Commit co-author rule — MANDATORY

**NEVER add yourself (the agent / client) as a co-author of a commit** unless the
user explicitly asks for it in that message. Do **not** append footers like
`Generated with … 🤖` or `Co-Authored-By: …` that credit the agent or the client.
Write a normal conventional commit message.

## 4. Golden rule: "If it works, don't touch it"

**Do not refactor, rewrite, or modify modules, files, or functions that are
already working and stable**, unless one of these is true:

1. It is **strictly necessary** to implement the requested feature or fix, or
2. The user **explicitly asks** for the refactor.

Prefer the smallest change that satisfies the request. Do not "clean up" or
"improve" unrelated code while you work. When a change could break existing
behavior, state the risk before editing and, when in doubt, ask.

## 5. Versioning and CHANGELOG — MANDATORY

### Semantic Versioning (SemVer)

Every version bump follows **SemVer**: `MAJOR.MINOR.PATCH`.

- **MAJOR** — breaking changes (incompatible API/behavior).
- **MINOR** — new features, backward-compatible.
- **PATCH** — backward-compatible fixes.

The agent must **autonomously advance the version** as part of each change that
warrants it (no need to wait for the user to ask). Update **all** of these to
keep them in sync:

1. `version.txt` — single source of truth for release metadata (mobile
   version + code + channel, DE version + code + channel — see "Desktop
   versioning" below).
2. `app/build.gradle.kts` — `versionName` (SemVer string) and `versionCode`
   (monotonically increasing integer; the Android requirement is that
   `versionCode` always increases on each release).

When in doubt about which segment to bump, prefer PATCH for fixes and MINOR for
features; only use MAJOR for genuinely breaking changes.

**Which version to bump depends on what changed** (this is the rule the user
considers obvious):

- A change to the **Android app** (`app/`, or an Android-only module/behavior)
  bumps the **mobile** version: `version.txt` line 1 **and**
  `app/build.gradle.kts` `versionName` (+ `versionCode`). Also advance
  `version.txt` line 2 (mobile version code) to match `versionCode`.
- A change to the **desktop edition** (`desktop/`, its build/installer, the
  `.github/workflows/` release pipeline, or a desktop-only behavior) bumps the
  **DE** version: `version.txt` line 4 (+ line 5 version code by 1).
- A change that affects **both** editions bumps **both** versions.

Never bump the DE version for a mobile-only change, and never bump the mobile
version for a DE-only change.

#### Desktop versioning (`<mobile>_DE-<de>` + channel)

Desktop releases are distinguished from Android releases with a combined
version of the form `<mobile>_DE-<de>` (e.g. `6.0.5_DE-1.0.0`):

- `6.0.5` is the Android (mobile) version the desktop is paired with; `1.0.0`
  is the desktop ("DE") version — the program's own SemVer.
- `version.txt` holds the release metadata on **six lines** (comment lines
  prefixed with `#` may follow): line 1 = mobile version, line 2 = mobile
  version code, line 3 = mobile release channel, line 4 = DE version, line 5 =
  the desktop **version code** (a small monotonic counter matching the number
  of DE releases, e.g. `57` — shown in the About screen, and bumped by 1 on
  every DE release), line 6 = DE release channel. The Android app version also
  stays numeric in `app/build.gradle.kts` (`versionName` / `versionCode`,
  e.g. `6.0.5` / `57`).
- Release channels (lines 3 and 6): the **DE** channel (line 6) drives the
  desktop release — `stable` (or empty) publishes a stable GitHub release;
  any other value (`rc`, `beta`, `alpha`, `nightly`, …) publishes a
  pre-release. The channel is shown (uppercased) in the About screen. The
  mobile channel (line 3) is informational for the Android side.
- The GitHub release title and desktop artifact filenames use the full
  version (`VIVIMusic-6.0.5_DE-1.0.0-setup.exe`, …). Release **tags carry no
  `v` prefix**: stable releases use the bare version (`6.0.5_DE-1.0.0`), while
  non-stable releases append the channel (`6.0.5_DE-1.0.0-nightly`). The `v`
  prefix is used **only** in commit messages, as the auto-release trigger.
- Windows/macOS installers need a purely numeric `MAJOR.MINOR.PATCH`
  (jpackage JDK-8283707; Inno Setup `AppVersion` too), so the
  **installer/package version is the DE version** (`1.0.0`, the part after
  `DE-`). `desktop/build.gradle.kts` derives both values from `version.txt`
  (`fullVersion` for display, `numericPackageVersion` for jpackage) and
  generates `AppInfo` so the About screen can show `fullVersion` + channel.
  Keep that derivation in place — do not put the full `_DE-` version into
  `packageVersion`.

### CHANGELOG.md — Keep a Changelog

Update `CHANGELOG.md` on **every important change**, following
[Keep a Changelog](https://keepachangelog.com/). Use exactly these sections:

- `Added` — for new features.
- `Changed` — for changes in existing functionality.
- `Deprecated` — for soon-to-be-removed features.
- `Removed` — for removed features.
- `Fixed` — for bug fixes.
- `Security` — in case of vulnerabilities.

Keep an `## [Unreleased]` section at the top; when a version is released,
convert it to a dated entry (`## [X.Y.Z] - YYYY-MM-DD`) and add the new version
to the top of `CHANGELOG.md`. Omit sections that have no entries.

**Desktop-specific entries are marked with `[DE]`** (e.g.
`- [DE] New desktop feature.`), so desktop and Android changes stay
distinguishable in the changelog. Desktop releases use the combined
`<mobile>_DE-<de>` version (`## [6.0.5_DE-1.0.0] - …`).

## 6. Localization (multilingual support)

The app is translated through Android string resources. **English is the
primary language** (the source of truth); every other language is a
translation of it.

The **desktop edition** is English-first too, using the same 49-language list
(locale tag → native name) in
`desktop/src/main/kotlin/com/music/vivi/desktop/Languages.kt`, with strings in
`Localization.kt` (English source of truth; other languages fall back to
English until translated). The language is chosen on first launch and can be
changed from the desktop Language menu.

> **Rule (always)**: when you modify code you MUST complete ALL missing
> translations for every new or changed string across all supported languages —
> never leave a key with an English-only fallback. For the desktop edition,
> add the missing entries to the `EXTRA_TRANSLATIONS` tables under
> `scripts/desktop_extra_translations*.py` and re-run
> `python3 scripts/generate_desktop_localization.py` so `Localization.kt` stays
> complete, then compile `:desktop`.

### Structure

- `app/src/main/res/values/strings.xml` — **default/English** strings.
- `app/src/main/res/values-<locale>/strings.xml` — one folder per language
  (e.g. `values-it/`, `values-de/`, `values-zh-rCN/`).
- Some folders also contain `vivi_strings.xml` and `updater_strings.xml`
  (app-specific and updater strings). Keep the same set of files per language
  as English when adding new translatable strings.
- The list of **selectable app languages** lives in code, in
  `app/src/main/kotlin/com/music/vivi/constants/PreferenceKeys.kt`, in the
  `LanguageCodeToName` map (locale tag → display name).

### How to add a new language

1. Create the resource folder for the locale, e.g.
   `app/src/main/res/values-<locale>/`, and add a `strings.xml` that translates
   every key from `values/strings.xml`. Do **not** invent new keys; translate
   the existing English keys.
2. Add the language to the `LanguageCodeToName` map in `PreferenceKeys.kt` so it
   appears in the language picker.
3. If the language was requested but is not in the supported list below, confirm
   with the user first.

### Supported languages

English is the base language. The supported translations are (display name →
locale tag):

| Language | Locale |
|---|---|
| English (primary) | `values/` |
| Azərbaycan dili | `az` |
| Bosanski | `bs` |
| Català | `ca` |
| Čeština | `cs` |
| Deutsch | `de` |
| Eesti | `et` |
| Español | `es` |
| Euskara | `eu` |
| Filipino | `fil` |
| Français | `fr` |
| Hrvatski | `hr` |
| Bahasa Indonesia | `id` |
| Italiano | `it` |
| Lietuvių | `lt` |
| Magyar | `hu` |
| Bahasa Melayu | `ms` |
| Nederlands | `nl` |
| Norsk bokmål | `nb` |
| Polski | `pl` |
| Português | `pt` |
| Română | `ro` |
| Slovenčina | `sk` |
| Slovenščina | `sl` |
| Српски | `sr` |
| Suomi | `fi` |
| Svenska | `sv` |
| Tiếng Việt | `vi` |
| Türkçe | `tr` |
| Ελληνικά | `el` |
| Беларуская | `be` |
| Български | `bg` |
| Русский | `ru` |
| Українська | `uk` |
| العربية | `ar` |
| हिन्दी | `hi` |
| অসমীয়া | `as` |
| বাংলা | `bn` |
| ਪੰਜਾਬੀ | `pa` |
| தமிழ் | `ta` |
| తెలుగు | `te` |
| മലയാളം | `ml` |
| ไทย | `th` |
| ខ្មែរ | `km` |
| 한국어 | `ko` |
| 简体中文 | `zh-rCN` |
| 繁體中文 | `zh-rTW` |
| 日本語 | `ja` |
