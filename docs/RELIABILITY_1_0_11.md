# 1.0.11: provider ads and empty catalog investigation

## Findings and evidence

These are source-confirmed failure paths, not a claim that every affected user has the same cause. No new affected-device trace was captured in this session.

### Why some users see WebView ads

- Playback prefers extracted direct media; when extraction is unsuccessful, or a user selects an embedded source, the app renders the provider's JavaScript player. Ads inside that page belong to the provider's embed content.
- `PlayerScreen.kt` previously injected cleanup only in `onPageCommitVisible`/`onPageFinished`. Provider scripts could execute earlier. `evaluateJavascript` targeted the top-level document, not cross-origin nested player documents.
- `EmbedAdPolicy.kt` matched known ad hosts and labelled containers. Pop-up refusal and top-level navigation restrictions do not remove an ad painted inside the current document or its child frame. The original observer watched child-list changes only, missing later `src`/class/id changes and mutations within blank child documents.
- `docs/NEXT_VERSION_EXPERIENCE.md` records a previous device reproduction of a same-origin blank iframe containing a Cloudpiko fake-robot notification template. That historical observation is distinct from this source review.
- Provider source, WebView support, page timing and returned ad resources can produce different outcomes across devices. Country/cookie/ad-campaign differences are plausible but were not measured here.

### Why some users see empty movie data

- `TmdbCatalogRepository.getSection` converted every exception into `emptyList()`. DNS, timeout, HTTP auth/rate-limit/server errors and mapping errors therefore looked like an empty successful row. `getHomeSections` then omitted those rows. A functioning TVMaze series row can coexist with missing TMDB movie rows.
- The popular-series blend requested TMDB before TVMaze inside the same failure boundary; a TMDB exception skipped the fallback entirely.
- `TvmazeCatalogRepository.searchSeries` cached the empty list produced by a failed request for five minutes. Restored connectivity did not immediately trigger a real retry for that query.
- Most movie rows still depend on TMDB. The HTTP cache only helps when a cacheable successful response already exists; it cannot supply a first-install catalog after a service/network failure.
- `BUILD_REPORT.md` documents an older confirmed release problem: minification stripped reflectively parsed DTOs, causing empty metadata. Current release minification/shrinking remain disabled and DTO keep rules remain present. That historical cause must not be assumed for every current report.
- The configured NexStream endpoint is a **playback embed**, not a movie metadata API. Adding its key cannot populate empty TMDB catalog rows.

## Changes in this build

- Add AndroidX WebKit document-start cleanup in all frames of the embed WebView when the installed WebView supports it. Keep page-callback cleanup for older implementations.
- Observe document and same-origin child-document mutations, including relevant attribute changes. Document-start installation works before `documentElement` exists. Preserve labelled containers that contain real media/player iframes.
- Keep the existing request-host filter, popup refusal and top-level navigation policy. This is targeted filtering, not a guarantee that arbitrary new/same-origin provider ads will never appear.
- Throw safe, actionable `CatalogLoadException` messages for failed sections. Home retains successful rows and surfaces failure when no rows load; section paging receives failure rather than a false end-of-results page. Diagnostics use section, exception class and HTTP status, excluding credential-bearing URLs/bodies.
- Fetch TVMaze's series fallback even when the TMDB request fails. Do not cache failed TVMaze searches; return last-known data if present. Preserve coroutine cancellation on the modified paths.
- Isolate a failed popular-movie recommendation pool so healthy genre/related pools still contribute.
- Offer the optional NexStream movie embed after existing sources with a resolved numeric TMDB movie ID. Load the key from Gradle properties, environment or root `.env`; no key is stored in source documentation. Renewing this build-time key requires rebuilding.

## Verification

- Clean debug compilation and **33 focused JVM tests passed**, including six catalog fault-injection/recovery tests and a recommendation-pool failure test.
- Signed release assembly and release vital lint passed; package, version/code, launchers and production signing certificate verified. Artifact/hash are recorded in `BUILD_REPORT.md`.
- JVM ad-policy tests verify host/path filtering and legitimate-resource exclusions. They do not execute the cleanup script in an Android WebView. Document-start timing, cross-origin frame cleanup and popup behavior still require physical-device verification on both supported and fallback WebView versions.
- Live NexStream playback and provider key/domain/expiry acceptance were not verified. The provider's supplied key notice states 30-day validity.

## Evidence needed from an affected device

Record app version/code, Android and Android System WebView versions, whether the chosen source is direct or embedded, the provider label, and whether the issue occurs on both Wi-Fi and mobile data. Capture the catalog failure class/HTTP status and a screenshot of the error or overlay. Do not collect API keys, full signed/embed query URLs, cookies or viewing history. Reproduce on 1.0.11 before attributing a remaining report to an older fixed code path.
