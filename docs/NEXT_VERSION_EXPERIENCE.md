# Next version: reliable discovery, playback and offline planning

Review date: 5 November 2026. Proposed release: **1.1 reliability release** (version/signing manifests are not changed by this work).

This is a source-led product review of the Android feature/data paths and existing backend/product documentation. Findings are code observations, not reproduced ISP incidents. The working tree already contained substantial ongoing 1.0.9/AI/premium work. This document supplements the premium-2027 specification; implementation status below describes this change set only.

## Product decision

Make choosing and watching a title predictable before adding more promotional surfaces. The best experience preserves what the viewer was doing, explains failures, respects data costs and offers a finite next action. “Always works on every network” cannot be established by a code change: provider availability, ISP filtering, captive portals, device codecs and CDN authorization are external dependencies. The engineering goal is successful playback where the source is reachable, and useful, truthful recovery everywhere else.

## Data provenance: these are different systems

| Data | Actual path | Offline implications |
|---|---|---|
| Movies, series, search, genres, credits, episodes | TMDB; TVMaze series/schedule support; Shoofaflam extras; IMDb helper for mapping | Metadata is not a movie file. Cache separately, keep namespace and media type, expose age when stale |
| Posters/backdrops | TMDB/other image URLs through Coil | Image cache is evictable and cannot establish offline title completeness |
| Where to watch | Watchmode | Availability is regional/time-sensitive; stale data cannot prove playback or download access |
| Playback | `VidsrcPlaybackRepository` builds provider embeds from TMDB/IMDb IDs; `StreamResolver` attempts direct manifest/file extraction | A working catalog does not imply a reachable embed or CDN; a direct URL can expire or require headers/cookies |
| Actual video | Media3 for direct HLS/DASH/files; WebView for provider embeds | Native and embedded capabilities differ. WebView cache and player temp files are not offline downloads |
| Saved titles/history | Owner-scoped Room repositories; settings in DataStore | Available locally; no demonstrated cross-device sync or complete offline media library |
| AI/voice | Firebase identity → Cloudflare assistant → NVIDIA; device recognition/TTS | Online generation is separate from catalog and video reachability; installed voices vary by device |
| Updates | Website JSON → Android DownloadManager → package installer | APK downloads need their own durable operation, integrity checks and recovery; do not reuse media queue semantics blindly |

## Feature-by-feature criticism and next experience

“Implemented” means source changed in this pass. “Planned” means work still required; it is not a shipped promise.

| Feature | Criticism grounded in current behavior | Enhancement / status |
|---|---|---|
| Home / hero / rows | Large all-at-once fan-out; empty/error responses can erase a useful screen; generic errors obscure provider failure | **Implemented:** single-flight initial/refresh loading, preserve populated Home on wholly failed refresh, nonblocking retry snackbar. **Planned:** disk catalog snapshots with fetched-at timestamp; independent row refresh and partial failures |
| Trending / Top 10 / section paging | Ranking labels can imply editorial/global facts; page errors are often converted to empty success and stop pagination | **Planned:** label actual ranking source; typed page result with retry for failed page; dedupe by media type + provider ID |
| Because You Watched / Play Something | Recommendations can include unavailable content; “trending” TVMaze search is a literal search rather than a real trending feed | **Planned:** candidate metadata validation, explainable preference chips, exclude disliked/watched titles by typed ID; rename or replace synthetic feeds |
| Discover / genre tabs | Fast filter/tab changes can race; Anime is animation genre, not an anime classifier; repository popular extras can repeat on later pages | **Planned:** one request generation per filter, real year/sort/language query contract, honest Animation label, stop fetching extras after page one |
| Search / people / history | Debounced, submitted and tab searches had separate job ownership; cancelled old work could publish errors/results; outages looked like no matches | **Implemented:** latest-request generations, unified cancellation, clear resets pending work/counts; repository propagates cancellation, reports total remote counts, local extras first page only, total service failure is an error. **Planned:** explicit partial-source messages and saved filter restoration |
| Saved / watchlist | Room is a strong base, but rapid poster toggles can race and gesture-only discovery is weak | **Planned:** visible Save state + Undo; serialize mutations per typed title; add empty state with discovery shortcut; retain account ownership during async operations |
| Details / cast / trailers / similar | Optional dependencies hold up the title; first-season lookup can scan every season; metadata and provider availability are conflated | **Planned:** render essential title first; independently load cast/availability/trailers; query selected season once; explicit no-trailer/no-availability states; “where to watch” remains distinct from playback |
| Season / episode selection | Latest selection is not guarded; resume was using a series-level timestamp for any episode | **Implemented:** resume a saved timestamp only when season AND episode match; publish next-episode result that was previously discarded. **Planned:** episode-keyed progress, cross-season next episode, latest-selection ownership |
| Native playback | Quality menu only changed text; next-episode navigation depended on the controls overlay and could cut off the final seconds | **Implemented:** apply quality/Data Saver height preference to Media3 track selection; autoplay after actual native end, independent of controls. **Planned:** expose real available renditions, chosen bitrate/height and audio/subtitle tracks; replace synthetic Skip Intro with real chapter markers |
| Embedded playback / ad filtering | Native controls intercepted provider taps although native seeking/speed did nothing; direct and embedded sources looked identical | **Implemented:** embedded-source labels, preserve provider fallback, remove native overlay for embeds, explain provider-owned quality/speed controls; shared hostname-anchored ad request filtering and targeted top-document overlay cleanup. **Planned:** renderer-death handling, bounded blank-page/load timeout and physical-device lifecycle verification |
| Repair Playback | Recovery could leave its spinner active on unexpected exceptions; DNS failure is often treated as device-wide offline | **Implemented:** automatic/manual recovery settle recovering state on failure/cancellation. **Planned:** distinguish offline from provider-only DNS/CDN failure; one shared recovery deadline; explicit authorization/codec/network outcomes, never clear owned data |
| Connectivity | Any-network callbacks confused Wi-Fi loss with total outage; INTERNET capability alone was treated as validated connectivity | **Implemented:** monitor default route and validation, re-evaluate replacement route on loss, captive-portal-aware initial playback message. **Planned:** endpoint-specific diagnostics and network-handover policy during active playback |
| Cellular / Data Saver | Default Always Ask actually blocked mobile data; Wi-Fi/Ethernet were assumed inexpensive | **Implemented:** viewing-session data confirmation; metered/cellular classification at initial access and manual repair; Wi-Fi Only rejects metered routes. **Planned:** policy enforcement during Wi-Fi → mobile handover for BOTH native and embedded playback; roaming opt-in; bitrate/bytes estimate |
| Metadata cache | Shared HTTP cache exists, but remote failure has no bounded cache-only fallback | **Implemented:** allowlisted public TMDB/TVMaze GET cache fallback for connection failure and 500/502/503/504, up to seven days stale; never hide auth denial or cancellation. Server cacheability still controls whether an entry exists. **Planned:** explicit repository freshness state and immediate Room snapshot rendering |
| Watch history / Continue Watching | Embed duration is unknown; series progress is title-scoped; final writes need stronger frozen-owner/snapshot sequencing | **Planned:** immutable owner/title/episode/progress checkpoint every five seconds and pause; separate unknown-position embed history; test account switch/process kill |
| Stats / streaks / sharing milestones | Engagement counts can suggest precision that incomplete embed history cannot provide | **Planned:** label tracked native watch time; no invented embed duration; review streak timezone rules; opt-in social prompts with quiet default |
| Profile / guest / onboarding | Guest time/trial and future premium messaging can be confusing; local-first data is mistaken for synced data | **Planned:** clear trial/access states, explicit guest-import choice, truthful local-device library status; onboarding skippable with deferred permission prompts |
| AI / floating chat / companion | Two-step planning/reply adds latency and cost; optional AI failure must not gate watching; client hints are not authoritative catalog facts | **Planned:** aggregate turn deadline, real transport cancellation, measured routing, grounded typed title actions, editable explicit tastes, bounded history; keep existing premium budget/entitlement work separate |
| Voice | Quality depends on installed engines; late recognition/TTS callbacks need generation ownership | **Planned:** audible voice preview, visible Stop, interruption/audio-focus lifecycle tests, finite silence handling and text fallback |
| Settings / storage / themes | Recursive disk scans ran on Main and double-counted filesDir inside dataDir | **Implemented:** IO-dispatched size collection outside state reducers, non-overlapping internal/external totals. **Planned:** distinguish metadata cache, image cache, temporary video and durable offline media; accessible theme contrast and scalable text |
| Notifications / daily title / inbox | Preference changes, OS permission and notification category semantics must agree; suggestions can have unavailable playback | **Planned:** category-specific permission-aware delivery, source-neutral daily-title action, deduplication by announcement ID, no paid-launch activation from local clock |
| Sharing / referral site / ads | Incentives and interstitials can interrupt intent; referral rewards require a durable ledger | **Planned:** share canonical typed title links, no interstitial before recovery/offline errors, bounded opt-in referral programme after billing economics are proven |
| Android TV | Voice-first alone is insufficient; focus, remote-only recovery and Ethernet deserve equal treatment | **Planned:** D-pad journey matrix, stable focus on row reload, explicit keyboard fallback, remote-accessible network/source controls; no phone-only download flow on TV |
| In-app updates | Local download ID, 32-bit progress arithmetic, missing-row polling and absent hash verification remain reliability gaps | **Planned:** durable single update operation, Long counters, bounded paused/missing-row handling, verified APK size/hash/package/certificate before installer; separate release artifact verification from catalog caching |
| Analytics / backend / release | Optional telemetry cannot prove successful playback, payment or user retention; outage retries can multiply across layers | **Planned:** bounded consent-aware diagnostic queue, redacted failure taxonomy and request IDs; shared AI turn budget and operational feature rollback; signed higher-version hotfix process |

## Network reliability design

Fibre is an upstream access link; Wi-Fi/Ethernet are device transports. A phone can use fibre over Wi-Fi, mobile data over a hotspot, or a VPN over either. Do not select behavior by transport label alone, force IPv4 globally, hard-code public DNS, disable TLS validation, or assume that changing DNS fixes provider authorization.

Implemented transport foundations:

- Default-route connectivity callback with validated-internet state. Old-network loss rechecks the replacement route instead of forcing false.
- Metadata clients have a 25-second call timeout in addition to connection/read/write limits. This bounds a call, not an entire multi-call feature.
- Resolver reuses connection resources, disables response caching for provider pages, cancels actual OkHttp calls on parent cancellation, allows two probes at once, and includes permit waiting in a 12-second probe budget. Optional ID enrichment is capped at six seconds, retaining known numeric/IMDb IDs on timeout.
- Known direct-source failures retain separately labelled embedded alternatives; native and embedded controls are presented honestly.

Remaining design:

1. Track network route generation, validated/captive state, metering and roaming; validation is a diagnostic hint, not proof every endpoint is reachable. Test networks that block Android connectivity probes before treating validation as a universal access gate.
2. Re-evaluate data policy before continuing any active native/embed connection on a changed route. Pause and ask on newly metered routes; preserve user pause and position; do not resume audio behind a dialog.
3. Diagnose **catalog / images / identity / assistant / embed / CDN / update** separately. Record hostname category, error class, elapsed time and network class; never signed URLs, query tokens, cookies, raw SSIDs, public IPs or viewing titles in diagnostics.
4. Keep bounded per-provider/route health with expiry. Probe a recovered route again; a failure on mobile must not blacklist that provider on fibre for the rest of the session.
5. Native network loss should preserve source and position, wait for validated route, then retry once with the current policy. Provider-only failure offers a specific source switch. No infinite cache-clearing loop.
6. First-class offline catalog snapshots need fetched-at/source/version fields. Display “Saved catalog · updated …”; never turn cache fallback into a false claim of live availability.

## Downloads: source-aware delivery specification

Current status: **no media download queue or download worker exists**. This pass plans offline delivery; it does not label stream cache as downloaded video or add a nonfunctional Download button.

### Eligibility and fresh descriptors

Obtain a download-capable descriptor from the provider integration at enqueue/start/resume time. Store the title identity `(owner, provider namespace, media type, title ID, season, episode)` independently of the temporary URL. The descriptor needs:

- Provider ID, explicit offline support, descriptor generation and renewable stable content identity.
- Stream kind, HTTPS URL, required request headers, expiry in **documented UTC milliseconds**, content MIME/type and selected video/audio/subtitle tracks.
- Expected size if known, strong validator/content hash when available, Range support, permitted expiry/retention and offline-license requirements.
- A refresh operation that returns the same episode/rendition identity. An arbitrary regex-extracted `.mp4` is not evidence that it is the requested feature film.

The current resolver's `expiresAt` comes from optional HTML fields whose units/coverage are not normalized. Never use it as a reliable offline deadline until parsing and query-based expiry contracts are implemented per provider. Cookies/Origin/Referer support must be tested per CDN; do not leak one provider's credentials to another host.

| Actual source | Download decision |
|---|---|
| TMDB/TVMaze metadata, image, Watchmode availability URL | Not video; save metadata/poster separately |
| Embed-only page (`OTHER`) | Explain “Streaming only from this source”; do not save HTML as a movie |
| Verified progressive MP4/WebM/etc. with explicit provider offline support | Resumable file engine; confirm real media body, content identity, expected size and validator |
| VOD HLS or DASH with offline-capable descriptor | Media3 DownloadManager/DownloadService with selected tracks and persistent DownloadIndex/Cache; fetch complete selected manifests/segments/keys through supported provider contract |
| Live manifest | Unsupported for full-title offline download; no never-ending job |
| Expired/soon-expiring URL | Refresh first; compare content identity; restart partial download if content changed |
| DRM without offline license integration | Unavailable offline with a specific reason; streaming authorization alone is insufficient |
| Unknown size/bitrate | Indeterminate bytes/time; probe or conservative estimate, never fabricated progress/space certainty |

### Queue, data costs and storage

- Default to **unmetered, non-roaming**, one active video download. “Wi-Fi” alone is insufficient because hotspots can be metered. Per-item explicit cellular override shows estimated bytes and applies to that item only; roaming needs independent opt-in.
- UX: choose episode/quality and visible estimated size → confirm network/storage policy → queue. Start with single movie/episode; season batches follow only after reliable single-item recovery.
- Durable states: `Queued → Resolving → WaitingForNetwork / WaitingForUnmetered / WaitingForStorage → Downloading → Verifying → Complete`; also `PausedByUser`, `RefreshingSource`, `Failed(reason, retryable)`, `Removing`. Persist state/reason/attempt/bytes/validator/account ownership so process death cannot invent completion.
- Foreground media download service with the required Android 14+ data-sync service declaration/permissions and notification channel; use the supported Media3 service requirements rather than a long-running coroutine or a generic worker per segment. WorkManager can schedule bounded deferred resolution/maintenance.
- Use app-owned durable storage isolated from `cacheDir/player_temp`, metadata/Coil cache and cache repair. Reserve space for concurrent writes and OS headroom; recheck periodically. Cache-limit UI must not delete completed downloads.
- Progressive resume requires `Range` plus validator. A `200` response to a resumed request restarts safely; a changed ETag/identity discards incompatible partial bytes; a `416` verifies expected length before treating anything as complete. Never append an HTML/403 response to media.
- For HLS/DASH use Media3's offline segment index and offline playback path, not “download manifest then hope it streams later.” Completion verifies selected-track coverage; test playback in airplane mode including seek near end and subtitles.
- Resolve signed descriptors just in time. A 401/403 distinguishes expiry refresh from definitive denial; at most one supported descriptor refresh, no cycling to unrelated providers. Backoff transient failures with a finite attempt budget; preserve user pause and show next retry.
- Download library: bytes/progress, waiting reason, Pause/Resume/Retry/Delete, expiry and storage total; no auto-delete of unwatched downloads by default. Downloads remain isolated by owner; sign-out policy explicitly explains retained/removed media.

### Delivery order and acceptance

| Stage | Scope | Acceptance |
|---|---|---|
| 1. Reliability foundation (this patch) | Connectivity, bounded source work, cached fallback, data confirmation, latest search, useful Home, native controls, IO storage scans | JVM regressions plus debug build; device/network validation still required |
| 2. Trustworthy online UX | Route handover policy, typed partial failures, progressive details, episode ownership/checkpoints, provider diagnostics | Offline/captive/blocked-provider and account-switch journeys reach finite truthful states |
| 3. Download-capability integration | Provider descriptor contract, verified media identity, expiry refresh, source-specific eligibility | A real provider test fixture proves offline support; unsupported titles have a reason |
| 4. Single-title offline pilot | Durable queue/service/storage/index, progressive + supported VOD adaptive engines, offline library | Complete/resume/process-kill/low-space/changed-content tests; airplane-mode full seek and correct episode |
| 5. Expansion | Season batches, smarter recommendations, sync, premium features under existing product spec | Only after measured playback/download success and cost/retention evidence |

## Verification matrix and honest release criteria

Use the same native title, embed-only title, series episode, search and APK update across each route. Record device/Android/WebView version, provider category and outcomes; do not call a list of planned tests a pass.

| Route / fault | Required observation |
|---|---|
| Fibre via Wi-Fi and Ethernet | Catalog/image/playback endpoints checked independently; native start, seek, quality preference and repair |
| Mobile 4G/5G, metered hotspot, roaming | Always Ask actually asks; reject/cancel starts no video; Wi-Fi Only does not stream on metered routes |
| Wi-Fi ↔ mobile while buffering/playing | No lost position, incorrect offline UI, duplicate player or unapproved mobile transfer; active handover policy remains a release gate |
| Captive portal / Wi-Fi without upstream | Clear sign-in/reconnect message, no false success; cached catalog can remain useful |
| IPv6-only/NAT64, dual stack with broken IPv6, ISP DNS failure | Finite deadlines; real resolver traces distinguish name/address/TLS failures without TLS bypass |
| Catalog works, provider or CDN blocked | No global “everything offline” diagnosis; explicit source error/alternative and finite retry |
| 200–800 ms latency, loss, throttling, airplane mode | Cancellable searches, no stale result overwrite, preserved Home, bounded resolution and recovery |
| 401/403/429/5xx / expired source | No stale authorization success; respect rate limits; refresh only under provider contract |
| Rotate/background/process kill/TV remote | Resources released, newest request wins, remote recovery usable; download completion survives only if persisted and verified |

Automated checks added in this patch cover metadata fallback policy (including auth/cancellation exclusions) and latest-search/clear-query races. JVM tests do not verify Android connectivity callback ordering, WebView behavior, provider health or real ISP routing. Record build/test results separately when executed.

## Execution record

- `:app:testDebugUnitTest :app:assembleDebug` passed on 5 November 2026, using the Gradle wrapper main class with `--offline --no-daemon -Pksp.incremental=false`. The nonincremental KSP flag worked around a generated-file collision encountered during verification. Existing deprecation/opt-in warnings remain.
- Twelve new regression tests cover metadata cache policy, latest-search ownership and embedded ad-host matching (including CDN URLs with ad-domain text in their query/path, observed notification-ad resources and genuine verification exclusions).
- Built `app/build/outputs/apk/debug/app-debug.apk`; installed successfully with `adb install -r` onto the connected Android phone and launched MainActivity. Release version/distribution manifests were not changed.
- The user's player-ad report prompted a focused WebView change. TMDB provides metadata, not these video ads. Known ad requests are filtered in the embed and resolver; known ad overlays in the top-level provider document are cleaned up. Pop-up blocking remains active.
- Follow-up device inspection reproduced the fake robot ad on VidSrc's movie embed. It was a same-origin injected blank iframe containing a Cloudpiko notification-ad template, alongside scripts from observed ad hosts and a click-capture layer. Applying the targeted cleanup removed that iframe/layer while retaining the actual provider player iframe; a subsequent screenshot showed the film playing. The final APK containing those rules was built and installed successfully. Genuine verification widgets are excluded from these ad rules.
- Cleanup cannot see arbitrary cross-origin iframe DOM, and cannot reliably separate an ad stitched into the same media stream from the film. No claim of universal ad removal or network success is established by this specific device reproduction or the JVM suite.
