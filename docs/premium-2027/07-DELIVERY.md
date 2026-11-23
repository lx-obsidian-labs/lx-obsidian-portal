# Delivery sequence and January launch gates

Target: **1 January 2027, SAST**, conditional on the gates below. Planning horizon starts 29 October: roughly nine weeks, including December holidays. Estimates are focused engineering days, exclude vendor/legal waiting time, and are not a claim that one engineer can execute all work concurrently.

## Critical path

Commercial permission/product boundary → billing adapter and durable ledger → paid entitlement → assistant quota/sync enforcement → pilot → renewal/cancellation evidence → controlled launch.

Playback/voice stabilization and interviews run alongside commercial discovery where staffing permits. A single engineer uses the fallback scope below, not an impossible parallel schedule.

| Window | Deliverable | Exit evidence | Proposed accountable role |
|---|---|---|---|
| Oct 29–Nov 5 | Source/terms inventory, merchant onboarding, product interviews, telemetry definitions | Provider/catalogue commercial route, 8 interviews or documented shortfall, reviewed fee inputs | Founder/product owner |
| Nov 6–19 | 1.0.10 stabilization; server launch state; billing prototype; entitlement DB; usage reservation prototype | Reliable build/upgrade, deterministic billing tests, cancellation UX and approved provider contract | Android + backend engineering |
| Nov 20–Dec 3 | 1.1 private beta: checkout, active/pending/cancelled UI; bounded AI; library-sync prototype | End-to-end staging payments, two-device sync/conflict/account-switch tests | Engineering + QA |
| Dec 4–17 | Controlled paid-path exercises, failure/renewal/reversal handling, cost sampling and support rehearsal | At least 10 controlled payment-path exercises; reconciliation totals match; usage/cost report | Billing owner + QA/support |
| Dec 18–27 | Feature freeze and regression, holiday support coverage, final provider/cost/channel review | Release candidate, rollback rehearsal, customer copy aligned with actual offer | Release owner |
| Dec 28 | Written go/no-go | Every hard gate has evidence and owner; unresolved blockers mean delay | Founder plus engineering/billing owners |
| Jan 1–7 | Invite-only R20 pilot; monitor first cohort | Daily reconciled money/access, customer feedback, bounded costs | Named on-call owner |
| After two renewal cohorts | Expansion decision | Measured retention, cancellation/refunds, contribution, source reliability | Product/billing owner |

Starting the target day is optional; correctness of charging/cancellation is not. Schedule a staffed daytime launch if midnight coverage is unavailable. No automatic charging of the existing 22 accounts.

## Implementation work packages

These are scoped delivery specifications for import into the issue tracker, not completion claims.

| ID | Work and likely code seam | Dependency | Estimate | Acceptance evidence |
|---|---|---|---|---|
| P01 | Permission/merchant/price inventory and final free/paid contract | None | 2–3 + external wait | Applicable model/catalogue/content terms, merchant approval and quote; no unknown-cost launch assumption |
| P02 | Server product configuration; replace local launch authority in `PremiumAnnouncement.kt`, profile, TV and app guide | P01 product decision | 1–2 | Fake device clocks/timezones never activate billing; planned/live/paused render consistently |
| P03 | Billing Worker/D1 migrations, authenticated routes and provider adapter | P01 | 5–7 | Protocol vectors, amounts, intent ownership and schema tests |
| P04 | Inbox/outbox, idempotent grants and scheduled reconciliation | P03 | 3–4 | Concurrent duplicates, reordering and crash-retry fixtures; unmatched-payment report |
| P05 | Android/TV membership states and hosted checkout/pairing | P02/P03 | 3–5 | Purchase pending/restart/return, wrong account, decline, QR expiry and accessibility tests |
| P06 | Cancellation, receipts, support, refunds and verified renewal exercises | P04/P05 | 3–5 | Cancel future billing with paid-through retained; live-supported paths reconciled |
| P07 | Entitlement middleware, logical-turn reservations, provider circuit breaker and spend reports | P04 | 3–4 | Quota races, refunds/released allowances, expiry and total-deadline tests |
| P08 | Room outbox/version migration, Firestore server sync, import/tombstone/conflict rules | P07 paid writes | 5–7 | All shipped schema upgrades, two-device restore/conflicts, guest import and owner isolation |
| P09 | Typed assistant filters/result references, server context and 80-case eval | P07 | 3–5 | Evaluation report against current model; no profile/payment authority from client strings |
| P10 | Voice generation/lifecycle/audio-focus coordinator and device previews | Stabilization baseline | 2–4 | Named physical-device matrix, interruption/permission/noisy-input evidence |
| P11 | Playback outcome instrumentation, authorized descriptor/recovery verification | Permission inventory | 3–5 | Distinct direct/embed outcomes, bounded retries and retained position |
| P12 | Observability, consent/data paths, admin support view, deployment/rollback runbook | P04/P07 | 2–4 | Redacted support diagnosis, restore exercise, real alert and rollout pause rehearsal |
| P13 | Full regression, cohort pilot and holiday support rehearsal | P05–P12 scoped RC | 5–7 elapsed+ | Release gate packet and signed artifact/checksum continuity |

The sum is substantially more than one engineer's uninterrupted days before January. Validate staffing by Nov 5. Reduce scope or move the date; do not relabel incomplete work “done”. If billing needs a new external backend integration the estimates must be revisited.

## Capacity fallback and scope order

If one engineer is available, prioritize P01–P07, P10/P11 stabilization and P12/P13 before advanced assistant changes. Cross-device sync remains a key proposed paid benefit: if P08 cannot be delivered, remove that claim and re-test willingness to pay for the smaller product, or postpone charging. It is not acceptable to sell sync as “coming soon” inside the core January benefit.

If provider/catalogue permission or margin remains unknown by Dec 15, keep a free technical beta. If billing/cancellation fails by Dec 17, disable checkout via the server flag and defer launch. If voice quality fails on some engines, retain text/device-default voice with accurate capability messaging; do not block a sound text product on a premium voice experiment.

## Hard gates

| Gate | Required evidence |
|---|---|
| Commercial route | Applicable permitted production AI/catalogue/content scope, approved merchant, final customer offer and cost model; no unquoted critical dependency assumed free |
| Money/access integrity | All specified billing fixtures pass; zero unexplained duplicate charges/grants; controlled transactions, cancellation, reversal and renewal schedule reconciled |
| Access activation | Target 95% of controlled confirmed payments visible within 60 seconds; aged pending cases reconcile/alert. Report actual numerator/denominator, at least 10 exercises |
| Data continuity | Every supported upgrade fixture, guest import, two-account isolation, deletion tombstone and multi-device conflict exercise passes |
| Assistant | 80-case report meets targets; atomic quota enforcement; no billable-user failures counted as success; measured reference inference budget fits advertised allocation |
| Playback | On permitted test sources: at least 95/100 controlled starts succeed; sample at least 30 distinct real sessions across named devices/networks; unavailable/denied outcomes reported separately and in all-attempt totals |
| Resume | At least 95% of supported direct-player resume fixtures within ±5 seconds; WebView support explicitly described rather than implied |
| Voice | Physical-device lifecycle matrix passed; no capture after Stop/background; engine-specific failures have text fallback |
| Stability | Proposed target ≥99.5% crash-free sessions; show counts and sample size. Fewer than 200 sessions is insufficient to infer a stable population rate |
| Operations | Named support/on-call coverage, one cancellation/refund rehearsal, restore/reconciliation drill, signed release and public delivery checks |

These are acceptance targets, not current measured performance or contractual SLAs. Tiny cohorts provide limited statistical confidence; one bad billing event or lost library record can block launch regardless of a percentage.

## Release evidence packet

Archive dated build/test commands, app/backend/schema/config versions, payment reconciliation totals with redacted IDs, permission/quote references, device matrix, AI evaluation, actual cost distribution and sign-off decision. Keep private evidence outside public docs. Exact customer identities, payment tokens and raw conversations are not release-note material.

Publish only selected signed APKs/docs/site/manifest changes from the site checkout. Verify certificate continuity and checksums; exclude APKs from Pages output. Stage production rollout by feature flags and invited UID allowlist, not by hoping an FCM topic equals the paid cohort. Use minimum-version enforcement only when needed for protocol/security compatibility, never as an excuse to require purchase.
