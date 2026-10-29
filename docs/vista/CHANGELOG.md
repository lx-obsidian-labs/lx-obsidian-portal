# Changelog

All notable changes to Vista Cinema.

## [1.0.9] - 2026-10-29

VersionCode **16**. Signed release APK successfully built at `app/build/outputs/apk/release/app-release.apk`.
Phone and TV use the same binary, hosted in the GitHub repository. The release manifest sets `mandatory: true`.
Cloudflare Pages serves the website, documentation and manifest, not APKs. The release signing certificate
matches the published 1.0.8 APK. Size: 22,369,333 bytes; SHA-256: `041131BC0350B193B2FF8327CC95289FED0AAF73D10BB79A22C09BE09221DB05`.

### Latest AI and UI changes
- Replaced classifier-only canned chat with generated NVIDIA NIM replies using bounded conversation history and catalog grounding.
- Verified and deployed `nvidia/nemotron-3.5-lightning-30b-a3b` behind Firebase-authenticated Cloudflare; retained Workers AI fallback.
- Fixed inherited environment credentials overriding the project's `.env` key; the provider key stays in a Worker secret.
- Added profile-alias and enabled taste-preference context, plus a maintained reference to Vista's supported features.
- Added allowlisted app-page actions and navigation to Search with a populated query.
- Changed the Home FAB to floating chat while preserving the bottom-tab full-screen assistant.
- Added foreground hands-free voice turns, spoken stop commands, high-quality Android voice selection and voice settings access.
- Added Settings AI/chat controls and Profile assistant access and kids-version planning information.
- Sent the date-free 1.0.9 coming-soon/kids-version-planned FCM announcement using Firebase CLI authentication; FCM accepted it for delivery.

### Verification and build recovery
- Nine backend tests passed; live page routing, app/profile knowledge and follow-up memory passed with NVIDIA explicitly required.
- Clean `:app:assembleDebug` passed with 45 tasks after stopping stale daemons and using one worker, in-process Kotlin and disabled incremental Kotlin/KSP compilation.
- Voice realism, microphone behavior and visual polish remain physical-device checks.

### Added
- **Vista AI Assistant & Voice Integration.**
  - **Natural Language & Voice Interaction:** Chat or speak directly with Vista Assistant using Speech-to-Text and Text-to-Speech engines.
  - **Personalized Discoveries & Taste Engine:** Real-time recommendation ranking powered by genre preferences, watch history signals, and mood/keyword prompts (*"relaxing"*, *"thriller"*, *"short comedy"*).
  - **Spoiler-Aware Companion:** Dedicated movie/series companion screen (`Screen.AiAssistant.companion`) accessible from Details and Player overlays with verified catalog metadata and customizable plot spoiler safeguards.
- **Smart Cache & Playback-Recovery System.**
  - **Automatic Verification Recovery:** Resolves video stream & verification errors via `player.listener.onPlayerError`. Classifies failures (expired local data, network timeouts, authorization expiration, server failures), clears corrupt temporary segments, and performs controlled retries.
  - **Active Session Protection:** Active Media3 playback sessions are registered and protected so temporary files are never wiped during playback.
  - **Background WorkManager Maintenance:** Periodically purges stale files (>24h) and enforces user-configured cache size limits.
  - **Manual Repair Action:** "Repair playback & clear player cache" button in Settings and Player error screen.
- **Clean Icon-Only Bottom Navigation Bar.**
  - Replaced bottom navigation bar text labels with clean tab icons and active indicator pills.
- **Redesigned Settings & Profile Experience.**
  - Enhanced profile card, customizable cache limit selector (250 MB to 2000 MB), live in-app update checker, and attribution sections.

## [1.0.8] - 2026-10-08

Release APK: `deploy/VistaCinema-v1.0.8.apk` (same APK serves phone + TV). Deployed with the
in-app update manifest pointing at `versionCode 15`.

### Fixed
- Restored catalog functionality after R8/ProGuard hotfix. ProGuard keep rules updated for reflective DTO parsing.

## [1.0.7] - 2026-10-05

Release APK: `deploy/VistaCinema-v1.0.7.apk` (same APK serves phone + TV). Deployed with the
in-app update manifest pointing at `versionCode 14`.

### Added
- **Your Stats.** A new Stats screen (Profile > Your Stats) with lifetime figures computed from
  local watch history: time reached, titles finished, titles in progress, films vs series split,
  current streak, last-7-days activity, and the longest title reached. Reachable from the Profile
  tab in the bottom navigation.
- **Bottom navigation now has Search, Profile and Settings.** Search sits next to Discover so it is
  one tap away; Profile and Settings moved out of being buried one level deep. Six tabs, ordered for
  one-handed reach with Settings last as the least-used destination.
- **Milestone sharing.** After a real achievement - a 3/7/14/30/100 day streak, finishing 5/10/25/
  50/100 titles, or 3+ titles in a week - the app offers a pre-written invitation to a friend on a
  dedicated screen with the message pre-filled and editable. The notification opens that screen
  directly, so there is no share button to hunt for.
  - **Strictly milestone-only.** Never on a timer, never as a follow-up, and each milestone fires
    exactly once.
  - **Segmented to power users.** Only viewers who have opened the app 3+ times, started 3+ titles
    and finished at least one. New and casual viewers are never asked.
  - **Capped at twice a month**, with a 14-day cooldown, and a "Share your wins" opt-out in
    Settings > Notifications on its own channel so it can be silenced independently of announcements.
  - Copy states only what this device recorded. There is no leaderboard and no fabricated ranking.
- **Library resume button.** Resumeable titles in Library now carry a play button over the poster,
  so continuing something is one tap instead of opening the title first.

### Fixed
- **Library merge keys collided.** Saved rows were keyed lowercase while history rows were keyed
  uppercase, so a title in both appeared twice and could crash the grid on duplicate keys. Both now
  use one `type:id` form (`ui/library/LibraryViewModel.kt`).
- **Settings showed a back arrow on its own root.** As a bottom tab there is nothing behind it to
  go back to, and the arrow popped the viewer off a root tab. The arrow now only appears on
  Settings sub-pages (`ui/settings/SettingsScreen.kt`).
- **Continue Watching required real progress.** Entries with zero duration or under 1% progress no
  longer appear, so the row cannot be filled with titles that were never actually started.

## [1.0.6] - 2026-09-28

Release APK: `deploy/VistaCinema-v1.0.6.apk` (same APK serves phone + TV). Deployed with the
in-app update manifest pointing at `versionCode 13` (mandatory: true — the country topics only take
effect once the base has moved onto this build).

### Fixed
- **Share links pointed at the wrong website.** `ViewUtils.shareMedia` was building
  `shoofaflam.tv/$type/$id` — every movie and series a viewer shared from the app sent the
  recipient to a third-party site instead of Vista Cinema. Now shares
  `https://www.lxobsidianportal.co.za` (`util/ViewUtils.kt`).
- Share copy no longer reads as brand marketing. It is written as a recommendation from the person
  sharing, which converts several times better than an announcement.

### Added
- **Share to WhatsApp.** One tap opens `wa.me` with the message pre-written and the contact picker
  up, so it works when the sender's number is unknown. Falls back to the native share sheet when
  WhatsApp is not installed, so the share is never a dead end.
- **Country-resolved messaging.** Installs now capture their country once on first launch
  (`util/CountryResolver.kt`, persisted via `PreferencesManager.ensureCountryCode()`) and subscribe
  to a market topic — `vista_updates_za` or `vista_updates_row` — in addition to the global
  `vista_updates` topic. Announcements can be aimed at one market.
- **First-party analytics** (`data/analytics/VistaAnalytics.kt`). No-PII event collection to the
  app's own Cloudflare site (`/api/collect` + D1) covering app opens, playback start/finish,
  shares, and Movie-of-the-Day card/notification events. Batched in memory, allowlisted
  server-side, gated by a shared secret, and nested-safe: it is entirely disabled unless the build
  carries `VISTA_ANALYTICS_KEY`. Aggregates readable via `/api/vista-stats`. See
  `DEPLOYMENT.md`.
- `deploy/vista/index.html` — country-aware referral landing page. **On hold; not published until
  the Phase 1 referral backend can honour the reward it advertises.**

### Fixed
- **Movie of the Day reminder respected the master switch.** Turning
  `Settings > Notifications` off still scheduled reminders. The worker now honours
  `movieOfTheDayEnabled`, the global notification master switch, and the OS
  `POST_NOTIFICATIONS` permission before notifying (`motd/MovieOfTheDayWorker.kt`).
- **Movie of the Day reminders suggested already-watched titles.** The worker now consults the
  watchlist history and skips any movie the viewer has already finished.
- **Tapping a Movie of the Day notification landed on the wrong screen.** The worker now launches
  MainActivity with an explicit "open Home" intent (`EXTRA_OPEN_HOME`), which the main app observes
  and honours after auth, instead of racing splash/auth screens.
- **Movie of the Day toggle could be silently ignored.** `SettingsViewModel.updateMovieOfTheDay`
  now persists the preference instead of only flipping the in-memory state.

### Notes
- Market topics are *additive*. Installs stay on `vista_updates`, so 1.0.5 devices that have not
  updated still receive global broadcasts, and no message is ever sent to both a global and a
  market topic for the same send.
- `VistaMessagingService.onNewToken` now re-subscribes to both topics: a refreshed token drops every
  prior topic subscription, so re-subscribing to only the global topic would have silently stopped
  market-targeted pushes after the first token rotation.

## [1.0.5] - 2026-09-23

Release APK: `deploy/VistaCinema-v1.0.5.apk` (same APK serves phone + TV). Deployed with the
in-app update manifest pointing at `versionCode 12` (mandatory: true — every install must move
onto this build before announcements can reach it).

### Added
- **Release notifications (FCM).** Vista Cinema subscribes to the `vista_updates` topic and can
  now push announcements straight to every running install (`data/remote/firebase/` +
  `util/AnnouncementsStore.kt`). Tapping a release notification forces an immediate in-app
  update check. Notification permission is requested once at first launch (API 33+).
- **Notification bell on Home.** A bell icon (with an unread dot) opens a bottom sheet with the
  announcement backlog plus a pinned "Premium is coming" card. Announcements are stored on-device
  so nothing is missed. `Settings > Notifications` deep-links directly from the sheet.
- **Cinematic onboarding.** First-run viewers get a three-act brand introduction (logo animation,
  "Why Vista", then a personal-name step). Returning viewers who have been away 30+ days get a
  single welcome-back screen. The name feeds the Home greeting.
- **Premium announcement.** A one-time prompt with a live countdown to the early-2027 Premium
  launch (R20/month), plus a compact card in the bell sheet. Acknowledging it hides it forever.
- **3-day guest trial.** Guests now start a single, non-renewable 3-day window
  (`guest_trial_started_at`); expired trials are sent back to sign-in with a clear message.
- **TV search.** Voice-first search on the TV build (`TvSearchScreen`): microphone via
  `RecognizerIntent`, recent/trending chips, poster grid, and a type-in field for remote keyboards.
- Home now shows a time-based greeting and real storage/cache sizes in Settings.

### Redesigned
- **Settings.** Rewritten with a professional layout: grouped section cards, a profile banner,
  real storage and cache figures, honest notification toggles (master gate + per-category),
  subtitle size slider and colour swatches with a live preview, merged Data & Storage page,
  theme choices, privacy/history controls, and a complete About page. Deep links open a specific
  page (`settings?page=NOTIFICATIONS`, etc.).
- **Profile.** Preference rows now deep-link straight into the matching Settings page; the fake
  Downloads row is gone (merged into Data & Storage).

### Fixed
- TV: a **mandatory** update could previously be dismissed with the BACK key, stranding the
  install on an old version indefinitely — mandatory dialogs are now non-dismissible.
- Guest greeting could not resolve a viewer alias; the greeting now uses the saved display name.

### Performance / internal
- `HomeViewModel` cleaned of a dead alias-fetching block; identity flows now source from the
  display-name preference only.
- `SettingsViewModel` removed hardcoded cache/storage estimates in favour of real directory sizes.

## [1.0.4] - 2026-09-22

Release APK: `deploy/VistaCinema-v1.0.4.apk` (same APK serves phone + TV). Deployed with the
in-app update manifest pointing at `versionCode 11` (mandatory: false).

### Fixed
- Series sometimes opened the wrong, unrelated movie - now routed to the correct show.
- "See All" pages and load-more were empty on most rows.
- Player back-navigation returned to the wrong screen from autoplay, "Play Now" and the center
  controls.
- Playback progress is now persisted on exit, so Continue Watching and Watch History resume
  where you left off; seek actions no longer spam the database.
- Streaming sources are more reliable and source switching is instant.
- Deep links and search navigation no longer mangle query URLs (`+` vs `%20`).
- Upgrading a 1.0.x install no longer crashes the Room database (missing migration added).
- Rapid watchlist taps can no longer double-fire the toggle.
- Leaving Discover via the bottom bar resets its tab state correctly.

### Added
- "Because You Watched" recommendation row on Home.
- "More Like This" suggestions on series details.
- Double-tap any poster to save / remove it from the watchlist (with a saved badge).
- Double-tap inside the player to seek (interval configurable in Settings).
- "Play Something" shuffle button on Home rows.
- Watch History search with a "no matches" empty state.
- "Check for Updates" action in Settings - About.
- Update polling throttled to one check per 30 seconds.

### Performance
- 50 MB HTTP response cache for all API clients.
- Larger Coil disk cache + 1024 px image request cap.
- TVMaze and TMDB lookup results cached in memory.

## [1.0.0] - [1.0.2]

Earlier releases (archived under `deploy/archive/`). Introduced the Netflix-style shell,
browse/discover/search/saved screens, Media3 player, watch history, guest mode, ad blocker,
brightness control and people search. Phone-number sign-in removed.

See `deploy/vista-version.json` for the change notes shipped to users with each manifest
update.
