# Stability research: best practices and their limits

Reviewed **29 October 2026**. This register supplements S01–S14 in [the commercial research](01-RESEARCH.md). Source IDs R01–R09 below distinguish retrieved material from engineering proposals. None establishes Vista's current measured stability.

## Retrieved primary sources

| ID | Source | Verified guidance | Application to Vista |
|---|---|---|---|
| R01 | [Android team's Now in Android architecture guide](https://raw.githubusercontent.com/android/nowinandroid/main/docs/ArchitectureLearningJourney.md) | Offline-first repositories, local reads as source of truth, unidirectional state flow, Room reconciliation and WorkManager-backed retry | Keep saved data readable during network failure; reconcile in a repository, not a composable; represent partial/error states explicitly |
| R02 | [Google Play Android vitals definitions](https://support.google.com/googleplay/android-developer/answer/9844486?hl=en) | Vitals excludes versions not installed through Play and uncertified device models; user-perceived crash/ANR rates are daily-user/device-normalized metrics | Do not infer sideload health from an empty Play dashboard, or compare its rates directly with Crashlytics session percentages |
| R03 | [Firebase Crashlytics crash-free metrics](https://firebase.google.com/docs/crashlytics/crash-free-metrics) | Crash-free users and sessions have different denominators; metrics count fatal events, not ANRs/nonfatals; opt-in collection affects coverage | Track sessions, installations, ANRs and operational failures separately; show telemetry coverage and raw counts |
| R04 | [OkHttp calls, official repository](https://raw.githubusercontent.com/square/okhttp/master/docs/features/calls.md) | Synchronous calls block their caller; calls can be explicitly cancelled; synchronous concurrency is the application's responsibility | Off-main execution alone is not cancellation or a concurrency budget; bind coroutine cancellation to `Call.cancel()` and bound actual calls |
| R05 | [OkHttp concurrency design](https://raw.githubusercontent.com/square/okhttp/master/docs/contribute/concurrency.md) | Threads/connections carry cost; pooling enables reuse; locks should not be held over blocking I/O/callbacks | Reuse configured clients/pools where appropriate and avoid slow operations in state locks or UI reducers |
| R06 | [AWS Builders' Library: timeouts, retries, backoff and jitter](https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/) | Connection and request deadlines, idempotency for side effects, bounded retries at one owning layer, jitter to prevent synchronized bursts | Define a complete user-operation budget; no multiplying client/Worker/provider retries; no blind payment or AI retry after an ambiguous timeout |
| R07 | [Google SRE: handling overload](https://sre.google/sre-book/handling-overload/) | Bounded work, per-customer limits, shedding optional requests and preserving critical work; retry budgets avoid cascading failure | Billing/access/export must remain available when AI/analytics is saturated; cap queues and provider concurrency before memory exhaustion |
| R08 | [Google SRE Workbook: canarying releases](https://sre.google/workbook/canarying-releases/) | Small reproducible releases, targeted exposure, attributable metrics, representative duration/volume, feature flags separate from binary releases | Separate APK availability from feature enablement; start with named testers and evidence, not a meaningless “1% of 22 users” rollout |
| R09 | [Android team's modularization guide](https://raw.githubusercontent.com/android/nowinandroid/main/docs/ModularizationLearningJourney.md) | Boundaries depend on project size and goals; excessive modularization is not a universal solution | Extract testable ownership/seams first; do not rewrite the app into dozens of modules as a stability project |

The OkHttp repository currently documents a newer major version than Vista's 4.12.0. Treat its cancellation/pooling concepts as design guidance; verify exact APIs and behaviour against the installed dependency before implementation. A sample architecture is a reference, not a mandate to copy its entire dependency graph.

## Measurement facts that change this plan

- R02 currently lists bad-behaviour thresholds of **1.09% user-perceived crash rate** and **0.47% user-perceived ANR rate**, with **8% per-device** thresholds. These are Play discoverability thresholds using Play's denominator, **not** Vista's target service levels or proof that a lower session rate is good enough.
- R02 defines slow initial display reference thresholds of 5 seconds cold, 2 seconds warm and 1 second hot. Initial display is not an interactive Home screen or first video frame. Vista measures all three separately.
- R03 defines a Crashlytics user as an installation, not a Firebase account. Its session begins at cold start or foreground after at least 30 minutes background. The 22-account inventory cannot supply a Crashlytics denominator.
- Fatal-free sessions can coexist with a stuck spinner, wrong-account history, duplicate payment, silent lost save or microphone that restarts. End-to-end success and safety invariants need separate tests.
- Zero crashes in 200 independent representative sessions does **not** demonstrate a population crash rate below 0.5% at 95% confidence. For zero events, the exact one-sided 95% upper bound is `1 - 0.05^(1/n)`: about 1.49% at n=200 and 0.498% at n=600. Repeated sessions on the same few devices are correlated; the formula is illustrative, not certification.

## Access limitations

Direct developer.android.com pages for ANRs, coroutine best practices, offline-first data, Compose saved state and Media3 player errors timed out, including retrying selected `?hl=en` variants. They are useful follow-up references, but their specific current text was not verified here. The guessed OkHttp website `/features/calls/` and a Firebase `monitor-stability` URL returned 404; R04 and R03 are the successfully retrieved replacements. A Kotlin cancellation page returned no usable text and the attempted GitHub guide path returned 404; no quote is attributed to those attempts.

Use Android's official samples plus the retrieved SDK/SRE guidance now; recheck version-specific Android lifecycle, Media3, Room, WorkManager and platform restrictions during implementation. Proposed constants and test counts in [the stability specification](11-STABILITY.md) are Vista design decisions, not numbers prescribed by these sources.

## Adopted practices and rejected shortcuts

| Adopt | Reject |
|---|---|
| Diagnose traces and actual operation failures | “No crash reports means stable” |
| Move blocking work out of Main and state reducers | Longer timeouts or catching every exception as a universal fix |
| Cancel transport work and invalidate stale generations | Merely hiding a spinner while the old HTTP call continues |
| Bounded optional queues with drop counters | Unbounded telemetry queues or persisting infinite logs |
| One retry owner, jitter, deadline and idempotency | Retry every error at every layer, including denials |
| Atomic database changes and explicit ownership snapshots | Clearing app data or destructive migrations to hide corruption |
| Reproducible signed release checks plus a real-device matrix | Debug-build success as performance/device proof |
| Feature isolation and forward-fix capability | Trying to install a lower versionCode as the normal rollback strategy |

Revalidate sources and dependency-specific guidance before the December release candidate. Preserve source date/version alongside benchmark evidence so guidance and measurements can be reproduced.
