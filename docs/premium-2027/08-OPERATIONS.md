# Operating a small paid service

The R20 product needs a trustworthy operating routine more than a large operations platform. Assign the named human for each role before accepting payments; role labels below do not imply staff currently exist.

## Measurement dictionary

| Metric | Definition / denominator | Authority |
|---|---|---|
| Registered accounts | Current Firebase Authentication records; 22 at the last CLI count | Auth inventory; not installs/MAU/payers |
| Activated users | Distinct consenting accounts completing a defined useful action during a stated window | Versioned event definition; document unavailable telemetry |
| WAU / D7 retention | Distinct eligible active UIDs / same eligible signup/activation cohort returning on day 7 | Cohort-aware pseudonymous events; show counts |
| Paying accounts | Distinct accounts with an effective verified paid grant at report time | Billing ledger, excluding unpaid trials/pending checkout |
| MRR | Recurring contracted monthly amount from active subscriptions, separated from cash actually collected | Billing state; refunds/past-due reported separately |
| Renewal success | Verified successful due renewals / eligible renewals due | Provider-reconciled billing periods |
| Checkout completion | Verified paid intents / eligible non-duplicate initiated intents | Server intent ledger; browser success is not the numerator |
| AI success | Useful completed answers / logical attempted turns; also report routing errors and provider fallbacks | Request ledger + human rubric, not token count |
| Playback startup | First frame confirmed / all attempted starts; also a separate eligible/available-source subset | Player events with source type; do not hide unavailable results |
| Sync durability | Successfully restored/versioned operations / attempted sync operations and retained-outbox count | Sync cursor/version evidence |
| FCM result | API accepted count, observable taps, and client receipts if implemented | Acceptance is not proof of delivery to every device |

Existing in-memory analytics can lose events; do not use it to reconcile money. Add a small versioned allowlist with pseudonymous account-scoped identifiers only where necessary and disclosed. Consent/diagnostic opt-out must actually control collection rather than exist solely as a toggle. No payment details, prompts, raw media URLs, names or emails in normal analytics.

## Operational targets and alerts

| Signal | Initial response |
|---|---|
| Paid transaction lacks entitlement after 2 minutes | Alert billing owner; reconcile provider and intent; never ask the user to pay again |
| Pending cancellation older than 15 minutes | Retry safely, alert support and make status visible; refund/escalate any unintended subsequent charge |
| Unverified or replayed notification surge | Reject unauthorized requests, keep valid inbox processing available and inspect merchant/provider state |
| AI failure >10% over at least 20 attempts or repeated provider denial | Open circuit, route only to evaluated permitted fallback within budget; show service state |
| Spend at 70% / 90% / 100% daily envelope | Warn / page / stop new optional billable work; keep account/billing/export and deterministic help accessible |
| Any confirmed cross-account data leak or lost paid grant | Pause affected feature/checkout immediately, preserve evidence and start incident process |
| Sync outbox older than 24 hours online | Show attention status and investigate; do not silently clear outbox |
| Release download/hash/manifest mismatch | Withdraw bad release pointer using a signed recovery plan; do not send another broadcast until corrected |

Low traffic makes percentages noisy. Report raw counts and time windows. Customer-visible status can be a simple hosted status page and in-app banner; it does not require a separate monitoring product.

## Support model

Proposed staffed commitment: billing/access tickets acknowledged within one business day; urgent money/access mismatches reviewed within two staffed hours. Publish actual South African support hours and holiday coverage. Do not market 24/7 support without a roster.

The support view shows account ownership, entitlement version, provider transaction references, redacted reconciliation state, app version and latest sync/request error. Access requires a separate privileged operator identity and an audit trail. Never ask for passwords, complete card numbers or Firebase tokens. Manual entitlement corrections require a reason, expiry and linkage to evidence; the model cannot grant them.

Receipts and cancellation confirmations must include merchant identity/contact, amount/currency, relevant dates, reference and correct tax wording. Refund timing follows the actual payment-provider/bank route and published customer policy. Apply current consumer rights after qualified review; do not invent a blanket “no refunds” policy.

## Retention and customer trust

Customer understands recurring billing before submitting to hosted checkout. No automatic migration from an earlier guest trial or “Got it” premium announcement into a recurring mandate. Keep local library/history readable and exportable after downgrade. Do not delete someone's account because a payment failed.

Proposed retention design: cloud conversations off by default; saved conversation expiry 30 days if enabled; operational request metadata 30 days; sync tombstones 90 days; financial/audit records retained under a separately reviewed statutory/company schedule, not erased with an ordinary conversation delete. Account-deletion flow explains legally required financial retention and stops future billing. Do not claim provider-side deletion/retention guarantees before contract verification.

POPIA work is tied to actual processing: assigned/registered information officer as applicable, documented purposes/operators and international transfers, user access/correction/deletion channel, marketing choice and incident contact (S12). No presumption that all child processing requires the same regulator application: assess the applicable permitted basis, parental competence/consent and any authorisation with qualified review before a Kids pilot.

## Notifications and the free-to-paid transition

Define separate `service_release`, `security`, `billing`, `product_news` and `marketing` categories. Required app updates can explain a genuine protocol/security need; paid-feature promotion respects marketing choices and applicable requirements. No lock-screen billing detail beyond a discreet prompt; no payment recovery through a globally subscribed topic.

- Global `vista_updates` is not the set of members, active users or all installations.
- Billing/account notifications use account-owned device tokens and never a public per-user topic containing a raw UID.
- Re-evaluate the current `updates` preference bypass before sending subscription campaigns.
- Pre-launch notices explain price, free features, quotas and explicit purchase. Renewal reminders/retry notices do not imply a charge succeeded.
- A required app update does not force opening Android or silently install an APK; purchase is a separate opt-in.

## Incidents and recovery

1. Classify money, identity/data, model, playback or distribution impact; assign an incident owner and start time.
2. Pause only the affected capability with a server flag; stop new checkout if money integrity is uncertain. Do not mass-expire paid users during an outage.
3. Preserve redacted evidence and durable ledger/outbox state. Reconcile before replaying jobs; use deduplication keys.
4. Communicate the symptom, affected scope and next update time. Never promise universal restoration from a green health endpoint alone.
5. Restore from tested backups/snapshots; reconcile provider facts again. Target ledger RPO ≤15 minutes with durable provider replay/reconciliation; target core billing recovery ≤4 staffed hours, then prove these through a drill before advertising them.
6. Correct paid periods/refunds where customers lost service, document the incident and add a regression fixture.

An APK downgrade cannot be achieved by pointing at a lower versionCode: Android generally rejects ordinary downgrades. Ship a corrected signed build with a **higher** versionCode, or disable the affected server feature. Preserve signing keys outside source publication. A manifest rollback must not strand users who already installed the new version.

## Vista Kids research track

Keep a separate specification and release decision. Start with parent-owned curated discovery, unknown-rating exclusion, parent PIN/pairing, independent profile/usage isolation and no behavioural ads/open-ended voice chat by default. Audit third-party trailers/embeds as well as metadata filters. Parents control exports/deletion; child activity is not silently included in adult personalization. No January inclusion or “safe for all children” claim without content and physical-device validation.
