# Vista Cinema

A native Android streaming companion app for **phones, tablets and Android TV / Google TV**.
Browse and search movies and series, read cast and "where to watch" info, build a watchlist,
and stream titles through multiple embed sources. Ships with in-app updates delivered from its
own website.

**Release package:** 1.0.9 (versionCode 16), signed and verified October 29, 2026. The release manifest marks this as a required update. Min SDK 24 (Android 7.0+), target SDK 35. Check the public manifest for deployment status.

**Website / APK repository:** [lx-obsidian-labs/lx-obsidian-portal](https://github.com/lx-obsidian-labs/lx-obsidian-portal), branch `master`. Public downloads: [Vista Cinema](https://lxobsidianportal.co.za/vista.html). See `DEPLOYMENT.md` for the verified publishing destinations and current release status.

---

## Features

- **Vista AI Assistant & Voice** - Talk or chat with Vista Assistant for personalized movie discoveries,
  cast details, and spoiler-aware movie/series companion guides. NVIDIA NIM runs through an authenticated
  Cloudflare Worker with conversation history, profile alias, enabled taste preferences and a guide to
  Vista's actual features. The verified model is `nvidia/nemotron-3.5-lightning-30b-a3b`.
- **Floating Chat & Page Actions** - The Home FAB opens a chat sheet while preserving the current tab;
  the AI bottom tab opens full-screen. Assistant actions open supported app pages or Search with a query.
- **Hands-Free Voice Turns** - Tap the microphone once to listen, generate, speak and listen again.
  Say “stop listening” or tap Stop to end. The highest-quality available Android voice is selected;
  Settings provides access to Android voice downloads/language settings. Audio quality needs device testing.
- **Smart Cache & Playback Recovery** - Automatic video verification recovery engine that resolves stream
  failures, clears stale temporary segments, and performs controlled retries without interrupting active playback.
- **Clean Icon-Only Bottom Navigation** - Sleek bottom navigation bar with active indicator pills.
- **Home** - Continue Watching, Trending/Top 10 rows, "Because You Watched" recommendations,
  and a **Play Something** shuffle button.
- **Discover** - Genre, year and sorting filters across movies and series.
- **Search** - Movies, series and people with live results and editable search history.
- **Watchlist** - Double-tap any poster to instantly save or remove it; saved screen with swipe
  actions.
- **Details** - Backdrops, cast, trailers, "Where to Watch" (Watchmode), similar titles, "Ask Companion",
  and "More Like This" for series, seasons/episodes.
- **Player** - Media3/ExoPlayer with multiple fallback stream sources, **double-tap to seek**,
  autoplay of the next episode, brightness control, ad blocker, and **Repair Playback** recovery.
- **Watch History & Your Stats** - Automatically tracked lifetime viewing statistics, streak tracking,
  and searchable watch history.
- **Profile & Redesigned Settings** - Guest mode with a free 3-day trial, app themes, configurable cache size limits
  (250 MB - 2000 MB), streaming preferences, honest notification toggles, and an **in-app update check**.
- **Notifications** - In-app notification bell with the team's announcements and on-device push
  via Firebase Cloud Messaging (`vista_updates` topic, plus `vista_updates_za` / `vista_updates_row`
  for country-targeted sends).
- **Movie of the Day** - Daily featured title with a home card and an optional reminder
  notification; the reminder respects the notification master switch and the OS permission, skips
  titles you have already watched, and opens the right screen when tapped.
- **First-party analytics** - Privacy-safe, no-PII usage events (opens, playback, shares, Movie of
  the Day interactions) to the app's own Cloudflare site + D1, with an opt-out-by-default design
  (disabled without `VISTA_ANALYTICS_KEY`). See `DEPLOYMENT.md`.
- **Sharing** - Share any title from its details screen. One tap to WhatsApp, or the native share
  sheet for Facebook, TikTok and anything else; falls back to the sheet when WhatsApp is absent.
- **Onboarding** - Cinematic first-run introduction (brand tour + personal name) and a
  welcome-back screen for returning viewers; a one-time Premium launch announcement with a live
  countdown.
- **TV** - Voice-first search on Android TV / Google TV.
- **Offline-first** - Room (watchlist, history, recent searches), DataStore (settings).

## Tech Stack

| Layer        | Technology                                                        |
|--------------|-------------------------------------------------------------------|
| Language     | Kotlin, Jetpack Compose (incl. TV Material)                       |
| Architecture | MVVM, Repository pattern, single-activity navigation (Compose)    |
| DI           | Hilt                                                              |
| Data         | Retrofit + OkHttp, Room, DataStore, Gson                          |
| Media        | Media3 / ExoPlayer, Coil                                          |
| Backend      | Firebase (Analytics, Crashlytics, Cloud Messaging), AdMob, first-party analytics (Cloudflare Pages Functions + D1) |
| Content APIs | TMDB, TVMaze, Watchmode, Shoofaflam, IMDb helper - see `ENDPOINT_INTEGRATION.md` |

## Project Layout

```
Vista/
├── app/
│   ├── build.gradle.kts
│   ├── schemas/                     - Room schema exports
│   └── src/main/java/com/example/vista/
│       ├── data/
│       │   ├── local/               - Room + DataStore
│       │   ├── remote/              - API clients (tmdb, tvmaze, watchmode, shoofaflam, imdb, playback)
│       │   └── repository/          - Repository implementations
│       ├── di/                      - Hilt modules
│       ├── domain/model/            - Domain models
│       ├── navigation/              - Nav graph + bottom tabs
│       ├── ui/                      - Compose screens (home, discover, details, player, search, saved, history, settings, splash)
│       └── util/                    - AppUpdateManager, ViewUtils
├── deploy/                          - Web deployment package (see deploy/README.md)
├── DEPLOYMENT.md                    - Release & deploy runbook
├── BUILD_REPORT.md                  - Build status report
├── ENDPOINT_INTEGRATION.md          - API integration guide
└── CHANGELOG.md
```

## Prerequisites

- JDK 17
- Android SDK (compileSdk 35)
- A google-services.json for your Firebase project at `app/google-services.json`

### API keys

Copy `gradle.properties.example` to `gradle.properties` and fill in the keys (never commit
`gradle.properties` - it is git-ignored):

```properties
TMDB_API_KEY=...
TMDB_ACCESS_TOKEN=...
WATCHMODE_API_KEY=...
```

These content API keys are compiled into the app via `BuildConfig`. **The NVIDIA API key is not.**
Store it in the project root `.env` as `NVIDIA_API_KEY` (or `NVIDIA_NIM_API_KEY`); the backend deploy
script uploads it as a Cloudflare Worker secret. Android uses `VISTA_AI_BASE_URL`, a public HTTPS
endpoint, with Firebase authentication. See `backend/README.md` for model probing, deployment and
the live smoke test.

### Release signing

Releases are signed with `release-key.jks` configured through `keystore.properties`
(git-ignored). See `DEPLOYMENT.md` - the keystore and its passwords must be backed up; losing
them makes it impossible to update installed apps.

## Building

```powershell
# Debug APK (no minification)
.\gradlew.bat :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Signed release APK (minification currently off - see BUILD_REPORT.md)
.\gradlew.bat --no-daemon :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Install on a connected device:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

> **Heads-up:** release minification is **disabled** because an earlier R8 run stripped the Gson
> DTO fields under `data.remote.**` and the catalog broke (no movies). Keep rules now exist in
> `proguard-rules.pro` (`-keep class com.example.vista.data.remote.**`), so R8 can be re-enabled
> later after a device smoke test if a smaller APK is desired.

## Verifying a build

```powershell
# Signing + version of a build
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt.exe" dump badging app\build\outputs\apk\release\app-release.apk

# Integrity checksum
(Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256).Hash
```

## Documentation

- `DEPLOYMENT.md` - step-by-step release and deployment runbook
- `backend/README.md` - NVIDIA/Cloudflare setup, model verification, conversation contract and voice behavior
- `MARKETING.md` - campaign copy, channel strategy, and the referral launch runbook
- `deploy/README.md` - what ships in the deploy package & quick start for hosting it
- `ENDPOINT_INTEGRATION.md` - all content/streaming APIs wired into the app
- `BUILD_REPORT.md` - current build status report
- `CHANGELOG.md` - release history

## Notes

- APKs are distributed outside the Play Store; Android shows a security warning and users need
  to allow "Install from Unknown Sources". HTTPS is required for APK downloads.
- The in-app updater polls `vista-version.json` on the deploy site - see `DEPLOYMENT.md`.
