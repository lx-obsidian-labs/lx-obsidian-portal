# Stability-first engineering specification

**Priority:** prerequisite to the R20 paid pilot, not optional polish after billing. Research: [10-STABILITY-RESEARCH.md](10-STABILITY-RESEARCH.md). Verification: [12-STABILITY-VERIFICATION.md](12-STABILITY-VERIFICATION.md).

This document specifies work. The source observations below are inspection evidence, not claims that a production incident has been reproduced or fixed.

## What “stable” means

Vista starts and stays responsive; survives interrupted networks and OS recreation; preserves the correct user's data; releases player/microphone resources; reaches a truthful terminal state for every operation; and can recover from a bad dependency or release without duplicate charges or data loss.

Six non-negotiable invariants:

1. A previous account/request/session never writes into the next one's state or history.
2. Stop/dismiss/background means no unauthorized microphone capture or stale response resurrection.
3. UI timeouts and cancellation bound real work, queues and connections, not just visible spinners.
4. Retry cannot create duplicate financial grants, saves or provider charges.
5. Cache repair never deletes durable user data or active player resources.
6. A verified update is installable; an unavailable update never invents success or traps users in an unexplained loop.

## Source-backed work priorities

Paths below are relative to `app/src/main/java/com/example/vista/` unless prefixed with `backend/`.

| ID | Observed pattern and location | Risk / proposed correction | Severity |
|---|---|---|---|
| ST01 | `ui/settings/SettingsViewModel.kt:160–193`: recursive file scans inside default `viewModelScope.launch`, including inside `MutableStateFlow.update` | Main-thread disk work can stall Settings; CAS reducers may reevaluate expensive work. Compute once on injected IO dispatcher, publish a small immutable result; correct `filesDir + dataDir` overlap | High responsiveness |
| ST02 | `data/analytics/VistaAnalytics.kt:52–53,89–103`: `Channel.UNLIMITED`, synchronous sending and broad receive catch | Slow upload plus event bursts can grow memory; cancelled receive must not become a busy loop. Bounded optional queue, explicit overflow/drop counters, propagate cancellation and cap payload bytes | High memory/lifecycle |
| ST03 | `ai/AiRepository.kt:131–148`: blocking `execute()` in `withContext(IO)`; `AiChatViewModel.kt:143–145` cancels only its job | Cancellation is not explicitly coupled to the HTTP call. Use a tested cancellable adapter and one owner/generation for the whole turn; bound in-flight work | High resource use |
| ST04 | `ui/player/PlayerViewModel.kt:427–498`: recovery job has no enclosing terminal-state/error boundary | Exception/cancellation before a handled result can leave recovery state stale; uncaught coroutine failure can reach the app handler. Serialize recovery, generation-check results, settle UI state in a scoped `finally` without cancelling a newer operation | High playback |
| ST05 | `ui/player/PlayerViewModel.kt:370–397,532–538`: final persistence reads current progress during a delayed exit save; repository resolves current owner at write time | Snapshot/account/episode may change across suspension; OS kill need not run `onCleared`. Persist owner+title+episode+progress together periodically and at explicit lifecycle checkpoints; idempotent ordered writes | Critical if reproduced as wrong-owner/lost data |
| ST06 | `util/AppUpdateManager.kt:183–189`: bytes and `received * 100` use 32-bit Int | Overflow starts above 21,474,836 received bytes; current APK is 22,369,333 bytes. Use Long end-to-end and safe indeterminate progress for unknown totals; test boundary arithmetic | Confirmed arithmetic defect candidate |
| ST07 | `util/AppUpdateManager.kt:138–203`: download ID exists only in a coroutine; missing query row has no terminal path | Restart/removed downloads can lose progress or poll indefinitely; repeated starts can race on one filename. Persist one download operation and restore via DownloadManager; bounded stale/paused handling, unique owner and single-flight | High distribution |
| ST08 | `util/AppUpdateManager.kt:103–124,169–173`: parses version/URL but not published SHA-256/size; success immediately exposes file | Add verified-size/hash/package/version/certificate checks before install action. Android still validates updates; client preflight adds clear diagnosis, not a replacement for platform trust | High update integrity |
| ST09 | `ai/VoiceSession.kt`: shared `vista-response` utterance ID and delayed callbacks without generation identity | A late callback from an old session can interfere after stop/start. Generation-tag recognition/TTS events; own audio focus and permission transitions explicitly | High lifecycle |
| ST10 | `data/cache/CacheManager.kt`: catches deletion errors; `worker/CacheMaintenanceWorker.kt:27–34` may report success after swallowed failures | Cleanup outcomes are not observable; cache guards require race testing. Typed result with bytes/failures/skipped-active, serial deletion coordination, no durable-store eviction | Medium/high storage |
| ST11 | `data/remote/playback/StreamResolver.kt:120–125` constructs a new client per fetch; `di/NetworkModule.kt` has connection/read limits but no full-operation call budget | Repeated source checks create additional pools and slow chained paths. Reuse appropriately configured clients; cap source fan-out and total resolution deadline | Medium resource/latency |
| ST12 | `backend/src/worker.mjs`: each mode can wait 25s on NVIDIA then independently fall back; request body fully buffered before size rejection | Two sequential modes can exceed a useful user budget; input memory limit is late. Bound streamed input, aggregate turn deadline, measured provider health, finite fallback and admission control | High backend resilience |
| ST13 | `ui/player/PlayerScreen.kt:440–499`: main-frame error handler and destruction exist; no renderer-death handling in reviewed client | Add version-supported WebView renderer recovery and lifecycle tests; recreate only a fresh owned view, never reuse a dead renderer or permit arbitrary redirects | High on affected devices |
| ST14 | `VistaApp.kt`: process-owned startup scope exists, alongside additional per-operation scopes and broad failure catches | Consolidate ownership, distinguish optional SDK failures and cancellation, defer optional startup work and report redacted errors without blocking launch | Medium startup |

ST06's arithmetic follows directly from the code; actual visible progress behaviour still needs device reproduction. Other items require fault injection/traces before assigning a root cause to a user report. Existing safeguards—targeted invalidation, two recovery attempts, disabled local WebView access, owner-scoped Room and explicit release hooks—are foundations to preserve, not remove in a rewrite.

## A. Main-thread, startup and UI discipline

- No recursive storage inspection, blocking HTTP, large JSON decoding or database migration on Main. Keep state reducers/composables side-effect-free; inject IO/Default dispatchers for deterministic tests.
- Distinguish expected domain failures from programmer defects. Re-throw cancellation; use typed `Result`/sealed states where recoverable. Do not turn every exception into an empty successful catalogue or silently swallow fatal errors.
- Every operation exposes `idle/loading/partial/success/failure/cancelled` as appropriate and has exactly one current operation ID. Completion of an older operation cannot clear a newer spinner or overwrite its result.
- Measure first frame, first interactive cached Home and first fresh content separately. Ads, optional analytics, update checks and voice initialization cannot gate a usable cached Home screen.
- Save only small restoration keys in saved state: route/search/filter/media IDs and owned-operation references. Room/durable storage holds larger records; do not bundle whole catalogues/bitmaps/transcripts into saved state.
- Test navigation/rotation/recreation and repeated tap/Back operations. Add debug StrictMode/trace-based inspection for main-thread violations; do not ship a debug penalty that kills production users.

## B. Networking, retries and overload

Proposed starting limits, calibrated against measured network percentiles rather than treated as vendor mandates:

| Logical operation | Deadline / retry owner | Failure state |
|---|---|---|
| Cached catalogue read + refresh | Cached UI immediately; remote refresh budget 12s; repository may make one idempotent retry within it | Keep last-known data with age and retry control; distinguish empty from unavailable |
| Search | Debounce 300ms, latest-query wins; 10s per query including retry | Cancelled query cannot replace newer results; Retry uses current query |
| Complete AI turn | 20s hard total target; planner+tools+reply+fallback share remaining time; p95 target ≤12s | Explicit timeout/service state; release customer reservation, account for actual provider cost |
| Stream descriptor resolution | 20s total across supported candidates, bounded concurrency | Distinguish denied/unavailable/network/unsupported; no provider cycling after denial |
| Playback automatic recovery | At most two application-level attempts with saved position and one coordinator | User-visible finite failure; media engine's own load retries included in measured whole-path budget |
| Billing mutation | Deadline set from validated provider contract; durable intent/idempotency and read/reconcile path | Unknown outcome stays pending; never resubmit a fresh charge key |
| Background sync | Unique durable work per account, constrained network, bounded exponential backoff with jitter | Retain outbox and next retry reason; no infinite foreground loop |

The two-mode AI protocol must carry a shared remaining-deadline/request identity until replaced with one server operation. An HTTP call timeout alone does not cap a chain of calls.

Do not retry 400 validation errors, permanent 403s, deliberate cancellations or deterministic unsupported codecs. For 401, allow a single coordinated identity refresh when appropriate; refresh cannot grant provider rights. Honour bounded `Retry-After` for 429/503 and show when the operation cannot fit the foreground budget. Use randomized backoff within total limits; prevent retries independently multiplying in app, repository, Worker and provider adapters (R06).

Bound synchronous as well as asynchronous concurrency; OkHttp Dispatcher limits do not govern arbitrary blocking `execute()` threads (R04). Initial proposal: one active cloud conversation per account/device, small bounded catalogue/source concurrency, optional telemetry lower priority. Validate limits under real memory and provider quotas. Circuit breakers need finite open time and one controlled probe; do not introduce an untestable permanently-open state.

## C. Memory, storage and data durability

- Replace unbounded telemetry with a proposed 256-event queue and 64 KiB encoded batch ceiling; drop/coalesce optional events without blocking UI and record a counter. Never drop billing facts or user mutations: those belong in durable bounded outboxes with explicit full-state errors.
- Bound images by rendered dimensions; cancel off-screen requests and check low-RAM navigation/long lists. Detect retained Activity/WebView/player/recognizer instances using debug inspection; count ownership before assuming a leak.
- Coordinate active-player registration and deletion under one ownership mechanism. A check-then-delete race is not prevented solely by a synchronized set. No forced deletion of active stream files in ordinary cleanup.
- Compute cache results accurately and outside Main. If disk is full/read-only, do not claim a successful save/cleanup. Preserve Room/database/WAL and signing/update metadata; never clear databases as playback repair.
- Every durable write carries immutable UID, media/episode ID, sequence and operation ID captured before suspension. Writes must not consult a different current account halfway through.
- Verify Room schema+data+indexes for every supported upgrade. Disk corruption/storage failure produces a recovery/export path and evidence, not `fallbackToDestructiveMigration()`.
- Flush playback checkpoints during normal operation; `onCleared`/`onDestroy` are not a guarantee against OS process kill. Explicitly state the maximum expected position-loss window (proposed 5s direct playback) and test it.

## D. Media and voice lifecycle

One clear owner creates/releases each player/WebView and registers/unregisters active sessions. Exercise source change, episode change, PiP, pause, audio focus, route exit and renderer/decoder failure. Treat Media3 and WebView separately; do not claim the same seek/resume guarantees for both.

Recovery records generation/source identity and position, stops the affected operation, revalidates descriptor/permission, then resumes only if still current. Every path settles recovering state, including cancellation and preference/account changes. Late callbacks must not resurrect exited playback.

Voice has generation-scoped recognizer and utterance IDs; no callback may restart an old conversation. Stop on explicit Stop/dismiss/background and permission/audio-focus loss according to the product's foreground policy. Use a visible Stop while speaking because voice commands cannot be heard during microphone suspension. Repeated silence/errors pause rather than loop forever.

## E. Update and release resilience

- Store a single update operation with versionCode, URL, expected size/hash, download ID and verified file state; recover it after process death without deleting an in-progress file.
- Validate HTTPS and approved redirect/download hosts. Unknown total size is indeterminate, not zero failure. Read byte counters as Long and prevent duplicate installer launches.
- Handle absent/failed/paused download, low storage, permission denial, returned-from-settings, installer cancellation and an already-installed version explicitly.
- Verify actual downloaded artifact against trusted release metadata and installed package/certificate continuity before exposing Install. Do not present a hash in the manifest as integrity enforcement if the client ignores it.
- Separate public availability, optional install prompt, backend feature enablement and mandatory minimum-version enforcement. A universal mandatory pointer defeats APK canarying; reserve it for a demonstrated compatibility/security need after pilot evidence.
- Recovery for an installed bad APK is a signed higher-versionCode fix; server feature rollback is separate. APKs remain GitHub-only and are excluded from Cloudflare output.

## F. Measurable objectives and stop conditions

All are proposed targets, measured on the declared release/profile/device/network cohort:

| Measure | Initial criterion |
|---|---|
| Fatal crash-free sessions | Observed ≥99.5%; also report crash-free installations and coverage, raw counts, version/device groups; never use it as an ANR metric |
| ANR/freeze | Zero reproducible new ANRs in scripted critical journeys; diagnose available field ANR/exit reports and trace long stalls separately; missing reports means unknown |
| Startup | Reference mid-range cold first display p95 ≤3s; cached Home interactive p95 ≤4s; regressions >15% against matched baseline require investigation |
| Foreground responsiveness | Target feedback to tap ≤100ms on measured reference tasks; investigate repeated frozen frames >700ms with traces, not only visual impressions |
| Memory | No OOM in soak/device matrix; after warm-up, investigate >10% persistent retained-memory growth over 20 identical feature cycles and any retained exited owner |
| Stop/cancel | Under controlled reachable-network tests, no new provider dispatch after cancellation observed, and local transport cancel issued within 1s; remote abort cannot guarantee zero provider billing |
| Data | No wrong-owner writes, lost confirmed saves, tombstone resurrection, duplicate payment or accidental active-cache deletion |
| Recovery | Every fault test has a truthful finite state; all invariants are hard blockers even when averages look good |

No universal heap-MB limit or “60 FPS everywhere” promise. Device refresh rate, graphics memory, codec path and telemetry consent affect measurements. Freeze new optional features when a critical stability invariant fails; fix and rerun the targeted suite before resuming scope.
