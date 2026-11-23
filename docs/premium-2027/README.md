# Vista 1.1 — January 2027 premium programme

Research date: **29 October 2026**. Target: **R20 per month, South Africa, January 2027**. Status: implementation-ready design proposal; billing, sync and launch controls described here are not implemented by creating these documents.

## Executive decision

Build a dependable personal cinema companion before adding more spectacle. The paid promise is continuity, useful discovery and a quieter first-party experience. Do not sell guaranteed access to every catalogue title, unlimited AI, universal natural voices, or a finished kids product.

The earliest target is **1 January 2027, 00:00 Africa/Johannesburg** (`2026-12-31T22:00:00Z`). This is a planning target, not an instruction to start charging by the clock. A server-controlled launch decision and explicit customer purchase are required. If readiness fails, retain the free experience and publish a revised date.

## Read in this order

| Document | Purpose |
|---|---|
| [Research and evidence](01-RESEARCH.md) | Official sources, blocked sources, commercial assumptions and revalidation dates |
| [Critical assessment](02-CRITICAL-ASSESSMENT.md) | Current-code evidence, weaknesses, and how proposed features survive criticism |
| [Product specification](03-PRODUCT.md) | Free/paid boundary, journeys, prioritized features and acceptance criteria |
| [Billing and entitlements](04-BILLING.md) | Payment lifecycle, API/data contracts, reconciliation and failure behaviour |
| [AI, voice and sync architecture](05-ARCHITECTURE.md) | Profile isolation, tool execution, quotas, sync conflicts and staged migrations |
| [Economics](06-ECONOMICS.md) | R20 margin model, verified fees, sensitivity and executable scenario calculator |
| [Delivery and launch](07-DELIVERY.md) | Dates, dependencies, implementation work packages and evidence-based release gates |
| [Operations, measurement and trust](08-OPERATIONS.md) | SLOs, support, cancellation, privacy, notification segmentation and incident recovery |
| [Design review and decisions](09-DESIGN-REVIEW.md) | Critique rounds, accepted decisions, unresolved business inputs and scope cuts |

## Baseline that the plan builds on

- 1.0.9 / versionCode 16 has signed phone/TV APKs, a live mandatory manifest, and a verified NVIDIA conversational backend. Website release commit: `5db6254` in `lx-obsidian-labs/lx-obsidian-portal`.
- **22 Firebase Authentication accounts** were counted using Firebase CLI. This is not MAU, installation count, paying customers or evidence of willingness to pay.
- Local Room v6 separates watchlist/history by owner. Cloud synchronization has not been demonstrated.
- The deployed assistant has Firebase JWT verification and a 15-request/minute per-UID limiter; it has no paid entitlement or durable spending ledger.
- The app currently promises a January launch from a local countdown. That UI is not billing infrastructure.
- Published APKs stay **in GitHub**. Cloudflare Pages receives website/docs/manifests, not APK binaries. The native source folder must not be pushed over the website tree.

## Definition of success

A user understands the R20 recurring price, purchases once, receives access reliably, sees their library on another device, gets useful bounded assistant help, and can cancel without contacting the founder. The product still handles outages, failed renewals, refunds and loss of connectivity honestly.

Success is measured by completed useful sessions and retained consenting customers, not registered-account counts, chat-message volume, or an animated countdown.

## Proposed releases

| Release | Commitment |
|---|---|
| 1.0.10, November | Reliability, measured voice behaviour, source/terms review, truthful launch messaging and instrumentation |
| 1.1 beta, November–December | Server entitlements, hosted checkout, library sync, usage limits and support flows |
| 1.1 paid pilot, January | Invite-only R20 monthly launch after all paid-path gates; same product contract before broader rollout |
| Later 1.1.x | Better recommendation explanations, permitted voice improvements and additional payment methods proven by pilot demand |
| Separate Vista Kids track | Parent-led discovery prototype only after content, privacy, parent controls and operational scope are defined |

## Business inputs still required

Merchant onboarding/approval, company VAT position, commercial catalogue permissions, permitted production AI arrangement, actual provider quotes, and an assigned support owner are not established by the available code. Each has an explicit deadline and fallback in the delivery plan. Recommendations here are concrete defaults, not fabricated approvals.

The required Beads CLI is unavailable in this environment. These files are product/design deliverables, not a substitute issue tracker. Import the work packages into Beads when it is available.
