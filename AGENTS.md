# Ctrl Music — guide for contributors and AI assistants

Ctrl Music is an ad-free, open-source YouTube Music player for Android (Kotlin, Jetpack Compose,
Media3). It is a GPL-3.0 fork of VIVI Music (which builds on Metrolist / InnerTune).

## Project map

| Path | What lives there |
|---|---|
| `app/src/main/kotlin/com/ctrlfxx/ctrlmusic/` | The app: screens, player, database, downloads |
| `…/ui/screens/` | Screens (Home, Search, Library, Album, Artist, Settings…) |
| `…/ui/player/` | Full player, mini player (`AppleMiniPlayer.kt`), queue |
| `…/ui/theme/Theme.kt` | Colors. `ctrlDark()` builds the dark look; `DefaultThemeColor` = Blue |
| `…/ui/screens/settings/ThemeSettings.kt` | Accent color list (`PaletteColors`) and Dynamic color switch |
| `…/playback/MusicService.kt` | Background playback service |
| `…/playback/DownloadUtil.kt` | In-app downloads (stored in a private cache) |
| `…/playback/SongExporter.kt` | "Save to phone": exports a download as `.m4a` to `Music/Ctrl Music` |
| `…/utils/YTPlayerUtils.kt` | Finds the playable audio URL for a song |
| `…/lyrics/` | Lyrics providers and `LyricsProviderRegistry` |
| `…/constants/PreferenceKeys.kt` | All settings keys |
| `innertube/` | YouTube Music API client (search, browse, player) |
| `spotify/` | Spotify login, playlists, liked songs, Canvas covers |
| `Motionartwork/` | Animated covers (Apple Music, Tidal, community) |
| `app/src/main/res/values/ctrl_strings.xml` | Add new English text here |

## Building

- You don't need Android Studio to get an APK: every push runs `.github/workflows/ctrl-build.yml`
  on GitHub Actions. Download `CtrlMusic.apk` from the run's **Artifacts**.
- Push a tag like `v1.0.1` to publish a GitHub Release with `CtrlMusic.apk` (arm64) and
  `CtrlMusic-universal.apk`.
- Release signing uses the repository secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`. Never commit a keystore.
- Locally (Android Studio, JDK 21): `./gradlew assembleArm64FossRelease`.

## Rules

1. Commit messages: `type(scope): short description` (e.g. `feat(player): add sleep timer`).
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts` for every release.
3. Don't change the Room database schema without adding a migration.
4. Ctrl Music is dark-only for now; light theme is planned for a later version.
5. Keep the GPL-3.0 license and the credit to VIVI Music / Metrolist.
6. Don't add trackers, analytics or ads.
