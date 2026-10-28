# Vista Cinema 1.0.9 Release

## Release Summary
- **Version:** 1.0.9 (versionCode: 16, versionName: "1.0.9")
- **Release Date:** 2026-10-12
- **Build Output:** `app/build/outputs/apk/release/app-release.apk`
- **Single Shipped APK serves both phone and Android TV / Google TV variants** (same binary).

## What's New in 1.0.9
- **Vista AI Assistant & Voice Integration:**
  - Chat or talk directly with Vista Assistant via Speech-to-Text and Text-to-Speech engines.
  - Personalized recommendation engine ranking catalog titles based on genre preferences, recent watch history, and mood/duration prompts.
  - Spoiler-aware movie & series companion accessible from Details screen and Player controls overlay.
- **Smart Cache & Playback-Recovery System:**
  - Automatic video verification error resolution via ExoPlayer Media3 `onPlayerError`.
  - Active playback session protection so active Media3 streams are never deleted during playback.
  - Background `WorkManager` cache maintenance running every 24 hours.
  - User-configurable cache size limit preference in `Settings > Data & Storage` (250 MB to 2000 MB).
  - Manual "Repair playback & clear player cache" action in Settings and Player error screen.
- **Clean Icon-Only Bottom Navigation Bar:**
  - Removed bottom tab labels for an uncluttered streaming experience with active red indicator pills.
- **Redesigned Settings & Profile Experience:**
  - Modern profile banner with gradient styling, app update checker, build details, and data attribution.

## Agent Instructions: Upload this release to live site

### 1. Upload the Signed Release APK
- Source File: `app/build/outputs/apk/release/app-release.apk`
- Live Target: `https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/VistaCinema-v1.0.9.apk`
- MIME type: `application/vnd.android.package-archive`

### 2. Host / Update the manifest
- Manifest File: `deploy/vista-version.json`
- Live Endpoint: `https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/deploy/vista-version.json`
- JSON content:
  ```json
  {
    "versionCode": 16,
    "versionName": "1.0.9",
    "downloadUrl": "https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/VistaCinema-v1.0.9.apk",
    "changelog": "v1.0.9 Release: All stream & video verification errors resolved with Smart Cache Recovery. Introduced Vista AI Assistant (talk or chat for personalized recommendations & spoiler guides), clean icon-only bottom navigation bar, and enhanced Settings UI.",
    "mandatory": false
  }
  ```
- MIME type: `application/json`

### 3. Broadcast FCM Announcement
- Run the push notification script using node:
  ```bash
  node send_fcm_cli.mjs --send
  ```
- Message Payload:
  - **Title:** `Vista Cinema v1.0.9 is Live! 🚀`
  - **Body:** `Stream errors fixed with Smart Cache Recovery! Meet your new AI Assistant — talk or chat directly with Vista Cinema for personalized recommendations, cast details & spoiler-free guides. Tap to update!`
  - **Topic:** `vista_updates`

### 4. Verify Over-the-Air (OTA) In-App Update
- Launch the app on an install carrying an earlier build (v1.0.8 or earlier).
- The app checks `deploy/vista-version.json` upon launch / resume and surfaces the "New version is here!" dialog.
- Tapping "Install Update Now" downloads `VistaCinema-v1.0.9.apk` and launches Android's package installer.
