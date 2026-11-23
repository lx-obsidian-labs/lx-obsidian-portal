# Vista downloads: product and implementation plan

Status: proposed design, 5 November 2026. Media downloading is not implemented by this document. This is the detailed offline-delivery specification beneath [the next-version experience plan](NEXT_VERSION_EXPERIENCE.md).

## 1. Main decision

Build **source-aware adaptive downloads**, not an MP4-only save button. Launch with one movie or episode at a time, one active transfer, unmetered-network default, a persistent queue and genuinely network-free playback of completed items. Expand to season batches after single-item recovery is proven.

Vista's own AdMob integration remains separate from provider-page filtering. Download eligibility, background transfers and opening a completed local title must not depend on an ad successfully loading. This plan does not change existing AdMob units or placements or introduce an ad-unlock scheme.

## 2. What the code tells us

| Evidence | Design consequence |
|---|---|
| `AppDatabase.kt`, `MIGRATION_3_4`: an earlier downloads table existed | Do not assume we are introducing offline history for the first time; preserve supported upgrade paths |
| `MIGRATION_4_5`: downloads removed because sources returned adaptive playlists, not saveable files | HLS/DASH must be first-class; restoring the old MP4 worker would repeat the failure |
| Current Room schema is version 6, with watchlist/history/search only | Add a version 7 migration and new exported schema if the feature proceeds; do not resurrect the old unowned table |
| `VidsrcPlaybackRepository` builds embed URLs using TMDB/IMDb/title mapping | A catalog ID is not a download URL; pin correct movie/series/episode and provider identity |
| `StreamResolver.resolveAll()` attempts HTML extraction and returns HLS, DASH, MP4 or no direct result | Extraction is best effort. Some titles will be streaming-only; JavaScript embeds may expose no reusable offline descriptor |
| Current `StreamSource` carries headers, optional expiry and DRM info, but no content identity/offline capability | Introduce a typed provider download descriptor before queue implementation |
| Native playback already uses Media3 1.3.1 and HLS/DASH modules | Reuse Media3's download/index/cache/service stack; align any added Media3 artifact versions |
| `PlaybackAccess.requireAccess()` checks online connectivity and session before playback | Completed downloads need a distinct offline access path; do not accidentally remove online access checks for streaming |
| `CacheManager` deletes designated temporary/HTTP/image caches | Durable offline media must have independent storage and deletion ownership |
| `ProfileScope.current()` returns Firebase UID or blank guest owner | Freeze owner when enqueuing; define an isolated guest namespace if guest downloads are later supported |
| `Screen.Library` already groups the viewer's personal content | Add a Downloads tab within Library, reachable when offline; avoid another bottom-navigation item |

## 3. How data reaches a download

```text
TMDB / TVMaze / local catalog
    → title metadata + typed catalog identity
    → provider mapping (TMDB / IMDb + movie/series + episode)
    → provider adapter checks offline capability
    → freshly resolved, verified download descriptor
    → manifest/media probe + rendition/audio/subtitle selection
    → persistent download request + selected tracks
    → Media3 transfer/index + durable offline media storage
    → completeness verification
    → local metadata + local player (network access disabled)
```

TMDB images and Watchmode “where to watch” results are not video download sources. Store enough title/poster/episode metadata locally so the Downloads screen never needs those services to open an existing item.

The first engineering gate is a **provider feasibility spike**: find real supported movie and episode descriptors; prove they identify the correct content, survive required request headers, and can be fully downloaded and played offline. If none of the current providers supports that workflow, the app needs a suitable provider integration before a download queue can deliver value. Do not ship an attractive button backed by a resolver that never produces an eligible source.

## 4. Eligibility and formats

| Provider output | User-facing state | Implementation |
|---|---|---|
| Offline-capable VOD HLS manifest | Download options available after probe | Media3 HLS downloader, selected variant/audio/subtitle tracks and referenced supported keys/segments |
| Offline-capable VOD DASH manifest | Download options available after probe | Media3 DASH downloader and selected representations |
| Verified progressive media file | Download options available after probe | Media3 progressive downloader initially; provider must demonstrate stable range/content identity behavior |
| Embed-only / arbitrary HTML | “This source supports streaming only” | No WebView caching or HTML download masquerading as a film |
| Direct URL with unclear identity or offline capability | “Downloads aren't available from this source yet” | Adapter integration required; a `.mp4` suffix alone is not sufficient |
| Live/unbounded playlist | “Live video can't be downloaded as a complete title” | Out of first-release scope |
| DRM requiring an unavailable offline license | “This source doesn't support offline playback” | Offline-license workflow is a separate integration |
| Expired or near-expiry descriptor | “Refreshing download source…” | Refresh descriptor and compare content identity before transfer |

Multiple sources should be evaluated within a bounded eligibility lookup. A title with several sources should not be declared streaming-only just because the first is an embed. Prefer a known offline-capable source; do not fan out to every provider on every details-screen render.

## 5. Viewer experience

### Movie and episode details

- Details show `Check download availability` until capability is known, then `Download` or a readable unavailable reason. Checking is user-triggered initially to avoid extra data on every detail visit.
- An episode gets its own action; never treat an entire series as one file. Season selection does not silently change a queued episode.
- The download sheet shows title/episode, actual available quality choices, audio language, subtitles, estimated size, available storage and network policy.
- Quality labels are derived from the manifest. If only 720p exists, do not offer fictitious 360p/480p/1080p choices.
- Proposed default: Balanced, preferably a real rendition up to 720p, with audio+video total estimate. Data Saver prefers a real lower rendition. Show a missing lower rendition explicitly rather than claiming savings.
- The title changes to `Queued`, then a small progress control. Repeated taps open the existing item instead of starting duplicates.

### Library → Downloads

- Separate `Ready` and `In progress`; group episodes under their series.
- Each item shows title/episode, selected quality, bytes downloaded/size, status or exact waiting reason, and expiry if applicable.
- Controls: Pause, Resume, Retry, Cancel for incomplete items; Play and Delete for ready items. Cancel deletes partial content after transfer shutdown; Pause retains it.
- Show total download storage and an easy storage-settings link. Deleting one episode does not delete the series' other downloads.
- Offline Home/Library can open Downloads without a mandatory catalog refresh, assistant request, provider lookup or ad request.
- No automatic deletion after viewing by default. Any future cleanup preference needs explicit viewer choice.

### Background notification

One download notification shows active title, real progress or indeterminate bytes, waiting reason and Pause/Cancel controls. Completion opens that title in Downloads; failed-download notification opens its reason and Retry. Notifications are separate from promotional notifications.

## 6. Network and data-cost policy

| Condition | Default |
|---|---|
| Validated, unmetered Wi-Fi/Ethernet | Eligible queued items may start |
| Metered Wi-Fi/mobile hotspot | Wait for unmetered network unless this item has an explicit data override |
| Cellular | Same metered rule; show estimated bytes before override |
| Roaming | Wait unless separately opted in; an ordinary cellular override does not grant roaming |
| Captive portal / no usable route | Keep queue; display sign-in/reconnect reason |
| Wi-Fi/fibre → mobile during download | Pause promptly before continuing paid-data transfers; one shared coordinator owns route transitions |
| Provider unavailable while other services work | Provider-specific retry reason, not “your phone is offline” |

Fibre is the upstream connection, not a guarantee of speed or free data. Device transport, metering and actual endpoint reachability matter. Use Android network requirements plus application-level policy for per-item consent and roaming; test how Media3's selected version applies requirements to active transfers.

One active download is the initial default. Playback gets priority: pause background transfers while streaming on a metered connection; on unmetered connections, start conservatively and measure buffering before allowing concurrency. No speculative next-episode downloading in the first release.

For a known aggregate bitrate: `estimated bytes = duration seconds × aggregate bits/second ÷ 8`. For example, a two-hour title at 1.5 Mbit/s is roughly **1.35 GB decimal** before overhead. Audio, subtitles, manifest/segment overhead and variable bitrate affect reality. Show approximate ranges when appropriate, and never equate resolution alone with exact size. Unknown size means indeterminate progress, not 0% forever or a fabricated number.

## 7. Architecture and ownership

| Component | Responsibility |
|---|---|
| `ProviderDownloadAdapter` | Provider-specific eligibility, content mapping, fresh descriptors and supported refresh |
| `DownloadPlanner` | Pure eligibility/network/storage decisions and truthful unavailable/waiting reasons |
| `DownloadRepository` | Owner-scoped domain records, idempotent enqueue, observe list, pause/resume/cancel/remove |
| Media3 `DownloadManager`, `DownloadIndex`, `DownloadService` | Actual persisted transfers, download requirements and notifications; distinct from Android APK `DownloadManager` |
| `DownloadCoordinator` | Reconcile Room intent with Media3 engine state, serialize transitions, handle current owner and network policy |
| Dedicated singleton download cache/database provider | Durable media storage and engine index, separate from HTTP/Coil/player-temp caches |
| `DownloadsViewModel` / `DownloadsScreen` | Reactive state and controls; never own the transfer lifetime |
| `OfflinePlaybackRepository` + local player route | Resolve a completed owned item locally and construct Media3 offline media item |

Use one Media3 engine for supported formats initially. Add a custom progressive transfer engine only if provider fixtures demonstrate a concrete need; two independent engines double reconciliation complexity. Media3 version-specific APIs, service permissions and Android background limits must be verified during implementation.

Long-running transfers belong to the supported foreground download service with Android's required service declaration, foreground-service/data-sync permissions and notification channel. WorkManager handles bounded deferred resolution/reconciliation/maintenance, not an unbounded homegrown segment loop. User-initiated transfer behavior must be verified on Android 14/15 and background-restricted devices.

### Proposed durable metadata

`offline_downloads` (new Room entity/table):

| Fields | Purpose |
|---|---|
| `downloadId`, `ownerId`, `requestGeneration` | Stable operation and immutable ownership; late callbacks cannot mutate a replacement item |
| `catalogNamespace`, `catalogId`, `mediaType`, `season`, `episode` | Correct movie/series/episode identity; use non-null sentinels for movie season/episode so uniqueness is reliable |
| `title`, `episodeTitle`, `posterLocalPath`, `durationMs` | Offline display snapshot |
| `providerId`, `providerContentId`, `renditionId`, selected track keys | Provider/content/rendition continuity during refresh and playback |
| `engineRequestId`, `requestedAction`, `displayState`, `reasonCode` | Reconciliation contract; engine owns actual transfer state, Room owns viewer intent/metadata |
| `bytesDownloaded: Long`, `totalBytes: Long?`, `estimatedBytes: Long?` | Exact counters separate from estimates; update UI at bounded cadence |
| `contentValidator`, `descriptorExpiresAtMs`, `offlineExpiresAtMs` | Refresh/integrity/retention; URL expiry and offline viewing expiry are different |
| `attemptCount`, `nextRetryAtMs`, `createdAtMs`, `completedAtMs` | Finite retry and lifecycle evidence |

Unique active request identity: `(ownerId, catalogNamespace, catalogId, mediaType, season, episode)`. A different quality uses an explicit replace flow for the first release, not two silent copies.

Store sensitive descriptor material only where required by the local engine, using a documented local credential-storage approach. Do not log signed URLs/headers or include credentials in filenames/notifications. Normalize expiry units per provider; current optional HTML `expiresAt` extraction does not establish a dependable contract.

Do not inherit blank-owner legacy-claim behavior for downloads. Proposed first release: signed-in users with explicitly captured owners; completed content is visible only to its owner. If guest downloads are desired later, use a separate stable guest owner and an explicit import policy.

## 8. Transfer state, resume and recovery

```text
Queued → Resolving → Downloading → Verifying → Complete
             ↘ WaitingForNetwork / WaitingForUnmetered / WaitingForStorage
Downloading ↔ PausedByUser
Downloading → RefreshingSource → Downloading
Any incomplete state → Failed(reason, retryable) / Cancelling → Cancelled
Complete → Removing → Removed
```

“Queued” and “Paused” are not interchangeable. A user pause survives a network return, reboot, process recreation and source refresh. Removing/cancelling must win over late completion callbacks.

- Resolve URLs just in time, not hours earlier when adding a season queue.
- On supported descriptor expiry, refresh once per attempt and verify the same provider content/episode/rendition identity. A deterministic denial becomes a readable failure; do not silently switch unrelated content/providers.
- Resume progressive content only under the engine/provider's verified range/validator contract. A full `200` response after a partial range request restarts safely; never append it as if it were `206`.
- Changed media identity/strong validator means incompatible partial content is discarded with an explicit restart state. A `416` is not automatic completion.
- HLS/DASH resume uses the persistent engine index and cached segment coverage. A rotated manifest cannot silently mix different content generations.
- Transient DNS/timeouts/5xx get bounded exponential backoff with jitter; honour rate limits. Proposed default is three foreground transfer retry rounds before manual Retry, in addition to separately measured engine retries.
- HTML, captcha pages and ad media are never valid download bodies. Validate content type/signature/manifest structure and content identity. A provider requiring an unsupported interactive verification flow is unavailable for background downloading.
- On process start, reconcile Room metadata, engine records and disk/index state. Do not mark complete from a filename or nonzero byte count.

## 9. Storage, backup and offline playback

- Store media in a dedicated durable app-owned location outside `cacheDir`. App-internal storage is the simplest first-release default; assess app-specific external storage later with removal/unmount handling. Display available space for the actual selected location.
- Exclude downloaded media, partial media and short-lived descriptor credentials from Android backup/device transfer. Backed-up metadata alone must not claim missing files are ready after restore.
- Use a non-evicting download cache; explicit user deletion or a separately approved retention policy owns removal. Existing cache repair must not reach this directory or its index.
- Reserve queued-item space conservatively and leave headroom; recheck while writing. Unknown total sizes cannot be declared guaranteed to fit.
- Refuse to delete active readers/writers until coordinated shutdown. Reconcile orphaned content without touching another owner's data.
- Local playback uses a download ID and selected offline tracks, not the online provider URL. Configure the offline data source to fail clearly on a missing segment rather than silently consuming mobile data.
- A completed, owned, unexpired download must open without online Firebase refresh, `PlaybackAccess` internet validation, TMDB, provider resolution or a successful AdMob request. Any future paid offline entitlement needs a defined cached entitlement/expiry policy; do not retrofit it implicitly.
- Preserve episode-specific local playback progress; synchronize later under the correct owner. An offline player must not write its timestamp into another episode's online history.

## 10. Delivery and acceptance

| Stage | Deliverable | Exit criterion |
|---|---|---|
| A. Source feasibility | Provider descriptors + movie/episode fixtures + HLS/DASH/progressive assessment | Correct-content complete offline download demonstrated on at least one supported real source; identify formats actually available |
| B. Engine and persistence | Media3 service/index/storage, Room 6→7 migration, owner identity, reconciliation | Single download survives pause, process kill and restart without duplicate or false completion |
| C. Product surfaces | Details/episode sheet, network/storage choices, Library Downloads, notification controls | User can understand every waiting/failure state and control the same operation from UI/notification |
| D. Offline player | Local route, strict offline data source, metadata snapshot and progress | Airplane-mode start/seek near beginning and end, audio/subtitles and correct episode; zero fallback network fetches |
| E. Reliability pilot | Metered handover, source expiry, disk-full, changed content, account switch and old-schema upgrades | No unapproved mobile transfer, mixed media, wrong-owner access, data loss or false “Downloaded” state |
| F. Expansion | Season selection/batch queue, more adapters, optional watched-item cleanup | Core source success, recovery rate, storage cost and support burden measured before expansion |

Specific fault tests include: metered hotspot masquerading as Wi-Fi; fibre Wi-Fi without upstream; mobile handover during segment transfer; roaming; cancel while descriptor refresh completes; resume returning HTML/403/200/416; changed ETag; adaptive manifest missing final segment; low disk mid-transfer; restored DB with no files; removed external storage; switching accounts during enqueue; expired descriptor but valid completed local media; cache repair while an offline title plays; repeated notification/UI taps.

Implementation proceeds from source feasibility. A download button is useful only after the app can deliver the complete right title offline.
