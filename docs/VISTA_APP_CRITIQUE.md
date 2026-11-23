# Vista Watch app critique and improvement plan

Scope: Android app (`app/src/main`), FCM tooling, Cloudflare backend, release/update path, docs and CI. This is a research/review document. No source files were changed as part of this audit.

Severity: **High** = data/security/trust/release blocker; **Med** = visible reliability or growth risk; **Low** = polish/maintainability.

## 1. Executive verdict

Vista Watch has unusually strong structure for a small app: clean separation of view models, repositories and policy objects; explicit recovery policies; allowlisted playback origins; a bounded announcement inbox; leadered FCM recruitment tooling; and unusually careful docs about what is and is not verified.

The main risks are concentrated in four places:

1. **Update integrity** — sideloaded APKs are accepted and installed without hash/size/signature verification at the device, and the public verifier compares two downloads against one digest (`app/src/main/java/com/example/vista/util/AppUpdateManager.kt:112-137`, `app/src/main/java/com/example/vista/util/AppUpdateManager.kt:147-196`, `scripts/verify_public_release.mjs:13-25`).
2. **Secrets and credentials** — plaintext API keys are committed in `gradle.properties:51-53`, and a tracked `keystore.properties` plus tracked signing-script examples widen the blast radius. Existing debug logging redacts headers, but the source-of-truth config does not.
3. **Notification categorisation** — release announcements are sent with `category: 'promotions'` (`send_fcm_cli.mjs:38`), while the app treats only `category == 'updates'` as essential (`app/src/main/java/com/example/vista/data/remote/firebase/VistaMessagingService.kt:61-68`). Users who disable release promotions silently miss critical update notices. LX Insider recruitment uses the same category, so its reach is capped by another toggle.
4. **Operational drift and unverified device claims** — public docs mix current 1.0.11/1.0.12 facts with older 1.0.9/1.0.10 narrative, iOS artifacts are unsigned, and no ADB device was attached for the claimed TV/voice/offline behaviour (`README.md:8`, `docs/PUBLIC_RELEASE_1_0_11.md:11`, `docs/PUBLIC_RELEASE_1_0_11.md:51`, `BUILD_REPORT.md:19`).

## 2. Feature-by-feature critique

### 2.1 Onboarding, auth and profile

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Guest mode is entirely local and described as a "non-renewable" 3-day trial, but clearing app data or reinstalling restarts it. | `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:90`, `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:120-131`, `app/src/main/java/com/example/vista/ui/auth/AuthViewModel.kt:88-99` | Cosmetic enforcement; anyone motivated bypasses the limit. It is also misleading if marketed as a real entitlement boundary. | Med | Either rename it honestly to "this install's guest session" or move entitlement state server-side with a pseudonymous install ID and documented abuse policy. Do not pretend local state is secure. |
| Password reset logs the raw email into an analytics event. | `app/src/main/java/com/example/vista/data/remote/firebase/FirebaseManager.kt:60-64` | Email addresses can leak into analytics tooling; contradicts "no-PII" claims elsewhere. | High | Remove the `email` parameter. Log only `password_reset_requested` with method/result. |
| Profile writes send a `lastSeenAt` update before reading the profile. | `app/src/main/java/com/example/vista/data/remote/firebase/FirebaseManager.kt:109-117` | Read path is not side-effect free; Firestore rules, offline queues and rate limits now affect profile lookup. | Med | Move `lastSeenAt` to a dedicated best-effort write in the lifecycle owner; do not gate navigation on it. |
| Sign-out clears Firebase analytics user ID but not the local guest/FCM topic state. | `app/src/main/java/com/example/vista/data/remote/firebase/FirebaseManager.kt:92-95`, `app/src/main/java/com/example/vista/data/remote/firebase/VistaMessagingService.kt:47-54` | Topic subscriptions and promotions can keep following an account that no longer exists on device; cross-account notification bleed risk. | Med | On sign-out, unsubscribe market topics or re-evaluate topics from the new account; clear owner-scoped caches and FCM token association. |
| Auth recovery is good, but `retryLast()` can resubmit a sign-up with an in-memory password after process restoration is not guaranteed. | `app/src/main/java/com/example/vista/ui/auth/AuthViewModel.kt:41-43`, `app/src/main/java/com/example/vista/ui/auth/AuthViewModel.kt:148-156` | Edge-case retry failure; not a security bug, but a support ticket generator. | Low | On retry after process death, route to the form with email prefilled and ask for the password again. |

### 2.2 Kids mode, parental controls and content policy

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Parental PIN is unsalted SHA-256, trimmed, then compared as hex strings. | `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:332-344` | Four-digit PINs are brute-forceable instantly if DataStore is extracted; `MessageDigest.isEqual` on hex strings is also less clean than comparing digests. | High | Store a per-install random salt plus PBKDF2/scrypt/Argon2id hash (or useAndroid Keystore-backed verification). Compare raw bytes with `MessageDigest.isEqual`. |
| Kids mode is a local preference with no re-auth on exit. | `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:313-348` | A child can switch modes if they can reach Settings; parental control is cosmetic. | High | Gate exit behind the PIN, require re-auth for adult content changes, and document that this is device-level supervision, not remote parental control. |
| Adult-content flag is derived from age band + kids mode + explicit toggle, but there is no server-side age assurance. | `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:348-376`, `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:454` | Self-declared age is weak for R18 content; store policy and ad partners still see traffic. | Med | Keep self-declaration but add clear disclosures, brand-safety allowlists, report/block controls and a no-adult-content QA gate. Get legal review before any Kids pilot (`docs/premium-2027/08-OPERATIONS.md:52`). |

### 2.3 Home, catalog, details and discovery

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Home catalog failures are broadly swallowed into empty lists. | `app/src/main/java/com/example/vista/data/repository/HomeCatalogLoader.kt:45-46`, `app/src/main/java/com/example/vista/data/repository/HomeCatalogLoader.kt:88-89` | Distinguishable failures (auth, upstream 5xx, cache corruption, timeout) all look like "nothing here". Recovery copy cannot be specific. | Med | Return a typed `CatalogLoadException` already present in the codebase (`data/repository/CatalogLoadException.kt`) and render one of three states: cached, retry, upstream degraded. |
| Metadata cache interceptor plus a 50 MB shared OkHttp cache applies to all APIs, including unrelated hosts. | `app/src/main/java/com/example/vista/di/NetworkModule.kt:29-35`, `app/src/main/java/com/example/vista/di/NetworkModule.kt:85-102` | One noisy provider can evict TMDB/TVmaze metadata; cache-key collisions across hosts are possible if paths match. | Med | Partition caches per API family or add host to the cache key; cap per-host cache sizes. |
| Details pages try many credited calls in broad try/catch blocks, silently dropping credits. | `app/src/main/java/com/example/vista/data/repository/TmdbCatalogRepository.kt:262-282`, `app/src/main/java/com/example/vista/data/repository/TmdbCatalogRepository.kt:506-513` | Cast/detail sections can be partially empty with no signal. | Low | Emit partial data plus a "details incomplete" state; log structured failure reason to Crashlytics without URLs. |
| Seasonal/MoTD sections are creative but can dominate Home with stale festive content if dismissal state is missed. | `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:63-67`, `app/src/main/java/com/example/vista/motd/MovieOfTheDayWorker.kt:82` | Repeated or out-of-season cards reduce trust. | Low | Add an expiry timestamp to every seasonal card and a server-driven kill switch; assert dismissal in UI tests. |

### 2.4 Search and See All

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Search has debounce and generation cancellation, but every partial query creates a coroutine and calls the API after 300 ms. | `app/src/main/java/com/example/vista/ui/search/SearchViewModel.kt:109-131` | Typical typing bursts still burn 2-4 upstream calls per word; TMDB rate limits become user-visible. | Med | Increase to 350-450 ms, require >= 2 characters for remote search, and add an in-memory LRU keyed by normalized query + tab. |
| Content filters are applied after the API response, so adult/age-filtered results still consume quota and pagination counts. | `app/src/main/java/com/example/vista/ui/search/SearchViewModel.kt:163-188` | `hasMore` can be wrong when most results are filtered out; users see empty pages. | Med | Pass include_adult/filter parameters upstream where supported and compute `hasMore` from filtered results plus provider total. |
| `recentSearches` stores raw query text locally with no retention limit shown in Settings. | `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:52-53`, `app/src/main/java/com/example/vista/ui/search/SearchViewModel.kt:134-141` | Potentially sensitive search history persists indefinitely on-device. | Med | Add a visible "Clear recent searches" action and a 30/90-day expiry; document it in the privacy notice. |

### 2.5 Player and WebView embeds

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Embedded playback runs third-party JavaScript with `mediaPlaybackRequiresUserGesture = false`. | `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:587-607` | Necessary for autoplay, but it also means any malicious provider script can start media/background activity. Combined with full-screen ads, this is a risky trust boundary. | Med | Keep only for allowlisted `StreamType.OTHER` origins (`EmbedOrigins.kt`), add a WebView-only permission deny list and block autoplay on TV unless user initiated. |
| `onCreateWindow` is blocked, but `setSupportMultipleWindows(true)` plus `javaScriptCanOpenWindowsAutomatically = false` is a subtle combination. | `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:575-595` | Future changes can accidentally re-enable pop-up ads. | Low | Set `setSupportMultipleWindows(false)` unless a known provider needs it; add a unit/instrumentation regression test for pop-up blocking. |
| Document-start ad cleanup script is injected into `setOf("*")`. | `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:625-630` | Broad injection can break provider players without warning; rules need versioning. | Med | Scope the script to known embed origins, add a remote kill switch and version the script so a failing rule can be disabled without an APK release. |
| Player forces landscape and keeps screen on for all content. | `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:191-209` | Accessibility and battery cost for short trailers or audio-only playback; orientation is global to the Activity. | Low | Use `setRequestedOrientation` only for full video, restore previous orientation (not UNSPECIFIED), and respect the system "rotation lock" setting. |
| Guest-limit dialog has a "Not now" escape that simply pops back. | `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:70-102`, `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:245-253` | The comment says there is deliberately no "keep watching" escape, but dismissing returns to the previous screen and another title can be opened. If the limit is enforced elsewhere it is fine; if not, it is a policy hole. | Med | Confirm enforcement in `GuestSessionManager`; either remove the dismiss action or make all playback entry points check the same gate. |
| Direct-source and WebView paths have separate lifecycle handling; ExoPlayer is released only in `onDispose`, while WebView is cleaned in `onRelease`. | `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:198-210`, `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:636-645` | Source switching can leak a WebView or player briefly; on low-memory TVs this surfaces as black frames. | Med | Centralize release in a single `DisposableEffect` keyed on `sourceGeneration`; add a leak test on repeated source switches. |

### 2.6 Stream resolution, sources and downloads

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| StreamResolver scrapes HTML with regex patterns and forwards extracted CDN URLs into the player/downloader. | `app/src/main/java/com/example/vista/data/remote/playback/StreamResolver.kt:84-127`, `app/src/main/java/com/example/vista/data/remote/playback/StreamResolver.kt:185-205` | Provider changes silently disable resolution. Extracted URLs are effectively untrusted input; only `https://` and media-like shape are checked. | High | Validate resolved host against an allowlist derived from `EmbedOrigins`, reject credentials/ports, record per-provider resolution health, and add a remote pattern-config channel with signed config. |
| Signed URLs expire, but downloads can be started from resolver output with only an `expiresAt` hint. | `app/src/main/java/com/example/vista/data/remote/playback/StreamResolver.kt:42-45`, `app/src/main/java/com/example/vista/data/remote/playback/StreamResolver.kt:119` | Truncated or failed offline files if the user waits or the network retries late. | Med | Fail fast when `expiresAt - now < 60s`; re-resolve before retry; rename temp file only after byte-count and container validation. |
| `DownloadManager` progress uses `getInt` for byte counters. | `app/src/main/java/com/example/vista/util/AppUpdateManager.kt:206-212` | Same class of bug applies to any large-file progress path; files > 2 GB overflow to negative values. | Med | Use `getLong(COLUMN_BYTES_DOWNLOADED_SO_FAR)` / `COLUMN_TOTAL_SIZE_BYTES`. |
| Download policy allows all sources unless explicitly blocked, and offline player uses a retry loop. | `app/src/main/java/com/example/vista/downloads/DownloadPolicy.kt`, `app/src/main/java/com/example/vista/downloads/OfflinePlayerScreen.kt:51-57` | Users can save content from providers that forbid it; the app cannot prove rights. | High | Default-deny downloads to an explicit provider allowlist, surface provider-specific permission copy, and add a legal/commercial review gate before promoting offline saves. |
| `VidsrcPlaybackRepository`/backup sources are the core playback supply. | `app/src/main/java/com/example/vista/data/remote/playback/VidsrcPlaybackRepository.kt`, `app/src/main/java/com/example/vista/data/remote/playback/BackupMovieSource.kt` | Single-provider outages look like app outages. | Med | Track source success rate by provider, auto-demote failing sources for a cool-down period and show a "source degraded" banner rather than a generic player error. |

### 2.7 FCM, announcements and notification bell

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Only `category == 'updates'` bypasses notification settings. Release scripts send `promotions`. | `app/src/main/java/com/example/vista/data/remote/firebase/VistaMessagingService.kt:61-68`, `send_fcm_cli.mjs:38`, `send_insider_recruitment_fcm.mjs:90-93` | Critical update announcements can be suppressed by a user who only disabled marketing. Recruitment reach is also capped by `notificationReleases`. | High | Use `category: 'updates'` only for genuine security/release notices; add a separate `category: 'campaigns'` with its own opt-in and a send-time eligibility check. Never relabel marketing as updates to bypass the toggle. |
| No server-side or client-side dedupe for announcement inbox entries. | `app/src/main/java/com/example/vista/data/remote/firebase/VistaMessagingService.kt:74-81`, `app/src/main/java/com/example/vista/util/AnnouncementsStore.kt:29-48` | Duplicate deliveries create duplicate bell items; notification ID is derived from `messageId.hashCode()` and can collide. | Med | Deduplicate on `announcement_id` in `AnnouncementsStore`; use a stable numeric hash of the ID with collision check or a persisted ID→notification-id map. |
| Promotions are routed through `ACTION_VIEW` with a safe HTTPS allowlist, but no campaign attribution is passed into the app. | `app/src/main/java/com/example/vista/util/NotificationLinks.kt`, `app/src/main/java/com/example/vista/data/remote/firebase/VistaMessagingService.kt:108-125` | Opens cannot be tied to wave/channel; LX Insider reporting loses attribution. | Med | Append non-sensitive `utm`-style fields to the approved apply URL and record the campaign ID in analytics when the deep link opens. Keep allowlist verification. |
| `onNewToken` subscribes to global synchronously and market topics in a fire-and-forget scope. | `app/src/main/java/com/example/vista/data/remote/firebase/VistaMessagingService.kt:47-55` | If country resolution or subscribe fails, the permanent global topic succeeds but market topic is missing until the next token refresh. | Med | Retry with backoff, persist the desired topic set, and reconcile on app start. |
| `send_fcm_cli.mjs` has an announcement ID/collapse key but no durable sent-ledger guard. | `send_fcm_cli.mjs:17-19`, `send_fcm_cli.mjs:43-49` | Re-running a broadcast can duplicate a release announcement. LX Insider sender is already better (`send_insider_recruitment_fcm.mjs:114-135`, `send_insider_recruitment_fcm.mjs:188-213`). | High | Port the ledger pattern to every send script; require `--send` plus explicit audience/wave; make console operators use one wrapper. |

### 2.8 In-app updates and release delivery

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Version manifest parsing ignores `sha256` and `sizeBytes`. | `app/src/main/java/com/example/vista/util/AppUpdateManager.kt:112-137` | Device cannot verify the downloaded file; a compromised CDN or redirect serves arbitrary APK. | High | Parse and require both fields; fail closed if absent. |
| Download completion is based on `DownloadManager.STATUS_SUCCESSFUL` alone; no hash or size check before `DownloadComplete`. | `app/src/main/java/com/example/vista/util/AppUpdateManager.kt:147-196` | Corrupt or malicious APK is offered for installation. | High | Compute SHA-256 on-device, compare with manifest, compare byte length, then emit `DownloadComplete`. |
| Mandatory updates can be dismissed on TV via "Later" and dialog dismissal. | `app/src/main/java/com/example/vista/TvMainActivity.kt:284-313` | Security floor is optional on the largest screen; contradict release policy. | High | For `mandatory == true`, remove dismiss/Later, disable `onDismissRequest` and block navigation until install or a documented grace period expires. |
| The public verifier downloads `android` and `tv` assets but compares both to one global `sha256`/`sizeBytes`. | `scripts/verify_public_release.mjs:13-25` | A wrong TV asset can still pass if the JSON digest happens to match the Android one (they were historically identical), hiding a real regression. | High | Require per-asset `sha256`/`sizeBytes` in `vista-version.json`; verify certificate fingerprint too. |
| Update checks run from a singleton with a 30 s dedupe plus ON_RESUME checks; no backoff for repeated failures. | `app/src/main/java/com/example/vista/MainActivity.kt:352-373`, `app/src/main/java/com/example/vista/util/AppUpdateManager.kt:68-89` | Awake app can poll the manifest every 30 s indefinitely, wasting data on metered connections. | Med | Exponential backoff on failure, respect Data Saver, and stop the infinite `while(true)` loop when app is backgrounded. |
| `check_update` is `true` for all `updates`, including LX campaign links if miscategorised. | `app/src/main/java/com/example/vista/data/remote/firebase/VistaMessagingService.kt:70-72` | Tapping a campaign link could trigger an update prompt. | Med | Derive from explicit `data["check_update"]` only, and set it only in genuine release senders. |

### 2.9 AI assistant, voice and personalisation

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Cloud AI requires sign-in, but guest paths exist for local recommendations. | `app/src/main/java/com/example/vista/ai/AiRepository.kt:73`, `app/src/main/java/com/example/vista/ai/AiRepository.kt:195-219` | Two assistant experiences with different promises; guests may read "AI available" and hit a sign-in wall. | Med | Label cloud features explicitly in UI; keep catalog search and local taste recommendations working as clearly separate capabilities. |
| Voice session creates `TextToSpeech` and `SpeechRecognizer` on construction. | `app/src/main/java/com/example/vista/ai/VoiceSession.kt:26-34` | Microphone/engine initialization happens even if the user never starts voice; battery and permission UX cost. | Med | Lazily create on `start()`; release in `close()`; add a permissions-preflight state. |
| Cloud voice falls back to device voice on any failure. | `app/src/main/java/com/example/vista/ai/VoiceSession.kt:177-180`, `app/src/main/java/com/example/vista/ai/ElevenLabsSpeechPlayer.kt:38-47` | Good UX, but failures are not reported with cause or cost metrics; infinite fallback can hide an outage. | Med | Add bounded fallback counters, per-turn provider label in logs and a user-visible "using device voice" state (already partly present). |
| AI base URL is a hardcoded remote Worker URL in Gradle config. | `gradle.properties:62` | Environment drift; no staging/production switch other than editing local properties. | Med | Add `VISTA_AI_BASE_URL` per build variant with explicit dev/staging/prod values and a release-time assertion that production is HTTPS and expected host. |
| Prompt/response safety is delegated to the Worker. | `backend/src/worker.mjs`, `backend/test/safety.test.mjs` | Client cannot detect a degenerate response beyond decoding; hallucination can still produce impossible actions. | Med | Keep the client `AgentActionPolicy` as the final gate (already present) and add response schema versioning plus a "show source" disclosure for recommendations. |

### 2.10 Ads and monetisation

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| App Open and Interstitial ads use the same ad unit ID. | `app/src/main/java/com/example/vista/util/AdManager.kt:21`, `app/src/main/java/com/example/vista/util/AdManager.kt:68` | AdMob reporting cannot distinguish formats; creative frequency and eCPM analysis are useless. | Med | Create separate ad units and load them through a config keyed by format. |
| `lastShownTimestamp` is initialised to `System.currentTimeMillis()` at object construction. | `app/src/main/java/com/example/vista/util/AdManager.kt:73` | First interstitial is delayed by the full interval after every cold process start. | Low | Initialise to `0L` unless a policy specifically wants a warm-up period; document the choice. |
| Premium is currently announcement-only: R20/month copy with an ack flag, no billing. | `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:251-257`, `app/src/main/java/com/example/vista/TvMainActivity.kt:361`, `docs/premium-2027/README.md:3` | Fine today, but any user interpreting the card as purchasable is misled. | Med | Keep copy explicitly "coming in 2027", link to the waitlist/notice page and never imply a purchase path until billing gates are passed. |

### 2.11 Settings, privacy and analytics

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Analytics uses an unlimited channel and synchronous `execute()` in the flush loop. | `app/src/main/java/com/example/vista/data/analytics/VistaAnalytics.kt:52-53`, `app/src/main/java/com/example/vista/data/analytics/VistaAnalytics.kt:89-134` | Slow endpoint + event burst = unbounded memory; synchronous IO blocks the analytics coroutine and delays shutdown. | High | Use `Channel(capacity = 256)` with drop-oldest + counters, async/dispatcher-bound sends with timeout, propagate cancellation, flush on app background. Already documented as ST02 in `docs/premium-2027/11-STABILITY.md:27`. |
| Analytics payload schema contradicts the endpoint doc. | `app/src/main/java/com/example/vista/data/analytics/VistaAnalytics.kt:121-125` vs `ENDPOINT_INTEGRATION.md:81-84` | Backend/D1 queries can silently discard events. | High | Publish one JSON schema, add a contract test in `backend/test`, and version the payload. |
| `VISTA_ANALYTICS_KEY` is blank in local `gradle.properties:61`, so the feature is inert locally, yet docs claim it as live. | `README.md:48`, `BUILD_REPORT.md:118-125` | Marketing/reporting can overstate measured coverage. | Med | State "collection enabled only in builds with a configured key"; record actual key presence per release without printing the key. |
| No explicit analytics consent gate; `share_diagnostics` is a preference, not a collection gate. | `app/src/main/java/com/example/vista/data/local/PreferencesManager.kt:41`, `app/src/main/java/com/example/vista/data/analytics/VistaAnalytics.kt:55-56` | POPIA/GDPR-style transparency issue if a future build sets the key. | High | Make `enabled = key present AND user consent`; add a clear Settings toggle and honor it at send time, not only at init. |
| Plaintext third-party API credentials are tracked in `gradle.properties:51-53`. | `gradle.properties:51-53` | Anyone with repo read access can use the TMDB/Watchmode keys; rotating them still leaves them in git history. | High | Remove from the working file, inject via environment/CI secrets, rotate the exposed keys, scrub history or at minimum document the exposure and restrict repo access. Never paste values in docs or issues. |

### 2.12 Feedback polls and support

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Feedback submit has no idempotency key; "you won't be counted twice" is only a local UX promise. | `app/src/main/java/com/example/vista/feedback/FeedbackViewModel.kt:57-73` | Timeout + retry can double-submit to Firestore. | High | Generate a client UUID per submission and enforce it as the Firestore document ID or a unique field with transaction/idempotency check. |
| Feedback hidden/sent state is keyed by poll ID + UID in plain SharedPreferences. | `app/src/main/java/com/example/vista/feedback/FeedbackViewModel.kt:30-39` | Signing out/in or switching accounts reuses keys unless `authStateListener` resets correctly; it does, but guest→sign-in transition can unexpectedly lose answers. | Low | Scope state per account and migrate guest answers intentionally with clear UI copy. |
| Support actions swallow intent failures silently. | `app/src/main/java/com/example/vista/support/SupportActions.kt:31-35` | "Email support" can appear to do nothing on devices without a mail client. | Med | Show a copy-to-clipboard fallback with the address; add an instrumentation test for the fallback. |

### 2.13 TV experience

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| Mandatory update can be dismissed (already listed in 2.8) and TV uses a separate Activity/graph. | `app/src/main/java/com/example/vista/TvMainActivity.kt:284-313` | Phone fixes do not automatically apply to TV; device matrix must test both. | High | Share one update dialog component or add a parity test that asserts mandatory flows in both activities. |
| TV scroll/focus policy is unit-tested, but docs explicitly admit no physical TV/ADB verification. | `docs/TV_SCROLL_1_0_11.md:20`, `docs/PUBLIC_RELEASE_1_0_11.md:51` | Overscan, large fonts, RTL and D-pad-only navigation are still claims, not evidence. | Med | Book one physical TV session per release; record device, Android TV version, WebView version and screenshot/video evidence (no secrets). |
| TV hides/handles controls differently for native vs embedded playback. | `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:156-160`, `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:240`, `app/src/main/java/com/example/vista/ui/player/PlayerScreen.kt:661-664` | Divergent controller state can desync with `uiState.showControls`. | Med | Make `showControls` the single source of truth for both player types; derive native controller visibility from it rather than writing both ways. |

### 2.14 Backend worker and AI service

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| JWKS cache has no retry when an unknown `kid` appears. | `backend/src/worker.mjs:48-57` | Key rotation causes auth failures until cache TTL expires. | High | On unknown `kid`, force refresh once with a short cooldown, then fail with a clear 401/503 distinction. |
| Every NVIDIA error falls back to Workers AI. | `backend/src/worker.mjs:118-137` | Billing/auth/quota errors are masked; cost and incident response become unpredictable. | High | Fall back only on retryable 5xx/timeouts; surface 401/403/quota errors distinctly and alert. |
| `/health` discloses provider configuration. | `backend/src/worker.mjs:143-144` | Information disclosure to unauthenticated callers. | Med | Return only `{ok, version, time}`; keep provider detail behind an authenticated debug route. |
| Rate limiting is per authenticated UID only. | `backend/src/worker.mjs:151-163` | Unauthenticated/guest abuse and distributed fake accounts are not covered. | Med | Add IP/ASN + UID composite limits, progressive backoff and abuse metrics without logging raw identifiers. |
| Custom deploy script hardcodes bindings, model, rate-limit namespace IDs and uploads secrets directly. | `backend/deploy.mjs:50-64` | Environment drift and accidental deployment to wrong account are likely. | High | Use `wrangler deploy` with per-environment config files (`dev`/`staging`/`prod`) and CI secrets; never pass secrets via ad-hoc script arguments. |
| Docs claim root `.env` precedence, but `backend/env.mjs:7-12` plus `backend/package.json:5` make `.env.local` effectively win. | `backend/README.md:7`, `DEPLOYMENT.md:78` | Operators can deploy with the wrong keys while believing otherwise. | Med | Fix the docs or the loader; add a startup log that prints only the selected source name, never values. |

### 2.15 CI, iOS, docs and public claims

| Weakness | Evidence | Impact | Severity | Best fix |
|---|---|---|---|---|
| iOS workflow produces unsigned simulator/device artifacts only. | `.github/workflows/vista-ios.yml:35-59`, `ios/README.md:38` | Not installable by ordinary iPhone users; every download must say "unsigned developer artifact". | High | Complete Apple signing/provisioning before any public install claim; update `docs/IOS_SIGNING_PLAN.md:61`, which still says build 19 for first signed upload while `ios/project.yml:9-10` is already 1.0.12/build 19. |
| `ios/scripts/github-secrets.mjs:10` hardcodes the repository. | `ios/scripts/github-secrets.mjs:10` | Forks/mirrors push secrets to the wrong repo if run accidentally. | Med | Read repo from `GITHUB_REPOSITORY` env or require an explicit flag; fail if it does not match the expected owner. |
| README says public release 1.0.11 while changelog says 1.0.12 published. | `README.md:8`, `CHANGELOG.md:19` | Users and testers cannot tell what is actually shipped. | Med | Make `README.md` a generated summary from `vista-version.json` + release doc; add a CI check for version consistency. |
| `send_1_0_12_coming_fcm.mjs:6` contains a device-testing playback claim; `docs/PUBLIC_RELEASE_1_0_11.md:51` says no ADB device was attached. | `send_1_0_12_coming_fcm.mjs:6`, `docs/PUBLIC_RELEASE_1_0_11.md:51` | Overclaiming test coverage damages trust. | High | Edit or withdraw the claim; only assert checks that have named device/evidence. |
| `VISTA_QUICK_REFERENCE.md:3` admits some examples describe earlier implementations. | `VISTA_QUICK_REFERENCE.md:3` | Useful as history, dangerous as an operator runbook. | Low | Add "historical" banners to stale sections or generate the quick reference from current source. |

## 3. Prioritised improvement plan

### P0 — before any further public broadcast or paid work

1. **Rotate and remove committed credentials** (`gradle.properties:51-53`, `keystore.properties` handling, `ios/scripts/github-secrets.mjs:10`). Inject via environment/CI secrets. Audit git history; assume exposed keys are compromised.
2. **Fix notification categorisation** (`VistaMessagingService.kt:61-72`, `send_fcm_cli.mjs:38`, `send_insider_recruitment_fcm.mjs:90-93`). Separate essential updates, campaigns and recommendations. Keep LX Insider opt-in honest.
3. **Harden sideloaded updates**: require `sha256` + `sizeBytes` in `vista-version.json`, verify before install, use `getLong`, block mandatory-update dismissal on TV, and give each asset its own digest (`AppUpdateManager.kt:112-212`, `TvMainActivity.kt:284-313`, `scripts/verify_public_release.mjs:13-25`).
4. **Bound analytics** and add consent gating (`VistaAnalytics.kt:52-53,89-134`), align the payload schema with `ENDPOINT_INTEGRATION.md:81-84`, and add a contract test.
5. **Remove email from analytics** and add feedback idempotency (`FirebaseManager.kt:60-64`, `FeedbackViewModel.kt:57-73`).
6. **Correct public claims**: README version drift, unsigned iOS wording, 1.0.12 device-testing playback claim (`README.md:8`, `CHANGELOG.md:19`, `send_1_0_12_coming_fcm.mjs:6`, `docs/PUBLIC_RELEASE_1_0_11.md:51`).

### P1 — next 2-4 weeks

1. Replace unsalted PIN hashing and gate Kids-mode exit (`PreferencesManager.kt:332-348`).
2. Partition HTTP caches and expose catalog failure classes (`NetworkModule.kt:29-35`, `HomeCatalogLoader.kt:45-46`).
3. Add source health/allowlists to stream resolution and downloads (`StreamResolver.kt:84-127`, `DownloadPolicy.kt`, `BackupMovieSource.kt`).
4. Unify player release lifecycle and reduce broad WebView injection (`PlayerScreen.kt:198-210,625-645`).
5. Broker resilience: unknown `kid` refresh, selective AI fallback, minimal `/health`, UID+IP limits, Wrangler-based deploys (`backend/src/worker.mjs:48-57,118-144,151-163`, `backend/deploy.mjs:50-64`).
6. Port the FCM send-ledger to all broadcast scripts; one operator wrapper with `--send` gating (`send_insider_recruitment_fcm.mjs:114-135,188-213` as the reference).
7. Add one physical Android TV and one low-end phone session per release with recorded device/WebView version and screenshots. Treat debug/emulator results as necessary but not sufficient.

### P2 — product and polish

1. Honest guest-trial language or server-backed entitlement.
2. Search result caching, min-length threshold, visible "clear recent searches" (`SearchViewModel.kt:109-141`).
3. Split ad unit IDs and fix first-ad timing (`AdManager.kt:21,68,73`).
4. Remote, versioned WebView cleanup rules scoped to allowlisted origins.
5. Generate README/quick-reference version facts from release manifests; add docs drift CI.
6. Add a privacy page section covering analytics, recent searches, FCM topics, crash reports and support email handling.

## 4. What is already done well and should be preserved

- Policy objects are testable and used at real boundaries: `AgentActionPolicy`, `PlaybackUiPolicy`, `EmbedAdPolicy`, `ContentAccessPolicy`, `AnnouncementPolicy`, `VoiceInterruptionPolicy`.
- Sensitive headers are redacted in debug logging (`NetworkModule.kt:77-83`), and passwords are kept out of SavedStateHandle (`AuthViewModel.kt:41-43`).
- FCM deep links are allowlisted to official HTTPS hosts (`NotificationLinks.kt`) with browserless fallback (`VistaMessagingService.kt:108-125`).
- LX Insider sender validates links/copy, uses a ledger, blocks duplicate broadcasts and refuses validation when `/insider` is 404 (`send_insider_recruitment_fcm.mjs`).
- Announcement inbox is bounded to 20 entries (`AnnouncementsStore.kt:80-84`).
- Release docs already disclose unsigned iOS, no attached ADB device and unverified provider availability when read carefully (`docs/PUBLIC_RELEASE_1_0_11.md:11,51`).

## 5. Verification checklist for the next release

- `.\gradlew.bat :app:testDebugUnitTest --console=plain -q`
- `node --test backend/test/*.test.mjs *.test.mjs campaign/*.test.mjs`
- `node scripts/verify_public_release.mjs` against the exact published manifest and both asset digests.
- Confirm `vista-version.json` includes per-asset `sha256`/`sizeBytes`, and the app parses/verifies them.
- Confirm no plaintext credential remains in tracked files; rotate anything ever committed.
- Confirm `/health` reveals no provider names/keys.
- Confirm mandatory update cannot be dismissed on phone or TV.
- Confirm a promotion-tagged FCM message is suppressed when promotion notifications are off, and that no release notice is sent as a promotion.
- Confirm no ADB/device-driven claim appears in release notes unless a named device session exists.
