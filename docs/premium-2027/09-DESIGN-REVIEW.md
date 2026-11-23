# Design review, decisions and remaining uncertainties

This plan was revised against current source and official research rather than simply expanding the feature list.

## Critique rounds

| Round | Challenge | Resulting improvement |
|---|---|---|
| Product value | Why pay if the new feature is another chatbot? | Make continuity, editable taste and reliable account/support flows central; AI performs real bounded tasks |
| Commercial reality | Does a successful free API call make a paid product viable? | Production permission and commercial catalogue scope become first-week dependencies; free credits removed from break-even savings |
| Low-price economics | What happens when a member uses every advertised turn? | Two-call token model, gross card-fee VAT, payouts, reserves and support included; visible bounded allowance and stress cases |
| Voice quality | “Highest quality” enum is not an audible quality test | Physical voice previews/engine matrix; no unlimited cloud TTS promise |
| Money correctness | Webhooks duplicate, arrive late and sometimes disappear | Immutable facts, idempotency, durable inbox/outbox, periodic reconciliation, cancellation pending state |
| Data correctness | Sync could resurrect deletes or transfer shared-device history | Version preconditions, tombstones, explicit import, session-based progress conflict rules and no destructive migration |
| Operational reality | One founder cannot safely ship every feature before a holiday | Effort ranges expose over-capacity; explicit scope/date fallbacks and staffed launch hours |
| Customer trust | Midnight or mandatory update might be mistaken for consent to pay | Server launch flag, no automatic charging, retained data/export/free account controls and separate marketing |
| Kids scope | A child filter is not a child product | Separate parent-led research and release gate; exclude from January paid promise |

## Decision log

| Decision | Proposed choice | Reconsider when |
|---|---|---|
| D01 target and timezone | January 2027, SAST; earliest Jan 1 subject to gates | Staffing, vendor approvals or pilot evidence misses dates |
| D02 offer | R20 monthly, one account profile/two devices; exact free/paid contract approved after discovery | Interviews show insufficient value or measured cost exceeds sustainable envelope |
| D03 payment channel | Sideload + hosted Payfast candidate; backend entitlement independent of gateway | Merchant eligibility/quote/test failure, or Play distribution requires a new channel design |
| D04 storage | D1 is billing authority; Firestore account-scoped sync via validated server API; Room offline cache | Measured operational complexity/cost justifies a consolidation, with a migration plan |
| D05 AI | NVIDIA preferred subject to applicable production permission/capacity; evaluated fallback; usage ledger | New model retirement, permission limit, evaluation or cost failure |
| D06 voice | Free installed Android TTS as default; no always-on service | Measured cloud alternative fits permission/privacy/cost and clearly improves quality |
| D07 distribution | APKs stay in GitHub; Pages hosts website/docs/manifest only | Owner explicitly changes hosting strategy; monitor repository binary growth |
| D08 subscriptions | Monthly only; no 12-month referral/annual promise in first two cycles | Actual cohort economics, renewal performance and funded liability model exist |
| D09 Kids | Separate discovery track, not January membership inclusion | Parent/content/privacy controls and operational ownership are demonstrated |

## Unresolved inputs and deadline/fallback

| Input | Required evidence / accountable role | Deadline | If missing |
|---|---|---|---|
| AI production route | Applicable NVIDIA/model/hosted-service permission, quota/capacity and price; founder/backend | Nov 5 initial, Dec 15 final | No paid AI promise; permitted evaluated alternative only after decision |
| Commercial metadata/content scope | TMDB and each actual media/provider contract; founder | Nov 5 initial, before paid beta final | Keep relevant feature uncharged/unavailable as appropriate; do not sell unverified rights |
| Merchant + exact ITN contract | Approved account and current signed fixture/verification vectors; billing owner | Nov 12 | Stop checkout implementation assumptions; move pilot date if necessary |
| VAT/customer terms | Accountant/qualified reviewer decisions on actual business; founder | Before first paid test with customers | Model scenarios only; no published tax invoice/contract claim |
| Final paid value/allowance | Interviews + 7-day observed use + cost report; product owner | Nov 12 offer, Dec 17 final allocation | Reduce/reprice communicated scope or postpone charging |
| Staffing/holiday support | Named engineer, reviewer, support and on-call coverage; founder | Nov 5 / Dec 17 | Scope cut or staffed later January launch |
| Android channel requirements | Current verification/Play rules for intended countries and channel; release owner | Nov 6 / Dec 15 | No unsupported distribution-channel claim; resolve before affected rollout |

## Final adversarial launch scenarios

Before sign-off, ask a reviewer to act as: a customer charged twice; a cancelled member who renews unexpectedly; a member without connectivity at expiry; a second family member using the same device; a user removing a saved title from a stale device; a user who asks for “the second one”; a provider that returns fabricated tool success; a payment arriving while the app is closed; and an operator who has to recover the ledger without charging anyone again.

The product is ready when these people get predictable, explainable outcomes with evidence. “The UI looks premium” and “the countdown reached zero” do not answer those scenarios.

## Confidence assessment

**High confidence:** current-code gaps, published list-fee arithmetic, the need for verified payment authority, preserving local data, and separating a Kids product. **Medium confidence:** proposed stack and initial limits, pending measured workload and provider tests. **Low/unestablished:** willingness to pay, retention, paid production provider pricing, commercial content economics and holiday delivery capacity.

Satisfied planning standard: each January feature has a customer outcome, source-grounded rationale, implementation seam, dependency, measurable acceptance, operational failure policy and a cost/permission boundary. Implementation success still requires the delivery evidence; these documents do not manufacture it.
