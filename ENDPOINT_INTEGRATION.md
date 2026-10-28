# Vista Cinema - Endpoint Integration Guide

How the app talks to its content and streaming providers, and where to change things.

## Overview

The app uses a repository pattern. UI (Compose) -> ViewModel -> Repository -> Remote clients
(Retrofit) or local stores (Room / DataStore). Repositories:

| Repository | Backing |
|---|---|
| `TmdbCatalogRepository` | TMDB API |
| `TvmazeCatalogRepository` | TVMaze API |
| `LocalWatchlistRepository` | Room |
| `LocalHistoryRepository` | Room |
| `LocalSettingsRepository` | DataStore |
| Playback sources (`data/remote/playback/`) | Multiple embed providers |

## Remote APIs

| Provider | Base URL | Used for |
|---|---|---|
| **TMDB** | `https://api.themoviedb.org/3/` | Catalog, search, credits, recommendations, similar, genres, trending, season episodes, external ids |
| **Watchmode** | `https://api.watchmode.com/v1/` | "Where to Watch": streaming sources/availability + title details (`title/{id}/sources/`, `title/{id}/details/`, `search/`) |
| **TVMaze** | `https://api.tvmaze.com/` | Series search + schedule fallback (`schedule`, `shows`, `search/shows`, `singlesearch/shows`, `shows/{id}/episodes`, `people/{id}`) |
| **Shoofaflam** | `https://shoofaflam.tv/api/` | Localized/large title listings (`titles.json`, `platforms.json`, `search/{letter}.json`) |
| **IMDb helper** | `https://imdb.iamidiotareyoutoo.com/` | IMDb id resolution for playback source mapping |
| **TMDB images** | `https://image.tmdb.org/t/p/` | Posters/backdrops (w500 and up, sized via Coil request) |
| **YouTube** | `https://www.youtube.com/embed/...` | Trailer playback in `TrailerPlayerScreen` |

### Streaming sources

The player tries embed sources in order and falls through on failure
(`data/remote/playback/`):

- `https://vidsrc.to/embed/movie/{id}`, `/embed/tv/{id}/{s}/{e}`
- `https://vidsrc.pro/embed/...` (same shapes)
- `https://vidsrc.me/embed/movie?{id}`, `/embed/tv?{id}/{s}/{e}`
- `https://www.2embed.cc/embed/{id}`, `/embedtv/{id}?s=&e=`
- `https://multiembed.mov/?video_id={id}` (+ `&tmdb=1` or IMDb id variants)

Embed URLs are rendered in the player's WebView overlay; the built-in ad blocker filters
tracker scripts.

## Authentication

- **TMDB** - Bearer token (`TMDB_ACCESS_TOKEN`) injected as the `Authorization` header.
- **Watchmode** - `apiKey` query parameter (`WATCHMODE_API_KEY`).
- Keys are compiled from `gradle.properties` into `BuildConfig` by `app/build.gradle.kts`
  (fail-fast if missing). Copy `gradle.properties.example` to `gradle.properties` to set them;
  `gradle.properties` is git-ignored.

## Configuration (DI)

- `di/NetworkModule.kt` - two OkHttp clients (one with logging), both sharing a **50 MB HTTP
  cache** at `context.cacheDir/http_cache`. Retrofit instances for TMDB, TVMaze, Watchmode,
  Shoofaflam and IMDb.
- `di/ImageModule.kt` + `ui/components/CoilImage.kt` - Coil with SVG support, disk cache factor
  0.10, and 1024 px max request size.

## In-memory caching

- **TVMaze series search** - `TvmazeCatalogRepository` caches results in a
  `ConcurrentHashMap` for **5 minutes**; the duplicated Home "trending" call is served from
  cache.
- **TMDB resolve** - `TmdbCatalogRepository` caches IMDb->TMDB id lookups (failed lookups use a
  `0` sentinel) so playback source resolution does not repeat network hits per session.

## In-app update manifest

- `util/AppUpdateManager.kt` polls
  `https://lxobsidianportal.co.za/vista-version.json` (base URL const in the file).
- Throttled to one check per **30 s**; a manual Settings - About "Check for Updates" forces one.
- The manifest drives download + install; see `DEPLOYMENT.md`.

## First-party analytics endpoints

The app POSTs no-PII usage events to its own Cloudflare site (`data/analytics/VistaAnalytics.kt`
batches in memory; base URL + optional shared key come from `BuildConfig`):

- **`POST /api/collect`** - JSON body `{ "event", "label", "v", "ts_ms", "guest", "uuid" }` on
  the site origin. Server allowlist; batched writes to D1; expects `X-Vista-Analytics-Key` to
  match the `VISTA_ANALYTICS_KEY` secret. Always answers `{ ok: true }` regardless (never
  confirms storage).
- **`GET /api/vista-stats?days=30`** - requires `X-Vista-Stats-Key` (or an admin JWT); returns
  per-event, per-country, per-day, per-app-version and per-guest-state aggregates for the last
  `N` days (default 30, max 365).

Implementation + provisioning live in `DEPLOYMENT.md` (First-party analytics section).

## Adding a new content API

1. Add a `@GET` interface under `data/remote/` (model DTOs in the same package).
2. Provide its Retrofit client in `di/NetworkModule.kt`.
3. Wire it through a repository in `data/repository/` (or extend `TmdbCatalogRepository`).
4. Map DTO -> domain model (`domain/model/Media.kt`) and surface it in a ViewModel.

## Testing

- Unit tests live under `app/src/test/` (JUnit4, coroutines-test, MockK - run with
  `.\gradlew.bat :app:testDebugUnitTest`).
- Instrumented/Compose UI tests under `app/src/androidTest/`.
- No network mock server is wired up yet; repository tests exercise the local (Room/DataStore)
  repos directly.