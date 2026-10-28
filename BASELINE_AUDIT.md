# Milestone 0 — baseline audit

Date: 2026-10-23. Status: in progress; build verification blocked by symbol-processing timeouts.

## Firebase CLI

- `firebase --version`: 15.28.1.
- `firebase login:list`: an authenticated login is available.
- `firebase projects:list --json`: succeeded; `vista-cinema` is accessible.
- `app/google-services.json` identifies project `vista-cinema`, project number `1060207808425`, Android package `com.example.vista`.
- `.firebaserc` now selects `vista-cinema` as the default project. Commands can also pass `--project vista-cinema` explicitly.
- Firebase CLI is the Firebase project/authentication/deployment workflow. Cloudflare remains the planned AI runtime; these are separate deployment targets.
- No Firebase deployment or broadcast was performed. FCM sending must use an actually supported API/Admin backend; a Firebase CLI login is not proof that a `messaging:send` command exists.

## Architecture reviewed

| Area | Existing implementation | Findings |
| --- | --- | --- |
| Build | AGP 8.6.0, Kotlin/Compose plugin 2.0.0, Gradle wrapper 8.7, Java target 17, KSP, Hilt | Baseline build initially fails in AI ViewModel Hilt processing. |
| Android UI | MainActivity, Compose Material 3, navigation graph, Home, shared media card, theme/type/color | Featured carousel, loading skeletons and transitions already exist. AI draft had invalid imports, layout scope and mutable-state usage. |
| Data | Catalog/History/Watchlist interfaces, local repositories, Room entities and migrations, DataStore | History and watchlist are separate repositories, not CatalogRepository methods. |
| Profiles | Firebase Auth plus Firestore `viewers/{uid}` | Watchlist/history tables have no profile UID column; account isolation needs explicit migration/design before per-profile personalization. |
| Playback | StreamResolver, VidsrcPlaybackRepository, PlayerScreen/ViewModel, recovery/cache/session/health managers | Required headers and source expiry are not applied by the reviewed direct-player construction. Root cause of original cache incident is unconfirmed. |
| Persistence | Room v5, migrations 1→2→3→4→5 | No database schema change in this stabilization pass. Existing migration tests require a device/emulator and need review of actual Room-open assertions and legacy mediaType normalization. |
| Firebase | FirebaseManager, VistaMessagingService, application topic subscription, google-services configuration | Project verified through Firebase CLI. Global and market audiences overlap. |
| Release | Gradle development version 1.0.9/code 16, deployment manifests | Draft manifests advertised an unverified 1.0.9 download; restored local published-manifest baseline to 1.0.8/code 15. No remote publication performed. |

## Stabilization changes

- Removed AI provider account/token BuildConfig fields from Android.
- Replaced invalid direct-provider recommendation endpoints and nonexistent domain/cache/rating methods in the earlier AI draft with an explicit unavailable repository boundary. No fabricated recommendations or fabricated success are returned.
- Removed the redundant AiModule: AiRepository uses constructor injection.
- Repaired ViewModel injection and exposed lifecycle-observed UI state with a single in-flight request guard and cancellation propagation.
- Repaired chat layout/input APIs and Home FAB alignment/imports; assistant opens in a dismissible full-width dialog.
- Preserved draft preference keys so existing stored preferences are not deleted.

This is baseline stabilization, not completion of the backend, grounded assistant, recommendations, voice, or the playback repair milestone.

## Verification evidence

1. `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --console=plain`
   - Initial attempt reached KSP and reported the AI ViewModel constructor injection error.
   - After stabilization edits, the next attempt exceeded 240 seconds at `:app:kspDebugKotlin` without a final compiler result.
2. Attempt to reduce worker contention initially lost an unquoted dotted Gradle property to PowerShell argument parsing. No source diagnosis is inferred from that command-line error.
3. `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --console=plain --no-daemon --max-workers=2 "-Pkotlin.compiler.execution.strategy=in-process"`
   - Exceeded 600 seconds at `:app:kspDebugKotlin`; no compile/test/lint pass established.
4. `gradlew --status` also timed out. Process inspection returned Java processes but did not finish within the terminal deadline. Do not terminate unrelated Java/IDE processes without identifying ownership.
5. Existing reports under `app/build/reports/tests/` predate this verification and are not counted as evidence for these changes.

## Remaining audit and verification work

Complete review of the remaining screens/ViewModels, API DTOs/repositories, resources, backup/network policy, TV implementation, worker lifecycle, schemas and test fixtures. Recover a responsive build environment, rerun the quoted baseline command and address any further diagnostics. Run migration tests on a device/emulator. Define backend tool contracts and provider authorization evidence before implementing live AI actions.

The required `bd prime` command is unavailable in this shell; Beads tracking could not be initialized. This document records audit evidence, not a substitute task tracker.
