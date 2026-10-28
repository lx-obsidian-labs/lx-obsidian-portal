# Vista Cinema 1.0.9 — implementation roadmap

Status: planning baseline, not a release-readiness report.

## Goal

Upgrade the existing Android app with a polished adaptive interface, five primary destinations, a grounded AI assistant, user-activated voice, explainable taste recommendations, spoiler-aware movie companionship, and evidence-based playback recovery. Preserve profiles, watch lists and viewing history throughout upgrades.

## Current architecture and evidence

Reviewed areas include Gradle dependencies, MainActivity/navigation, HomeScreen, shared media cards and theme, domain models, Room schema/migrations, DataStore preferences, AI drafts, stream resolution, player UI/ViewModel, cache management, session refresh and playback recovery.

- Kotlin, Jetpack Compose/Material 3, Navigation Compose, Hilt/KSP and coroutines/Flow.
- Room database version 5, exported schemas, migrations 1→2→3→4→5; DataStore settings. Firebase provides identity and other services.
- Retrofit/OkHttp catalog integrations include TMDB, TVMaze and Watchmode. Catalog metadata is not proof that playback is available or authorized.
- Media3/ExoPlayer handles direct streams; WebView handles embeds. These paths have different capabilities and error visibility.
- Featured content, shimmer loading, transitions, offline components and history-based suggestions already have foundations to improve.
- Current primary destinations are Home, Discover, Search, Library and Settings. The requested destinations are Home, Discover, AI Assistant, Watch List and Profile.
- Earlier AI edits are incomplete drafts: references do not match existing models/repository contracts, dependency injection has conflicting construction, and UI state/layout errors are visible. A passing build has not been established.
- The draft builds an AI provider token into BuildConfig and calls Cloudflare directly. Replace this with an authenticated backend before shipping.
- Release manifests were advanced to 1.0.9 before a verified artifact existed. Release metadata must be reconciled during stabilization and published only against a tested artifact.
- No successful Firebase broadcast is recorded in this session. A sender script is not evidence of delivery; its current global-plus-market sends would duplicate announcements.

The entire source/resource/configuration inventory and baseline verification remain part of milestone 0. Generated build outputs and APK binaries do not constitute source review.

## Milestone 0 — complete audit and establish a reliable baseline

### Work

- Finish inventory and review of application source, resources, manifest, API contracts, auth/profile persistence, repositories, TV entry points, tests, exported schemas and deployment configuration.
- Document ownership and data flow: UI → ViewModel → repository → local storage/API; identify actual backend deployment location and supported provider authorization contracts.
- Capture compile, unit-test and lint baseline, distinguishing existing failures from the incomplete AI draft.
- Reconcile draft AI interfaces with real domain types and DI; establish a buildable starting point before feature expansion.
- Record a feature/dependency map, migration coverage and reproducible playback failure procedure, including app/WebView versions and source type.
- Reconcile premature release metadata without publishing an unavailable APK or unverified claims.

### Acceptance

A complete audit report, reproducible build/test commands, explicit failure list and agreed API/data contracts exist. Unverified functionality is labeled accurately.

## Milestone 1 — secure backend and data-preservation contracts

### Work

- Create an authenticated Cloudflare backend for assistant requests. Validate identity, profile access, request schema, rate limits and content permissions server-side.
- Keep AI/provider credentials in backend secret bindings. Android contains only the backend URL and obtains short-lived identity credentials through the established auth flow.
- Separate deployment credentials, R2 storage settings and runtime AI secrets; provide an importable environment template with exact binding names. Do not assume an R2-scoped token permits Workers AI.
- Define typed assistant tools, initially searchCatalog, getTitleMetadata, checkAvailability, getRecommendations, addToWatchlist, removeFromWatchlist, openTitle and requestPlayback.
- Validate arguments and authorization in deterministic code. Models propose calls; app/backend functions enforce them. Playback requires a current authorized source, not a model-generated URL.
- Define verified result objects with source, timestamp, media type and identifier. Unknown/stale availability remains explicitly unverified.
- Establish profile isolation and additive storage changes. Verify migration paths from every supported shipped schema; retain existing watchlist/history keys and resume positions.

### Acceptance

Provider keys are absent from the APK and client logs. Invalid tokens, unauthorized profile access, unknown tools and malformed arguments are rejected. Upgrade fixtures preserve existing data.

## Milestone 2 — diagnose playback and implement bounded recovery

### Evidence to investigate

- PlayerScreen creates direct playback using only MediaItem.fromUri(source.url); StreamSource.headers and drmInfo are not applied there. StreamResolver explicitly states required headers may prevent CDN 403 responses. This is a concrete defect candidate, not a confirmed explanation of the cache-clearing incident.
- Resolved sources expose expiresAt, but the reviewed player does not refresh an expired source before playback.
- Recovery classifies errors mainly by message text, globally evicts HTTP cache and can repeatedly alternate sources without a total retry cap or attempted-source set.
- SessionManager reports refresh success for a signed-out non-guest. Firebase refresh also does not by itself renew a separate provider session.
- The reviewed ExoPlayer construction does not connect to player_temp or the shared OkHttp cache. Clearing those stores cannot be presumed to repair the failing playback path.
- WebView persistent state is separate from the app's HTTP cache. Establish whether the original failure occurred in WebView or Media3 before choosing invalidation.

### Work

1. Reproduce failure and collect redacted structured causes, HTTP status, source identity, expiry, player/WebView state and refresh results.
2. Preserve current position and stop the affected playback operation before repair; serialize recovery so only one attempt runs.
3. Check connectivity and current app/provider authorization. Offline recovery waits for connectivity; a permission denial stops with an actionable message.
4. Refresh the relevant session once when evidence indicates expiry. Obtain a fresh authorized playback descriptor when needed.
5. Invalidate only the affected app-owned temporary entry/session. Preserve databases, profile settings, watch lists, history and unrelated active playback.
6. Recreate/reprepare the actual player with required authorized headers/DRM configuration and restore position where supported.
7. Apply a global recovery budget: at most two automatic recovery attempts, bounded delay and no cycling through previously failed sources. Alternatives must be authorized and verified.
8. Add Settings → Playback → Repair playback using the same coordinator, with progress and a truthful result.

### Acceptance

A written root-cause report distinguishes confirmed evidence from hypotheses. Tests reproduce expired sessions, required headers, stale descriptors, network loss, denial and retry exhaustion. Repair preserves user data and demonstrably restarts the real player. Authorization is never bypassed.

## Milestone 3 — premium UI and persistent primary navigation

### Work

- Implement Home, Discover, AI Assistant, Watch List and Profile with stable per-tab back stacks and state restoration. Keep Search accessible from browsing; Settings from Profile.
- Keep primary navigation persistent across primary destinations and their browsing flows; provide an immersive player with predictable return navigation.
- Refine the featured-film hero, continue watching, recommendation rows, poster hierarchy, spacing, typography and theme-aware colors using existing Compose foundations.
- Standardize skeleton, empty, partial-load, retry and offline states. Use restrained transitions respecting reduced animation settings.
- Introduce adaptive phone/tablet layouts and TV-appropriate focus/navigation. Validate system insets, large fonts, contrast, TalkBack semantics and 48dp touch targets.
- Use lazy content, bounded image sizes and narrowly scoped state updates to protect mid-range device performance.

### Acceptance

All five destinations work with back navigation and process recreation. Core screens remain usable with large text and TalkBack, on compact/expanded displays and through network failures. Representative device traces show no material performance regression.

## Milestone 4 — grounded natural-language assistant

### Work

- Build a dedicated assistant destination with observable conversation state, cancellation, result cards, retry and useful suggested prompts.
- Route requests through typed tools and validate every tool result. Resolve ambiguous title/year/media type before performing an action.
- Ground availability statements in checkAvailability results, distinct from metadata search results. Give an explicit unknown/unavailable answer when evidence is missing.
- Enforce current profile, guest limits, content settings and playback authorization for actions, including actions requested in natural language.
- Treat metadata and retrieved text as data rather than instructions. Reject arbitrary URLs, code execution and unregistered actions.

### Acceptance

Requests such as “find a short comedy” and “save this film” use the defined functions. Fabricated title IDs, fake availability, malicious metadata and unauthorized actions fail safely and visibly.

## Milestone 5 — tap-to-talk voice interaction

### Work

- Add explicit microphone activation with runtime permission handling and platform speech-recognition capability checks.
- Model idle, listening, transcribing, executing, speaking, interrupted and error states with visible indicators and editable transcripts.
- Execute voice requests through the same assistant tool dispatcher and permission checks as typed requests.
- Add spoken responses with TextToSpeech, audio-focus handling, stop-speaking and tap-to-interrupt support. Stop TTS before starting a new capture to prevent feedback.
- Cancel microphone capture when the user stops it or leaves the active flow; handle calls, rotation, backgrounding, recognition timeout and unavailable speech services.
- Provide typed fallback when speech recognition or its network service is unavailable. No continuous background listening or wake-word service in this release.

### Acceptance

Real-device checks cover permission denial, silent input, noisy input, service unavailability, interruptions and repeated activation. Indicators match actual microphone state; leaving the flow releases recognition/audio resources.

## Milestone 6 — taste-based recommendations and reset

### Work

- Build a profile-scoped taste model using actual watched history, verified genres, completion signals and explicit likes/dislikes/preferences. Catalog star ratings are not user ratings.
- Offer “For your taste,” mood-based picks, similar titles, hidden gems and “what should I watch next?” with explanations tied to real signals.
- Let users choose genres, language, content type, mood and available time; explicit preferences outrank inferred taste.
- Rank verified catalog candidates and attach availability separately. Provide a sensible cold-start experience and cached/local fallback.
- Add preference editing, personalization opt-out and Reset personalization. Reset clears derived signals, feedback and personalization caches locally/remotely as applicable while retaining profiles, watch lists and viewing history.
- Use a reset timestamp/version so retained pre-reset history does not immediately recreate the old inferred profile unless the user chooses to reuse it.

### Acceptance

Recommendations change predictably with real inputs, respect profile/content restrictions and explain their basis. Reset and opt-out work across local/backend state without deleting viewing history or saved titles.

## Milestone 7 — spoiler-aware movie companion

### Work

- Add companion access from title details and playback using verified title/cast/crew metadata and authorized contextual material.
- Default to spoiler-safe answers. Support explicit spoiler permission and episode/progress boundaries only where the supplied metadata supports them.
- Retrieve/filter spoiler-sensitive material before generation. Do not assume a general plot synopsis or the model's prior knowledge is safe at a given playback timestamp.
- Show source attribution and acknowledge missing evidence. Do not invent scene-level explanations when no authorized timestamped context exists.
- Support typed and user-activated voice questions using the same grounded assistant pipeline.

### Acceptance

Cast answers match verified credits. Spoiler fixtures do not expose later episodes/endings with controls enabled. Unsupported questions produce an evidence-limited response rather than an invented answer.

## Milestone 8 — integration, resilience and release verification

### Work

- Verify migration paths, profile boundaries, saved lists and resume history with existing-install fixtures.
- Test playback startup, seek, resume, source refresh, network interruption, manual repair, decoder/DRM failure and recovery budgets on direct and embed paths.
- Test voice lifecycle, microphone permissions, TTS interruption and typed fallback on devices.
- Test assistant tool schemas, authorization, prompt injection, availability grounding, spoilers and personalization reset with deterministic fixtures.
- Run unit, instrumentation, UI and backend tests plus compile/lint checks; measure startup, scrolling, image memory and playback on representative mid-range hardware.
- Verify signed APK upgrade and backend configuration in staging. Publish the version manifest only when its download artifact exists and passes verification.
- Send one accurate FCM announcement to the intended audience after validating its timing/content. Use the global topic once for all subscribers; do not also broadcast to overlapping market topics. Record Firebase message ID; API acceptance is not proof of every-device delivery.
- Release messaging describes measured improvements and actual availability, without guaranteeing universal smooth playback.

### Acceptance

A release evidence report records commands, results, device coverage, remaining issues and the verified artifact/backend versions. Critical playback, voice, data-preservation and AI-grounding checks pass before release publication.

## Delivery order

Milestone 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8.

Security/data contracts and playback reliability are foundations. Each milestone ends with demonstrable acceptance evidence before the next feature layer is considered complete.
