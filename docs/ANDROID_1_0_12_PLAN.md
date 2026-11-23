# Vista Android 1.0.12 — proposed next-update plan

## Release intent

Proposed version: **1.0.12**, Android **versionCode 19**. This is a plan, not an announced release date or a claim that the features are implemented.

Theme: **A more dependable movie night.** Build on 1.0.11's catalog recovery, provider-ad controls, TV focus scrolling and optional feedback poll. Favor measurable improvements to existing viewing journeys before expanding the product surface.

## Starting evidence

- Android 1.0.11/code 18 is public, signed with the existing production certificate. Its focused JVM tests, release vital lint and public download hashes passed.
- Physical-device TV behavior, regular-user Firestore poll submission, provider playback and a signed upgrade remain verification gaps. Close these first; if a serious regression is reproduced, ship a focused hotfix rather than waiting for every planned feature.
- The new poll/reporting system exists, but the last recorded aggregate report had zero responses before rollout. Do not infer user priorities or satisfaction without new data.
- Live TV and Sports have been announced as planned, with no confirmed launch timing or availability.

## Priority and scope

| Priority | Workstream | User outcome | Main implementation area |
|---|---|---|---|
| P0 | Real-device baseline and upgrade | Know that the public APK installs over 1.0.10/1.0.11 and retains the viewer's data | Signed APK/device verification, onboarding, Room/preferences |
| P1 | Catalog continuity | Previously loaded titles remain useful when a service fails; failures are distinguished from empty results | `TmdbCatalogRepository`, `MetadataCacheInterceptor`, `HomeViewModel`, section paging |
| P1 | Playback recovery | A clear, finite path from an unavailable source to another source, without repeated dead-end loops | `VidsrcPlaybackRepository`, `StreamResolver`, `PlayerViewModel`, `PlayerScreen` |
| P1 | TV navigation completion | Predictable remote movement, focus restoration and usable short/large-screen layouts | `TvScroll`, `TvHomeScreen`, `TvSearchScreen`, `TvDetailsScreen` |
| P2 | Feedback delivery and action | Users know their feedback arrived; the team can turn actual responses into fixes | `feedback/`, Firestore access rules, `poll-report.mjs` |
| P2 | Library/download clarity | Saved, resumable, downloaded and streaming-only titles are clearly distinguished | Library, playback progress, downloads state and recovery |

## 1. Close the device-verification gaps

Run the exact published 1.0.11 APK on an Android phone and a TV/Google TV device before changing behavior. Exercise a fresh launch and an in-place signed upgrade. Confirm saved titles, profile, trial/onboarding state and completed downloads are retained.

Verify Home, title search, movie details, series seasons/episodes, native and embedded playback, source switching, recovery, Settings and feedback submission. Record device/OS/WebView version and Wi-Fi/mobile outcomes so environment-specific failures can be separated from application defects.

**Acceptance:** reproducible failures have evidence and a targeted fix; successful journeys are recorded as actual device results, not inferred from compilation.

## 2. Make the catalog resilient beyond the HTTP cache

- Add a last-successful Home/section metadata snapshot with source, schema and fetched-at information, using an upgrade-safe persistent store.
- Render a useful saved catalog promptly, then refresh in the background. Clearly label saved/stale metadata when it is being used.
- Keep a healthy previous row when that row's refresh fails, even when other rows succeed. Do not silently present an old row as freshly loaded.
- Keep pagination failures retryable at the same page; prevent duplicate fetches and accidental end-of-list states.
- Distinguish connectivity, timeout, access denial and rate limits in safe diagnostics and actionable UI. Never include API keys, signed URLs or response bodies in routine diagnostics.

**Acceptance:** after one successful load, a simulated catalog outage does not erase the screen; retry restores fresh data without duplicate cards. A first install without data receives an honest recovery state.

## 3. Finish playback recovery and source presentation

- Show clear source labels for native/direct, embedded and backup playback.
- Track failed sources within the current playback session so automatic recovery does not bounce repeatedly between the same failures.
- Keep retries bounded and preserve supported native-player progress during recovery.
- Test provider ads across nested frames and multiple supported Android WebView versions; add targeted fixes only for reproduced resources/overlays.
- Establish a maintenance check for the 30-day backup key and confirm its acceptance before a release. Design a server-managed configuration/proxy only if the provider supports that integration; do not assume embed credentials can be refreshed invisibly.

**Acceptance:** a failing source leads to a finite retry/switch path, real verification challenges and controls remain usable, and backup playback is tested with the current key. Do not claim universal ad-free playback or exact resume for every embed.

## 4. Complete the TV experience

- Test long-press/rapid D-pad input across long rows and search grids.
- Restore the last focused card and scroll position when returning from Details or playback.
- Make Details, season/episode lists, recovery actions and source controls reachable by remote.
- Check overscan, 720p/1080p/4K logical layouts, large text and right-to-left navigation where supported.
- Add on-device Compose focus tests for measured problems; geometry-only JVM tests cannot establish remote usability.

**Acceptance:** no focus traps or clipped selected cards in the agreed TV matrix; returning from a title preserves context and Back navigation remains predictable.

## 5. Make feedback useful and dependable

- Verify poll submission with a normal signed-in account under the actual deployed Firestore rules, not only the admin reporting account.
- Confirm retry does not duplicate votes and profile updates preserve feedback fields.
- Improve account-change and delivery-error behavior where device testing exposes gaps.
- Summarize actual sentiment, pain points, feature priorities and discovery channels. Keep private comments separate from explicitly permitted marketing quotes.
- If a follow-up poll is needed, give it a new campaign ID and ask a short question about whether the update improved the user's experience; do not overwrite the 1.0.11 answers.
- Use the aggregate results to choose one follow-up usability improvement rather than guessing or adding every requested feature at once.

**Acceptance:** normal-user submission, duplicate prevention and aggregate reporting are demonstrated. Any marketing summary uses real counts and respects quote choices.

## 6. Clarify watchlist, resume and downloads

- Clearly distinguish saved metadata from an actually completed offline download.
- Show actionable download states such as waiting for Wi-Fi, insufficient storage, source expired and streaming-only, where the engine can establish them.
- Verify pause/resume/retry/removal and playback after process restart with a public adaptive-media fixture.
- Preserve completed downloads during temporary-cache repair.

**Acceptance:** an eligible completed download plays with networking disabled; unsupported sources are not represented as offline-ready. Native playback resume is tested separately from embeds.

## Live TV and Sports roadmap

Keep public wording as planned. During this cycle, establish the intended channel/event sources, supported regions, program/event data and playback integration, then prove one real channel/event in an internal prototype. Decide whether a separate beta is justified from that evidence. No nonfunctional Live TV tab or invented schedule belongs in the stable 1.0.12 release.

## Delivery sequence

1. Establish device baseline, read real poll results and reproduce the highest-impact failures.
2. Implement catalog continuity and finite playback recovery.
3. Complete focused TV, feedback and library/download improvements.
4. Run targeted regression tests and real phone/TV journeys against the signed candidate.
5. Build with the existing production certificate, verify package/version/hash/size, and test an in-place upgrade.
6. Publish GitHub APKs, website notes and the Android manifest together as an optional update initially.
7. Verify the actual public downloads, then send one availability announcement and monitor feedback.

## Release evidence required

| Area | Required evidence |
|---|---|
| Data continuity | Signed upgrade retains profile, saved titles, history, preferences and completed downloads |
| Catalog | Outage/recovery, stale labeling, first-install failure and failed-page retry |
| Playback | Working native source, embedded source, failed-source recovery and tested backup key |
| TV | Physical remote navigation, focus restoration, readable/uncropped layouts |
| Polls | Normal-user submit, retry/idempotence and accurate aggregate reporting |
| Downloads | Eligible adaptive fixture completes and plays offline; cache repair preserves the copy |
| Distribution | Correct version/code, production signature, public bytes matching published hash/size |

## Proposed announcement after verification

**Vista 1.0.12 is available**

> A more dependable movie night: useful catalog recovery, clearer playback choices and refined remote navigation. Open Vista to check for the update, then tell us how it feels in Settings.

Adjust this wording to the features actually shipped and verified. Do not announce a date or promise Live TV/Sports availability from this plan.
