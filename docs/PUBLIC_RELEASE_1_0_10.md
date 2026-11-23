# Vista 1.0.10 public-release record and runbook

Public release: Android **1.0.10 / versionCode 17**, published 8 November 2026 as an optional update. The live site and manifest have been verified against the GitHub release and signed artifact.

**History:** the initial decision was to finish the plan while the phone was disconnected. The user subsequently explicitly requested Cloudflare publication, GitHub APK hosting, accurate site-wide release information and an update notification. Publication proceeded with the outstanding physical-device coverage disclosed; it is not represented as passed.

## Current readiness

| Item | Actual status |
|---|---|
| Latest debug APK | **Passed:** `:app:assembleDebug`, including onboarding corrections; 7m 19s |
| JVM suite and Android test APK | **Passed:** `:app:testDebugUnitTest :app:assembleDebugAndroidTest`; 3m 53s |
| Backend tests | **Passed:** 12 Node tests, including legacy/new contracts and input bounds |
| Actual NVIDIA action probe | **Passed:** numbered download selection, AI & Voice destination and Watch List action; provider explicitly `nvidia` |
| Provider fake-robot overlay | Specific live reproduction removed successfully earlier; own AdMob retained |
| Latest candidate installation | Earlier 1.0.10 debug build installed; final onboarding/DI corrections still need installation on a reconnected device |
| Adaptive offline test | Initial run failed at the instrumentation DI access interface before transfer; interface corrected and test APK rebuilt; rerun **pending** |
| Onboarding UI/upgrade matrix | Regression state tests pass; final physical-device matrix **pending** |
| Signed 1.0.10 release/certificate/hash | **Passed:** 22,501,177 bytes; SHA-256 `31BADF75DFBA417D3D3871BB76084311AE293A06C468C5399BCB1EDC88DA5B1A`; certificate matches the documented production fingerprint |
| New AI Worker deployment/live authenticated smoke | **Deployed and passed:** legacy and agentic authenticated checks; NVIDIA primary with working Cloudflare fallback; temporary test identity deleted |
| Public website/APK/update manifest | **Live:** 1.0.10 / 17, optional update; GitHub APKs/Release assets, matching object/digest and CDN bytes verified |

## Publication evidence

- Website source: latest GitHub `master`, preserving concurrent README/runbook work. Release commit `b8bc07171e3731225926906c277ef2f40dbd6445`; download/404 fix `32e05cf85fd796f5506a7f143fc8e52109f8bfbe`; Smart TV highlight commit `7750f8c4194bf29eb7b88cd3c1a2b898fa8d4d0f`.
- Latest verified Cloudflare production deployment: `35043792-83fa-4be7-b5a1-14dd24ac7867`, `https://35043792.lxobsidianmainsite.pages.dev`, successful and tied to `7750f8c`.
- Latest GitHub CI: https://github.com/lx-obsidian-labs/lx-obsidian-portal/actions/runs/42472472874 — success.
- Site build checks pass; 15 website tests pass. Live audit loaded 23 page/routes with HTTP 200, no horizontal overflow and no page-script exceptions. Existing CSS/assets and header/footer design were preserved; an observed 404 clean-URL loop and six pre-existing text-encoding snippets were corrected. Missing routes now return 404.
- GitHub Release: https://github.com/lx-obsidian-labs/lx-obsidian-portal/releases/tag/v1.0.10. Phone/TV assets have identical GitHub SHA-256 digests and size. Downloaded CDN bytes matched the signed APK and apksigner verification passed.
- APKs are not in `dist`; a build guard rejects installers/signing files in Pages output. `/api/vista-download?platform=android` or `tv` only returns a public GitHub CDN redirect; no APK bytes are proxied or stored on Pages.
- Availability FCM accepted once: `projects/vista-cinema/messages/-1671408473659384313`, topic `vista_updates`, title “Vista Cinema 1.0.10 is available”, high-priority visible notification data, seven-day TTL, `check_update=true`, optional/nonmandatory. Its text prominently includes Smart TV responsiveness and Android/TV detection. Acceptance does not prove every device displayed it.

Candidate artifacts: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

## Release scope and accurate promises

- Festive Home collections/countdown with seasonal opt-out and everyday behavior outside the date window.
- Improved movie suggestions using meaningful viewing, saved-title seeds, explicit taste/negative feedback, metadata-backed runtime/year constraints, diversity and visible reasons.
- Searchable Settings with Downloads and AI & Voice control centres, real data/storage summaries, system notification/permission links, native playback language/caption preferences and legacy-quality compatibility.
- Catalog-backed assistant actions: show watchlist, offer actual resume progress, compare exact movie metadata, prepare a download review and confirm save/remove. Numbered follow-ups stay within current catalog results. No arbitrary URLs/code execution or fabricated success receipts.
- Download engine/library/local player for eligible direct HLS/DASH/progressive sources. Embed-only sources, offline DRM, provider expiry and network restrictions remain explicit limitations. Season batches and cellular overrides are not in this candidate.
- Provider-page ad filtering is separate from Vista's own AdMob, which is retained.

The new festive hub, Downloads screens and advanced Settings/AI surfaces are phone/tablet flows. The shared APK still includes Android TV, but TV focus/navigation and feature parity require their own device checks; do not advertise a TV download interface before it is implemented and verified.

## Onboarding continuity

Keep the existing cinematic three-stage introduction, optional name and returning-viewer welcome. Preserve `onboarding_seen_at`, display name, guest-trial timestamp, profile, Room data and preferences on upgrade.

Corrections in this candidate:

- First-time guest/sign-in/profile-setup success routes through onboarding when no completion timestamp exists, instead of bypassing the introduction until the next launch.
- Completion persists name/timestamp before navigating away, blocks duplicate finish taps and retains the tour on save failure.
- Name and tour step survive configuration recreation; completing the tour removes it from the back stack.
- Copy describes local-device library state, optional personalization, signed-in cloud AI, supported/confirmed actions, unmetered eligible downloads and streaming-only providers accurately. It does not promise universal playback, universal download availability, cross-device sync or exact resume for embedded playback.
- Replay introduction is accessible from Settings → About. Existing completion is not reset by a version bump. Returning welcome does not misrepresent “days since last welcome” as actual days of inactivity.

## Verification before publication

| Evidence | Required result |
|---|---|
| JVM regressions | No failures in catalog/search/recommendation/action/settings/seasonal/onboarding policy and state tests |
| Worker tests | Legacy clients and capability-negotiated actions both pass; authentication, role validation, selection bounds and body-size checks pass |
| Android build | Debug and test APKs compile; signed release built separately with production signing configuration |
| Download device test | Public adaptive fixture completes and seeks near end through a cache-only data source; pause/remove and profile scope checked |
| Real provider checks | Identify which movie/episode sources actually resolve/download; publish limitations, not universal compatibility claims |
| Onboarding device journeys | Fresh guest, fresh sign-in/profile setup, optional-name skip, rotation/keyboard/large text, returning viewer and replay; Back cannot resurrect completed tour |
| Upgrade journey | Signed production 1.0.9 → signed 1.0.10 retains owner-scoped saved/history, settings, trial and onboarding completion; no uninstall/data clear |
| Core device journeys | Search, details, native/embed playback, source repair, Settings actions, AI local/cloud/follow-up/stop, notifications, download review and local playback |
| Network routes | Unmetered Wi-Fi/Ethernet, metered hotspot, mobile, captive/no-upstream and Wi-Fi → mobile handover, distinguishing provider-only failure |
| Release artifact | Correct package/version, same production signing certificate, actual release size and SHA-256; verify the public downloaded bytes after publication |

A failed test blocks its affected capability from being presented as verified. A debug install cannot prove production certificate continuity. The public fixture cannot prove third-party source availability. Device verification results belong in the release record, with failures/coverage stated directly.

## Publication sequence

| Order | Work | Evidence to record |
|---|---|---|
| 1 | Finish candidate/device checks and release notes; resolve any release blockers | Test/build/device record and exact feature limitations |
| 2 | Build `:app:assembleRelease` with existing release signing; verify APK with apksigner and aapt | Package `com.example.vista`, version 17/1.0.10, certificate fingerprint, size, SHA-256 |
| 3 | Verify/deploy backwards-compatible AI Worker changes and authenticated live smoke tests | Legacy request and agentic request results, actual provider, deployed version |
| 4 | Prepare a checkout of the website repository's current `master` | Preserve current site; only selected artifacts/site/release documents |
| 5 | Publish the verified APK as `VistaCinema-v1.0.10.apk`; update phone/TV download links as applicable | Downloaded public artifact matches signed APK checksum |
| 6 | Update website release notes and root `vista-version.json` to 17/1.0.10 with real artifact hash/size | Fetch the actual public manifest; use an optional update initially unless compatibility requires mandatory |
| 7 | Pilot fresh install and signed upgrade through the actual public link | Preserved data/onboarding and working core journeys |
| 8 | Send one release-available notification only after those checks | Exact FCM ID/payload, no claims beyond verified scope |

Verified destinations from the existing runbook:

- Website/APK repository: `lx-obsidian-labs/lx-obsidian-portal`, branch `master`.
- Website: `https://lxobsidianportal.co.za/vista.html`.
- App update manifest: `https://lxobsidianportal.co.za/vista-version.json`.
- AI Worker: `https://vista-assistant.feedpatelmusic.workers.dev`.
- Cloudflare Pages project: `lxobsidianmainsite`.

APKs remain on GitHub and out of the Cloudflare Pages build output. Never publish the entire Android workspace, `.env`, signing material, user data or debug APKs to the website repository. Commits/pushes/public publication are separate requested release operations, not implied by creating this plan.

## Communications and rollback

The festive **coming-soon** message was already accepted once: `projects/vista-cinema/messages/4261578796388226545`. Do not repeat it as a test. Existing 1.0.9 release messages are retired locally after users upgrade; previously delivered notifications on older clients cannot be universally recalled by a new broadcast.

Suggested availability wording, after public verification:

> Vista Cinema 1.0.10 is available
>
> Your festive movie nights just got an upgrade: seasonal collections, better suggestions, useful new settings and downloads from supported sources. Open Vista to check for the update.

For a bad APK, publish a signed higher-versionCode hotfix; do not ask users to uninstall and lose local data. Server action behavior can be rolled back independently while preserving legacy request compatibility. Keep the previous public artifact and release record for diagnosis; only adjust mandatory minimum-version behavior when its impact is understood.
