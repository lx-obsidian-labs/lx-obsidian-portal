# Vista 1.0.10 — festive experience, suggestions and offline foundations

Status: **published 8 November 2026**, Android 1.0.10 / versionCode 17, optional update. Signed release, GitHub assets, Cloudflare production, website CI and live AI checks pass. Final physical-device offline/onboarding/upgrade coverage remains pending. See [publication evidence](PUBLIC_RELEASE_1_0_10.md).

## Published highlights

Your Smart TV responsiveness and Android phone/tablet/TV detection work is explicitly highlighted in the live website, in-app manifest changelog, public release notes/changelog, TV download description, GitHub Release and availability notification. Verified source behavior includes multi-factor TV detection and TV focus/D-pad scrolling; hardware-wide compatibility and phone/TV feature parity are not implied.

The public APK is 22,501,177 bytes; SHA-256 `31BADF75DFBA417D3D3871BB76084311AE293A06C468C5399BCB1EDC88DA5B1A`. Its certificate matches the documented production fingerprint. The website's first-party download route redirects to GitHub Release CDN assets and never proxies or stores APK bytes on Pages. Both stored asset digests and downloaded CDN bytes match the signed binary.

Availability push accepted once: `projects/vista-cinema/messages/-1671408473659384313`, topic `vista_updates`, `check_update=true`. The visible message includes Smart TV responsiveness and Android/TV detection. Delivery/display depends on device connectivity, permissions and notification preferences; FCM acceptance is not a device receipt.

## UI and missing-feature review

The source review identified concrete experience gaps: recommendation reasons were hidden; there was no direct negative-feedback action; downloads had no durable service/index or local playback route; native controls obscured embed players; seasonal content existed only as a small December daily-pick pool; and old release announcements remained in the inbox after upgrading.

This change addresses those flows rather than adding decorative screens without working actions:

| Area | Implemented source behavior |
|---|---|
| Festive Home | November 1–January 5 window; year-aware Christmas/New Year copy, local countdown, daily rotating suggestion, three curated collections, save/open actions and a hide-for-this-season preference; date refresh follows the foreground lifecycle |
| Suggestions | Multiple candidate pools, saved-title or meaningful-viewing seed, bounded genre/runtime hydration with public-metadata cache, explicit dislikes, genre-diversity penalty, short-film metadata validation, visible reasons and Not for me action |
| Downloads | Movie details, per-episode and player actions; media source lookup plus short-lived exact-title/episode player observations; native HLS/DASH/progressive preparation; quality preference and actual selected-quality label |
| Download operations | Media3 foreground service/index, one transfer, unmetered requirements, roaming guard, persistent item pause, resume, fresh-source retry, deletion confirmation and explicit waiting/error/unknown-size states |
| Offline library/player | Library entry, owned request metadata in the single Media3 index, isolated non-evicting cache keys/storage, strict cache-only player, local position checkpoints, account change guard and active-reader deletion protection |
| Announcements | Older 1.0.x release notifications/inbox entries retired after upgrading; 1.0.10 is not described as publicly available while manifest still points to 1.0.9 |
| Professional settings | Searchable directory, Downloads and AI & Voice pages, owned download/storage totals, default offline quality, genre/feedback controls, Android notification/app-permission links and seasonal visibility |
| Smarter assistant | Local typed action policy plus negotiated model actions, numbered prior-result references, own-library/resume lookup, exact-title comparison, download-review routes, execution receipts, progress/cancel/retry and transport cancellation |
| Accurate onboarding | Existing three-step tour retained; first guest/sign-in/profile setup goes through it, optional name/step restored on recreation, completion persisted before navigation, current feature limitations explained and replay available in About |
| Provider ads | Observed fake robot notification-ad template blocked without changing Vista's AdMob manager or placements |

## Research applied

- Android's [Downloading media guide](https://developer.android.com/media/media3/exoplayer/downloading-media): use DownloadService, persistent DownloadIndex, a non-evicting download cache, DownloadHelper for adaptive track selection and the request's MediaItem for offline selected-track playback.
- Reviewed the actual **Media3 1.3.1** [DownloadHelper](https://github.com/androidx/media/blob/1.3.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/offline/DownloadHelper.java), [DownloadManager](https://github.com/androidx/media/blob/1.3.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/offline/DownloadManager.java), and [DownloadService](https://github.com/androidx/media/blob/1.3.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/offline/DownloadService.java) APIs rather than copying examples from a newer incompatible release.
- Persistent per-item stop reasons preserve user pause across process death. Download progress requires periodic observation because listeners do not emit each byte update. Both are reflected in the implementation.
- Media storage is under `noBackupFilesDir`; the standalone engine index is excluded from cloud/device backup, so restored metadata cannot invent ready media. The app's Room version remains 6 because download state/metadata live in Media3's index, avoiding a competing state database.

## Deliberate first-release boundaries

- Download eligibility is best effort, not universal provider availability. Some embeds reveal no usable media URL, require cookies/interactive challenges, or expire before completion. These remain unavailable or failed with a reason; they are not reported downloaded.
- DownloadHelper selects compatible video/audio and available English text tracks; the initial UI offers quality preferences, not an invented list of confirmed variants or exact file sizes. Audio/subtitle selection UI, season batch queue and per-item cellular overrides remain follow-up work.
- Retry resolves a fresh source, removes incompatible partial content, then starts again. It does not append bytes from an unrelated refreshed URL. Resume retains the original descriptor and may fail after expiry; Refresh & retry is the explicit recovery path.
- Offline media stays on the device, isolated by locally retained owner identity. Cloud library sync, offline DRM licensing, billing entitlements and family profiles are separate features.
- A public fixture test establishes adaptive transfer/offline behavior under its test conditions. It cannot establish that every third-party provider supports downloading or every ISP is reachable.

## FCM execution

One data-only promotional greeting was accepted by FCM for topic `vista_updates`:

- Title: **Vista 1.0.10 is coming ✨**
- Body: “Wishing you a wonderful start to the festive season! We’re preparing Vista 1.0.10 with festive picks, better movie suggestions and downloads. Here’s to great stories and memorable movie nights. Stay tuned!”
- FCM message: `projects/vista-cinema/messages/4261578796388226545`
- Category `promotions`, no forced update, normal priority, 24-hour TTL. Current clients' service applies their notification preference. FCM acceptance does not prove delivery/display on every installation.

`node send_festive_fcm.mjs` validates; `--send` broadcasts. Do not rerun a broadcast as a deployment check.

## Verification record

Record final build, JVM, device, migration and offline-test results here after execution. Added regressions cover seasonal boundaries/year rollover, daily-pick exclusion, source eligibility/expiry, owner/episode identity, obsolete-release filtering and recommendation hydration/explicit feedback. A device test covers a public HLS fixture's complete download and end seek through a strict cache-only source.

Additional backend checks cover capability negotiation, bounded numbered references, legacy app compatibility and byte-bounded streamed request bodies. `node backend/probe-agentic.mjs` is an opt-in live NVIDIA contract probe using fictional profile/results; it does not send notifications or mutate app data.

The first device-test attempt exposed an instrumentation DI setup issue: a test-only entry point was absent from the production Hilt component. The access interface was moved into the app component so verification uses the real download singleton. This initial failure did not exercise or establish download success; the corrected test must be rerun.

### Verified 7 November 2026

- Latest `:app:assembleDebug`: **BUILD SUCCESSFUL**, 7m 19s.
- Latest `:app:testDebugUnitTest :app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, 3m 53s. Onboarding regressions verify durable completion before navigation, duplicate-tap protection, save failure and returning-viewer preservation.
- `node --test` in `backend`: **12 passed, 0 failed**.
- Actual NVIDIA probe passed all three prompts: `Download the second one` → download/selection 2; `Open AI and voice settings` → navigate/ai_voice; `Show my watchlist` → watchlist. This verifies the model contract, not a new production Worker deployment.
- `node --check send_fcm_cli.mjs`: passed. Sender guards available-now messaging against the real public version/hash/size; public manifest still returns 1.0.9 / 16.
- `git diff --check`: no whitespace errors; Git reports existing Windows line-ending normalization notices.
- Phone disconnected before the corrected adaptive-download test could be rerun. The user chose to finish the release plan first. Final device checks, production signing and actual publication are deferred and must not be represented as passed.
