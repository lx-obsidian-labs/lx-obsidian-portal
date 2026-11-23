# Stability delivery, controlled rollout and recovery

This programme makes [11-STABILITY.md](11-STABILITY.md) a prerequisite to new paid behaviour. It augments P01–P13 in the main delivery plan; overlapping voice/playback/operations work is not counted twice.

## Order of work

| Batch | Scope | Evidence before leaving batch |
|---|---|---|
| A — establish truth | Reproducible baseline; capture ANR/crash/failed-journey counts and release-profile traces; reproduce ST01/ST06 | Matched baseline packet; tested distinction between unknown and zero failures |
| B — remove resource/lifecycle hazards | ST01–ST04, ST09, ST11/ST12: off-main work, bounded queues, cancellation/deadlines, serialized generation-owned operations | V01/V07–V12/V18/V19/V26; no endless spinner or stale callback resurrection |
| C — protect persistence/distribution | ST05–ST08/ST10: immutable owner snapshots, checkpoints, cache deletion coordination, durable verified updates | V02–V06/V13/V20–V22/V24; all hard invariants pass |
| D — media/device resilience | ST13/ST14 plus P10/P11 player/voice hardening; renderer/audio focus/optional SDK failures | V14–V19/V26 and phone/TV soak evidence |
| E — bounded pilot | Candidate observation and incident/forward-fix rehearsal, with paid functionality off until commercial/billing gates pass | Named-cohort evidence, support ownership and explicit promote/pause result |

Reproduce and fix high-severity items before expanding UI/AI scope. Scope the batches after actual traces and assign engineering ownership. Proposed incremental capacity reserve is **8–12 focused days** beyond the existing plan, excluding overlapping P10/P11/P12/P13 work; it is an estimate, not extra staffing. Rebaseline on Nov 5. If that reserve does not fit, reduce optional scope or move the paid date rather than lowering money/data/lifecycle gates.

## Calendar integration

- **Oct 29–Nov 5:** research and Batch A; baseline evidence and first fault reproductions accompany commercial discovery.
- **Nov 6–19:** Batches B/C define the 1.0.10 stabilization candidate; paid feature development may proceed behind disabled flags on isolated branches.
- **Nov 20–Dec 3:** Batch D plus beta data/billing tests; no release candidate with unresolved hard invariant failures.
- **Dec 4–17:** Batch E and end-to-end payment/sync/renewal exercises; collect representative field evidence.
- **Dec 18 onward:** critical fixes and targeted retesting; no broad dependency/SDK upgrade, architecture rewrite or unrelated redesign inside the feature freeze.

This does not alter the already-published 1.0.9 mandatory manifest or send another notification. It defines safer treatment of future releases.

## Small-cohort rollout for the present scale

| Stage | Exposure / minimum observation | Advancement decision |
|---|---|---|
| Lab | Controlled accounts/devices; no real-user financial side effects from synthetic load | Relevant V01–V26 and release artifact checks pass |
| Internal sideload | 3–5 named willing testers, at least 24h and 30 documented critical journeys | No hard invariant failure; affected-device and failed-journey review |
| Expanded beta | 10–15 named willing testers, at least 72h and 100 documented critical journeys | No reproducible regression; backend quotas/recovery, field diagnostics coverage and support feedback reviewed |
| Broader existing cohort | Invite remaining eligible users; target at least 7 days across rollout stages and representative device/network coverage | All paid gates plus stability packet; raw counts and remaining uncertainty explicit |
| Paid pilot | Only explicitly consenting accounts; invited start, not automatic conversion | Daily payment reconciliation and reliability/cost review; pause on hard failure |

These small samples find defects and bound exposure; they do not prove a 99.5% population success rate. Do not coerce users into diagnostics or manufacture sessions to meet a denominator. If volume/device coverage is missing, document that and remain in limited observation rather than asserting a pass.

A GitHub APK URL is public distribution, not access control. Use a distinct beta manifest/manual tester link and explicit eligibility for new server features. A globally advertised mandatory update moves everyone who checks it, so it must not precede canary evidence. Immutable versioned filenames/checksums identify the exact candidate; an installed APK cannot be remotely removed by ending a cohort.

## Feature-control contract

Versioned flags: `checkoutEnabled`, `cloudAssistantEnabled`, `syncWritesEnabled`, `voiceHandsFreeEnabled`, plus minimum-supported protocol versions and permitted model/source configuration. Server enforcement protects paid/cloud actions; client flags alone are not authorization.

Flags have schema version, revision, issued/expiry times, safe defaults, rollout criteria and last-known-good handling. Invalid or absent config must not activate charging, create a global startup dependency, erase data or stop account/export/cancellation access. Never force a player stop mid-session for an optional experiment; apply transitions at defined safe boundaries. A voice-disable response ends new capture according to the foreground policy and invalidates stale generations.

Before relying on a kill switch, demonstrate: who can change it, audit record, which clients obey it, propagation latency measured on online devices, offline behaviour, and recovery from malformed/stale configuration. A client that cannot reach config has not magically received the switch; the server must still refuse affected new cloud work.

## Pause rules and failure budget

Any hard invariant failure pauses exposure immediately, independent of percentage. For other failures, pause when the candidate repeatedly exceeds the declared baseline/target on the same device/network scenario, or when two testers reproduce the same new blocker. One severe reproducible crash/ANR is enough; a requirement for two reports must never delay addressing critical harm.

Example only: a 99.5% session objective allows five failed sessions per 1000 in that defined measurement population/window. It does not permit five duplicate charges or wrong-owner writes. With 22 accounts, use individual incident review plus fault tests rather than noisy percentage-only alerting. Fixes must rerun their regression case and affected integration flows, not every unrelated suite indefinitely.

Protect essential account/billing/export routes during overload; shed optional analytics/discovery/model enhancements first (R07). Alert severity and support hours are explicit; do not promise 24/7 operation without staffing.

## Forward-fix and rollback drill

1. Pause the affected new feature/checkout and preserve current paid grants/local data.
2. Identify exact app/backend/config/schema revisions and latest known-good evidence.
3. Revert compatible server/config changes only after checking current database/schema compatibility. Use expand/contract migrations and keep old-client contracts for the supported window; destructive schema rollback is not a routine fix.
4. An already-installed bad Android binary needs a corrected **higher versionCode** signed with the same certificate. Keep a tested build/release path ready; don't point at a lower-code APK and call it rollback.
5. Publish verified immutable GitHub APKs, then site/manifest, then accurate communications. Confirm Pages output contains no APKs. Use mandatory enforcement only with a genuine documented need and a reachable recovery route.
6. Reconcile any billing/sync outbox and actual provider facts; no blind replay with fresh operation/payment keys.
7. Reopen a small cohort after the targeted fault/regression and resource-release checks pass. Record unresolved limitations and next observation window.

## Definition of stability readiness

The paid launch packet contains the source-backed findings, completed relevant fault matrix, release-profile device results, honest crash/ANR/failed-operation coverage, preserved owner/data invariants, finite retries and queues, and a rehearsed recovery path. Compile success, an online `/health` response and an attractive interface do not substitute for this evidence.
