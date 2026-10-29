# Vista Cinema - Build Report

## Current verification: 1.0.9 signed release and debug builds successful

Verified **October 29, 2026**. VersionName **1.0.9**, versionCode **16**.

- **Command:** `.\gradlew.bat --no-daemon --no-build-cache :app:clean :app:assembleDebug --console=plain`
- **Result:** `BUILD SUCCESSFUL in 7m 38s`, 45 tasks executed.
- **Artifact:** `app/build/outputs/apk/debug/app-debug.apk`.
- **Build recovery:** stopped stale Gradle/Kotlin daemons and avoided concurrent IDE/terminal builds. Local settings use a 1536 MB Gradle heap, one worker, no parallel builds, in-process Kotlin, and disabled Kotlin/KSP incremental compilation. See `gradle.properties.example`.
- **Remaining output:** deprecation/coroutine opt-in warnings and two native libraries packaged without symbol stripping; these did not fail the build.
- **Backend:** 9 tests passed. Authenticated live NVIDIA checks passed for page routing, profile/app grounding and follow-up memory with fallback rejected.
- **Signed release:** `:app:assembleRelease` passed, including release lint. Artifact: `app/build/outputs/apk/release/app-release.apk`, 22,369,333 bytes.
- **Release SHA-256:** `041131BC0350B193B2FF8327CC95289FED0AAF73D10BB79A22C09BE09221DB05`.
- **Signing certificate SHA-256:** `a4fc4156be6fc87480c04a7defc5e811ba4006df4ed180345c429cd4d0a85318`; matches the GitHub 1.0.8 APK. `apksigner` and `aapt` verified signing, package, version and both launchers.
- **Publication package:** phone and TV APKs belong in GitHub; the 1.0.9 manifest is mandatory. Cloudflare Pages receives no APKs. Verify the public manifest after deployment.
- **Device testing:** microphone permissions, voice quality/turn-taking, floating chat and visual behavior still require physical-device verification.

## Historical signed release verification: 1.0.6

| | |
|---|---|
| **Date** | September 28, 2026 |
| **Build Type** | Signed release APK (minification disabled, see note) |
| **Version** | 1.0.6 (versionCode **13**) |
| **APK** | `app/build/outputs/apk/release/app-release.apk` |
| **Size** | ~21.0 MB (22,024,645 bytes) |
| **SHA-256** | `0C03D7EF829A94E053FB237B632C86279C6FFD0522AA80EE586D3A1A815D7523` |
| **Signing** | Release keystore `release-key.jks` (CN=Vista Cinema) |

Verified with `apksigner verify --print-certs` and `aapt dump badging`:
package `com.example.vista`, `versionCode='13'`, `versionName='1.0.6'`, `sdkVersion='24'`,
`targetSdkVersion='35'`. Both launchers present: `MainActivity` (phone) and
`TvMainActivity` (leanback), and the `POST_NOTIFICATIONS` runtime permission is declared.
Signature verified with `apksigner verify`: v2 scheme passes.

## Minification note (important)

The first 1.0.4 release build had R8 minification + resource shrinking enabled, producing a
7.3 MB APK. **That build did NOT show movies/titles.** Cause: R8 stripped/renamed the Gson DTO
models under `data.remote.**` (they were missing from the ProGuard keep rules), so Gson's
reflective JSON parsing returned all-null objects and the catalog came back empty.

Fix applied since the shipping 1.0.4 build (still in force for 1.0.6):
1. `isMinifyEnabled = false` and `isShrinkResources = false` in `app/build.gradle.kts`
   (release) - the shipped APK is now ~21.0 MB and behaves like the known-good debug builds.
2. Keep rules added to `app/proguard-rules.pro` (`-keep ... data.remote.**`).
   Minification can be re-enabled after a device smoke test if the smaller APK is wanted.

## What This Release Contains (1.0.6)

### Fixed
- **Share links pointed at the wrong website.** `ViewUtils.shareMedia` was building
  `shoofaflam.tv/$type/$id`, so every movie and series a viewer shared from a details screen sent
  the recipient to a third-party site instead of Vista Cinema. Now shares
  `https://www.lxobsidianportal.co.za` (`util/ViewUtils.kt`).
- `VistaMessagingService.onNewToken` re-subscribed only to the global topic. A refreshed FCM token
  drops *every* topic subscription, so market-targeted pushes would have silently stopped after the
  first token rotation. It now re-subscribes to both.

### New
- **Share to WhatsApp.** One tap opens `wa.me` with the message pre-written and the contact picker
  up, which is the only form that works when the sender's number is unknown. Falls back to the
  native share sheet when WhatsApp is not installed, so the share is never a dead end.
- **Country-resolved messaging.** `util/CountryResolver.kt` resolves the viewer's ISO 3166-1
  alpha-2 country (first configured locale carrying a real region, falling back to the SIM's
  network country) and maps it to `vista_updates_za` or `vista_updates_row`. The result is
  captured once on first launch and frozen by `PreferencesManager.ensureCountryCode()`.
- **Country-aware referral landing page** at `deploy/vista/index.html`. Client-side country
  detection mirrors `CountryResolver` so the page and the app bucket viewers identically, and only
  South African visitors are shown the R20 Premium price, since that is the only market it is
  priced for.
- **First-party analytics** (`data/analytics/VistaAnalytics.kt`). Privacy-safe, no-PII event
  collection to the app's own site: `app_open`, `playback_started`, `playback_completed`,
  `share_whatsapp/x/facebook/sheet`, `motd_card_shown`, `motd_opened`, `motd_dismissed` and
  `motd_reminder_delivered`. Events are batched in memory (~every 400ms) and POSTed to
  `/api/collect` on Cloudflare with a shared secret (`VISTA_ANALYTICS_KEY`); the endpoint is
  allowlisted server-side and disabled entirely when the key is blank. Aggregates (which covers
  from the free-trial Mandatory update onwards) are readable at `/api/vista-stats` with a separate
  `VISTA_STATS_KEY`. D1 migration: `migrations/0001_analytics.sql`. See `DEPLOYMENT.md`.

### Notes
- Market topics are **additive**, never a replacement. Installs stay subscribed to
  `vista_updates`, so 1.0.5 devices that have not updated still receive global broadcasts. No
  message is ever sent to both a global and a market topic for the same send.
- `mandatory: true` for this release so the base moves onto the country topics — a targeted send
  reaches nobody until the install carrying it is on 1.0.6+.
- `deploy/vista/index.html` is **staged but not published.** It advertises a 12-month Premium
  reward that no build can yet track or honour; publishing it now would collect signups against a
  promise the product cannot keep. It ships with the Phase 1 referral backend.

## What Previous Release Contained (1.0.5)

### New
- **Release notifications (FCM).** On-device push via the `vista_updates` topic;
  `VistaMessagingService` + `util/AnnouncementsStore.kt` keep an on-device announcement inbox.
  Tapping a release notification forces an immediate in-app update check. One-shot
  `POST_NOTIFICATIONS` request on first launch (API 33+).
- **Notification bell on Home.** Bell icon with an unread dot opens a bottom sheet with the
  announcement backlog and a pinned "Premium is coming" card; `Settings > Notifications`
  deep-links from the sheet.
- **Cinematic onboarding.** First-run viewers get a three-act brand tour (logo animation,
  "Why Vista", personal-name step); returning viewers who have been away 30+ days get a single
  welcome-back screen. The name feeds the Home greeting.
- **Premium announcement.** One-time prompt with a live countdown to the early-2027 launch
  (R20/month); acknowledging hides it forever.
- **3-day guest trial.** `guest_trial_started_at` records a single non-renewable window;
  expired trials return to sign-in with a clear message.
- **TV search.** Voice-first search on the TV build (`TvSearchScreen`) with `RecognizerIntent`,
  recent/trending chips and a poster grid.

### Redesigned
- **Settings** rewritten: grouped section cards, profile banner, real storage/cache sizes,
  honest notification toggles, subtitle size slider + colour swatches with a live preview,
  merged Data & Storage page, privacy/history controls, About page. Deep links open a specific
  page (`settings?page=...`).
- **Profile** rows deep-link into the matching Settings page; the fake Downloads row is gone.

### Fixed
- TV: mandatory update dialogs could be dismissed with the BACK key — now non-dismissible.
- `HomeViewModel` removed a dead alias-fetching block; identity now sources from the
  display-name preference.

## Earlier releases

This report is kept two releases deep. Full history for 1.0.4 and earlier — including the
Playwright-free fix list, the 50 MB HTTP cache work and the Room `1->2` migration — lives in
`CHANGELOG.md`.

## Architecture (current)

```
UI (Jetpack Compose) -> ViewModel -> Repository -> Remote (Retrofit) / Local (Room + DataStore)
```

- **Single-activity** navigation with five bottom tabs: Home, Discover, AI Assistant, Watch List and Profile. Search is reached from Home or assistant actions; the Home FAB opens floating chat.
- **Hilt** DI; **Room** for watchlist/history/recents; **DataStore** for settings.
- **Media3/ExoPlayer** player with multi-provider embed fallbacks.
- Content sources: TMDB, TVMaze, Watchmode, Shoofaflam + IMDb helper (see
  `ENDPOINT_INTEGRATION.md`).
- Firebase Analytics + Crashlytics + **Cloud Messaging**, AdMob in the player.
- First-party analytics via Cloudflare Pages Functions + D1 (no Firebase dependency).
- Phone + TV share one signed APK via dual launchers.

See `README.md` for the module layout and build commands, `DEPLOYMENT.md` for shipping,
`MARKETING.md` for campaign copy, and `CHANGELOG.md` for release history.

---

**Current result: 1.0.9 signed release and debug builds successful; release artifacts and mandatory manifest prepared for publication.**
