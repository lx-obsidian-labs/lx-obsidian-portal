# AI, voice, data and playback architecture

Keep Kotlin/Compose/Hilt, Room v6, Firebase identity, and the Cloudflare assistant. Avoid a rewrite, second identity system or self-hosted GPU cluster for a 22-account cohort.

## Target boundaries

```text
Android UI → ViewModel → repositories → Room / outbox
                 │                         │
                 ├─ Firebase identity      └─ account-scoped sync API → Firestore
                 ├─ Cloudflare assistant → entitlement + usage → approved model
                 └─ billing API → dedicated D1 ledger → verified payment provider

GitHub: signed APKs + release docs
Cloudflare Pages: site + docs + update manifest, never APK binaries
```

Authentication, paid entitlement, content permission and user action approval are separate checks. The model does not decide any of them.

## AI request evolution

Replace loosely trusted profile/context strings as authority with this versioned request envelope:

```json
{
  "schemaVersion": 1,
  "requestId": "random-id",
  "conversationId": "account-owned-id",
  "message": "Save the second suggestion",
  "screen": {"route": "home", "mediaId": null, "mediaType": null},
  "resultSetId": "owned-recent-results",
  "spoilerPolicy": {"allowed": false, "season": null, "episode": null}
}
```

Server resolves the UID, owned conversation/result set, permitted catalogue metadata, entitlement and current quota. `screen`/progress are hints, not evidence of payment, verified viewing position, content rights or a privileged role. Policy is the most restrictive intersection of account settings and request preferences; an old request cannot re-enable a revoked setting.

Response separates `answer`, structured `cards`, and allowlisted `actions` with stable IDs. Actions resolve on the app/server dispatcher, never by parsing generated prose or launching an arbitrary model URL. One-time action IDs bind the UID, context/result identity, arguments, policy version and expiry.

### Tools

`searchCatalog(filters)`, `getTitleMetadata(type,id)`, `getRecommendations(filters)`, `openPage(destination)`, `openTitle(type,id)`, `proposeWatchlistChange(type,id,operation)`, and `getMembershipHelp()`.

Title IDs must come from verified tool results. `openPage` remains an enum. Save/remove confirm against current profile/content settings immediately before mutation. No billing mutation, refund, arbitrary fetch, file execution or content-permission override tool. Hard limits such as runtime/language use typed metadata, not a substring classifier.

### Context and memory policy

- Keep a bounded session transcript; target 8 recent turns plus a versioned summary rather than replaying every 4000-character item twice. Token limits, not character limits alone, bound provider cost.
- Initial proposed prompt budget: 6000 combined input tokens and 600 combined output tokens across the normal two-call turn; measure and enforce actual per-call/aggregate bounds. These are planning inputs, not current measurements.
- Explicit tastes are authoritative user preferences; inferred memories are optional, reviewable suggestions with source/date. Do not send email, payment facts, precise device identity or unrelated watch history to the model.
- Offer separate controls for session history, personalization and long-term cloud memory. Disabling personalization stops using existing inferred history, not just writing new memories.
- Retain cloud conversations only with clear user choice; proposed default is session-only. If saved, proposal is 30-day expiry with immediate user-delete path. The final retention policy must match actual provider/storage behaviour.
- Spoiler protection is not guaranteed by a prompt. Exclude disallowed synopsis/episode material before generation and evaluate boundary-specific answers. General questions may acknowledge missing scene-level context.

### Usage and cost control

Entitlement lookup → atomic reservation → provider/tool execution → atomic settle/release. Store reservations with request ID, UID, billing period, maximum cost, expiry and final status. One reservation covers the complete logical turn; planner, reply and fallback are not separate customer charges. Concurrent requests cannot exceed daily/monthly/cost caps.

- Proposed paid allowance: 150 successful turns/billing period and 10/day, pending economics and pilot evidence. A smaller spend cap may stop provider work earlier only if clearly disclosed; do not advertise 150 while secretly guaranteeing fewer.
- Abuse rate limit and customer allowance are different controls. The existing 15-request/minute limit means roughly 7 normal two-request turns/minute; count logical turns consistently in the next contract.
- Timeouts/failed/no-answer responses release customer allowance; infrastructure still incurs cost and is included in operational cost measurement.
- Track tokens, provider/model, fallback reason, latency, request status and cost estimate without raw prompts. Global daily spending cap and per-account concurrency cap bound attacks across many accounts.
- Bound free-trial spending separately: one lifetime grant per eligible account, verified identity/email policy, sensible signup/request abuse limits and a global trial envelope. Do not equate a reinstall with a new trial. Assess App Check against actual sideload distribution before relying on it; avoid invasive fingerprinting as a shortcut.
- Disable a failing model with a short circuit breaker; no repeated 25-second NVIDIA timeout for every paying user. Failover has one bounded attempt within the complete request deadline.

NVIDIA remains the preferred provider if a production-permitted arrangement is established (S01/S02). Evaluate any replacement/fallback on the same dataset before choosing it. A model listing and an “available” key are not readiness evidence. No automatic live provider switch is made by this plan.

### Model evaluation before a change

Version at least 80 fixtures: 20 title/filter tasks, 15 app navigation/help, 10 follow-ups/result references, 10 profile/account isolation, 10 hostile instructions/prompted fake success, 10 spoilers and 5 missing-metadata/provider-failure cases. Add locale/device transcripts from consenting testers with identifiers removed.

Targets: 100% rejection of unknown actions/cross-account facts; at least 95% correct action routing; at least 90% grounded useful answers by a documented human rubric; zero observed critical spoiler leaks in the protected fixture set; p95 complete-turn ≤12 seconds on the named reference connection. Report counts, not a claim of universal safety. Regression against existing model blocks rollout.

## Voice design

Implement one screen-owned coordinator with a generation counter so late recognition/TTS callbacks cannot restart listening after Stop, dismissal or a new request. Handle audio focus and phone interruptions explicitly. Cancellation must cancel the actual OkHttp call/provider stream where supported, not only discard the UI result.

Preserve the TTS preference/engine selection per account/device without pretending every device has the same voice. Query installed quality/locale/network requirements, preview with user activation and provide a default fallback. Never record/transmit background audio. Do not introduce cloud TTS until its commercial permission, privacy and worst-case costs fit the plan; published character pricing can exceed membership revenue (S10).

## Sync without data loss

Firestore canonical paths: `accounts/{uid}/watchlist/{type:id}` and `accounts/{uid}/progress/{title:episode}`. Use one server-mediated sync API for paid writes so an edited APK cannot bypass D1 entitlement checks with direct Firestore writes. Client direct writes are denied; server credentials are private, tightly scoped. Firebase Admin/REST writes bypass rules, so the API must validate UID and every record explicitly.

Room remains the offline cache. Add an additive migration for an `outbox` and per-record `serverVersion`, `operationId`, `deletedAt`, `playbackSessionId`, and `lastSyncedAt` as needed. Confirm actual schema changes against exported v6; do not guess a migration version until implementation branch is current. Never use destructive migration fallback.

| Conflict | Resolution contract |
|---|---|
| Duplicate upload/retry | Same operation ID yields same result/version |
| Watchlist remove vs stale device add | Server-version precondition; durable deletion tombstone prevents resurrection; user can explicitly re-add against new version |
| Two progress updates in same session | Monotonic sequence; greatest valid sequence wins, not arrival time |
| Two playback sessions/devices | Server orders session start/finalization; retain a conflict outcome and last verified active session; device clock cannot decide ownership |
| Intentional restart from zero | New explicit playback session/restart action; not overwritten by a naive maximum-position merge |
| Offline deletion beyond tombstone retention | Force snapshot/cursor reset before upload; do not accept ancient records blindly |
| Sign-out/account switch during request | Cancel old requests, isolate Room/outbox by UID, discard results with stale account-generation ID |

Proposed tombstone retention: 90 days, with expired sync cursors requiring a full refresh. Support one-time guest import with a preview and owner confirmation; no global claim of all blank-owner data on the next sign-in. Imported operation IDs are idempotent. Cloud downgrade does not delete local data or cloud history immediately.

## Playback and permitted sources

Store measured health by permitted provider, source type, region and app/player version. Health never substitutes for content permission or payment-provider eligibility. Refresh expiring descriptors and apply required headers/DRM only where supplied through authorized integrations. Classify network, expired, unavailable, denied and decoder errors separately.

Preserve position and active playback data through bounded recovery. Never loop provider mirrors after denial. Document which controls are supported on direct Media3 versus WebView; episode/time spoofing is not an authorization mechanism.

## Implementation seams

Create `EntitlementRepository`, `BillingRepository`, `UsageReservationService`, `LibrarySyncRepository`, a versioned `AssistantContext`/action dispatcher, and a foreground `VoiceCoordinator`. Keep UI state reducers pure enough for lifecycle/billing fixtures. Backend auth/token validation and body limits are shared middleware; profile/catalogue data never enters system instructions without structured separation.
