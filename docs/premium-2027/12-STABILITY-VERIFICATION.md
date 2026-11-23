# Stability verification and failure-injection programme

Specification: [11-STABILITY.md](11-STABILITY.md). This is a planned test programme; it contains no claim of test execution. Commercial/payment checks remain in [04-BILLING.md](04-BILLING.md).

## Evidence before percentages

Every run records signed build/versionCode, native-source revision or archived source identifier, backend/config/schema versions, named device/OS/RAM/WebView/voice engine, network profile, dataset, test steps, duration and observed result. Separate physical-device, emulator, lab automation and field-pilot evidence. Keep private user data and secrets out of the report.

For a failed case retain a redacted trace/log, operation ID, expected terminal state, actual state, reproduction rate, affected owner and regression fixture. A green unit test does not prove a microphone was released or an install succeeded on a real TV.

## Device and network coverage

| Profile | Required coverage |
|---|---|
| Low-resource supported phone | Physical Android device with 2–3 GB RAM and constrained storage; actual supported OS recorded |
| Reference mid-range phone | Physical 4–6 GB device on a representative Android version; primary comparative benchmarks |
| Recent phone | Recent supported Android/API level, current permission/background behaviour and larger refresh rate |
| Tablet | Compact/expanded layout, rotation, large fonts and floating-chat focus restoration |
| Android TV / Google TV | Physical remote/D-pad path, decoder/audio capabilities, sign-in, update installer and no-microphone fallback |
| Minimum SDK | API 24 emulator and, if available, physical device; supplement the matrix, not replace actual low-resource hardware |
| Optional services unavailable | Google services absent/unavailable, speech engine missing, notifications denied, unavailable WebView/provider |

Exercise stable Wi-Fi, high latency (proposed 800ms RTT/1 Mbps profile), interrupted transfers, DNS/TLS failure, Wi-Fi↔cellular transition, captive/no-internet connectivity and airplane mode. Use controlled fixtures for impairment; do not generate load against third-party production streams/model endpoints without their permission. A connected network interface is not proof of reachable internet.

## Failure matrix

| Case | Injection / steps | Required outcome | Coverage |
|---|---|---|---|
| V01 Main-thread I/O | Large nested cache tree; open/refresh Settings repeatedly with traces/StrictMode in debug | Responsive controls; scan off Main; one result per request; correct non-overlapping size totals | Unit + instrumented trace |
| V02 Disk full | Fill test storage; save library/progress and download update | Honest failed/pending write; old confirmed data intact; no successful receipt invented | Device/emulator |
| V03 Database migration | Open representative data through every shipped schema→candidate migration | Schema/index correctness, owner counts, episode IDs and resume values preserved | Room instrumented fixtures |
| V04 Account switch mid-write | Pause save/exit persist, switch A→B, release barrier | A's snapshot never writes under B; stale UI/outbox hidden | Deterministic repository race + device |
| V05 Process death | Background then kill process during search, playback checkpoint, pending checkout and update | Restore small keys/owned durable operations; no duplicate payment/download; no dependency on `onCleared()` | Process-death harness + device |
| V06 Rotation/recreation | Recreate Activity, rotate and change large-font setting during key flows | No duplicate owner/request/installer or lost confirmed state | UI + device |
| V07 Out-of-order search | Delay query A, complete query B, then release A | B remains visible; A transport cancelled where applicable | Fake API + UI |
| V08 AI cancel race | Cancel during planner/tool/reply and immediately send another message | Old work stopped/invalidated; new request owns spinner/result; reservation settles once | Fake transport/backend + device |
| V09 Provider failure | Timeout, invalid JSON, truncated response, retired model, 429 and 503 | Bounded deadline/fallback; clear error, no fake success and no duplicate customer turn debit | Worker/API fixtures |
| V10 Retry amplification | Instrument every layer's dispatch during a transient outage | Total attempts/time stay within named logical budget; no multiplicative retry tree | Integration counter assertions |
| V11 Offline cache | Seed local library/catalogue cache, disconnect and navigate | Saved data accessible, freshness shown; no empty-success substitution or endless spinner | Repository + UI |
| V12 Analytics overload | Stall uploads and emit a controlled large event burst | Bounded queue/batch/memory; counted drops; UI and billing unaffected | Unit/load fixture + device memory trace |
| V13 Active cache race | Barrier between active registration, cleanup check and deletion | Active resources never deleted; cleanup reports skipped/failed accurately | Deterministic concurrency fixture |
| V14 Descriptor expiry | Expire a permitted playback descriptor during startup/seek | One bounded authorized refresh and resume or truthful terminal error | Media fixture + player instrumentation |
| V15 Playback denial | Inject 401/403/DRM denial | No cache/mirror bypass; useful action and preserved progress | Playback policy test |
| V16 Decoder/renderer failure | Unsupported fixture codec; WebView renderer loss on supported API | Finite error or fresh owned renderer; no dead view reuse, app crash or endless repair | Physical media matrix |
| V17 Playback lifecycle | 20 open/play/seek/exit cycles plus PiP/audio-focus interruption | One owner, listeners released, correct active-cache status, no retained exited view/player | Device + memory/audio trace |
| V18 Voice stop late callback | Deliver old recognition/TTS completion after Stop, after restart and after profile switch | Old generation cannot listen/speak/mutate current chat | Fake coordinator + physical microphone check |
| V19 Voice environment | Missing engine/language, permission revocation, call/headset change, repeated silence | Visible paused/fallback state, released capture/audio focus and usable text | Physical devices |
| V20 Update arithmetic | Counter values near 21,474,836 bytes, current 22,369,333-byte APK, unknown and >2 GB counters | Long arithmetic, monotonic bounded/indeterminate progress, no overflow jump to zero | Pure unit fixtures |
| V21 Update lifecycle | Remove DownloadManager row; kill/reopen; low storage; repeated Update taps | One durable operation, no permanent poll/spinner, retry resumes or starts safely | Device/instrumentation |
| V22 Update verification | Truncated APK, changed hash, wrong package/version/certificate, unapproved redirect | Install action withheld with reason; previous app and owned data usable | Artifact tests + controlled device installer |
| V23 Payment ambiguity | Timeout after provider success, duplicate callback and app return before callback | Pending→one verified grant; no second charge submission | Billing integration + permitted merchant exercises |
| V24 Sync conflict | Two devices remove/add/resume/restart while offline then reconnect | Version/tombstone/session rules hold; no lost confirmed save or wrong-owner merge | Repository/server + two devices |
| V25 Config failure | Missing/malformed/stale feature config and old client schema | Safe cached/default free UI; no accidental checkout activation; server enforces paid actions | API + UI |
| V26 Cold-start dependencies | Ads/model/update service fails or stalls at launch | Cached Home remains usable; optional startup errors isolated and diagnosable | Release-profiled startup trace |

Boundary tests should use barriers/fake clocks/controlled schedulers, not arbitrary sleeps. Test the real persistence/HTTP cancellation boundaries, not a mock that simply returns the expected implementation value.

## Soak and performance protocol

1. Install the signed release candidate and compare with the known-good release on the same named hardware/network/content fixtures. Debug builds support diagnosis; they do not establish release performance.
2. Capture a cold-start set (proposed 30 repetitions/device) and a warm/hot set; report distribution, sample count and outliers. Track first interactive content separately from first frame.
3. Execute at least 20 repeated navigation/chat/player/voice cycles. Establish warm-up, then inspect retained heap, native/graphics memory, open connections, player/recognizer count and pending jobs.
4. Run a two-hour permitted playback soak on reference phone and TV, including seek/episode/source changes supported by the fixture. Observe thermal/battery/network effects and checkpoint loss after interruption.
5. Run a 30-minute optional-event/provider-failure soak with capped synthetic clients. Assert queue size and global/provider concurrency rather than using only CPU averages.
6. Retest an interrupted upgrade and installation-permission return flow. Do not require installing an unverified artifact to test verification refusal.

The counts are an initial practical protocol, not a statistical guarantee. Investigate the hottest main-thread spans and retained owners before raising memory limits, extending deadlines or clearing caches.

## CI and release evidence layers

| Layer | Required for relevant changes |
|---|---|
| Every source change | Compile/static checks; pure state/timeout/retry/ownership tests; backend schema/idempotency fixtures |
| Data/lifecycle change | Room upgrade fixtures and targeted instrumented race/process-recreation tests |
| Candidate build | Debug and signed-release assembly/lint, artifact package/version/certificate/hash verification, current backend contract tests |
| Device candidate | Matrix cases, ANR/freeze traces, release-profile benchmarks, voice/player resource checks |
| Field pilot | Named opt-in cohort, declared diagnostics coverage, crash/ANR/failed-journey counts by candidate version, support review |

Known local commands, run serially against one checkout on the constrained Windows workstation:

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:lintDebug --console=plain
.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest --console=plain
.\gradlew.bat --no-daemon :app:assembleRelease --console=plain
node --test backend/test/safety.test.mjs
```

Instrumented tests require a supported connected emulator/device and correct fixtures; absence of a device is a missing result, not a pass. Do not launch Android Studio and terminal builds concurrently against the same outputs. Investigate a fresh failure before deleting shared caches; keep the last verified artifact separately. Candidate CI source/test workflows belong in the controlled native-source environment; publish only selected release artifacts/docs/site changes to the website repository.

## Release packet template

```text
Candidate/versionCode/source/backend/config/schema identifiers:
Dates, device/network/voice/WebView versions:
Unit/instrumented/release build results and report references:
Fault cases V01–V26: pass/fail/not run + evidence + remaining owner:
Observed fatal sessions / total reported sessions / installations:
ANR incidents, freeze traces, OS exit evidence and collection limitations:
Critical journey failures / attempts (lab separated from field):
Startup/interactive/first-frame percentiles and matched baseline:
Memory/queue/connection/resource-owner bounds:
Artifact hash/package/certificate and download verification:
Reproducible unresolved defects, affected scope and mitigation:
Rollout decision, coverage limitations, next review date:
```

## Promotion policy

No confirmed wrong-owner write, duplicate charge, lost confirmed save, active-resource deletion, microphone restart after Stop, verification bypass or reproducible candidate-introduced critical ANR/crash is accepted for the paid pilot.

Use the observed ≥99.5% fatal-free-session target with raw counts and collection coverage, but do not label 200 clean sessions as proof of the population target. The exact zero-event calculation and correlation caveat are in the research register. A short clean pilot permits a cautious next cohort only when deterministic fault tests and device coverage also pass; it does not establish a universal SLA.
