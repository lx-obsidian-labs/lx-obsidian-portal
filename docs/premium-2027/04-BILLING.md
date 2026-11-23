# Billing and entitlement design

Status: proposed contracts, not deployed endpoints. Primary candidate: **Payfast hosted recurring card checkout**, pending merchant approval, exact ITN specification and live-cycle verification (S04–S06). Current distribution is sideloaded APK; a future Play build requires a separate channel review under S07.

## One authority for each fact

| Fact | Authority |
|---|---|
| Who is signed in? | Verified Firebase UID/token, including expiry and session policy |
| What product/price can be bought? | Versioned server product catalogue, not client request amount |
| Was money received/reversed? | Verified payment-provider facts plus reconciliation |
| What can this account access now? | Server entitlement derived from the immutable payment ledger and product rules |
| What does Android display offline? | Bounded cached projection; never proof for a new payment or server action |

Do not use DataStore, Firebase profile fields, browser return URLs, client clocks or a `premium=true` request as billing authority. Custom claims can advertise an entitlement version for refresh; stale tokens cannot extend paid access (S08).

## Proposed service layout

Cloudflare Worker billing routes + **dedicated Vista D1 database** for the ledger/entitlements + scheduled reconciliation. Reuse Firebase identity, not the website's unrelated customer/team records. Keep sync data in scoped Firestore paths as specified in architecture. The billing database is the sole authority; Firestore/profile projections are never independent writers of paid status.

Workers hold provider secrets as bindings. The payment adapter handles current provider authentication/signatures and never logs card details, passphrases, reusable charge tokens or raw authenticated payloads. Durable storage holds only required provider identifiers, encrypted reusable tokens if indispensable, and redacted event facts. Hosted provider checkout owns card entry.

## Internal API contract

All authenticated routes derive `accountId` from Firebase. Standard errors include `code`, `requestId`, `retryable`; no credentials or full provider response is returned.

| Route | Input | Output / behaviour |
|---|---|---|
| `GET /api/v1/product` | App version/channel | R20 / `ZAR` / monthly, launch state, feature allocation, terms version, minimum supported billing client |
| `POST /api/v1/billing/checkout` | `productId`, `termsVersion`, random idempotency key; no trusted UID/amount | Server intent, allowlisted hosted checkout URL/fields, expiry; one pending intent per account/product |
| `GET /api/v1/billing/checkout/{id}` | Owned intent ID | `pending`, `verified_paid`, `failed`, `expired`; includes entitlement version if paid |
| `GET /api/v1/entitlement` | Identity | Effective capabilities, paid-through, renewal state, policy version and server time |
| `POST /api/v1/billing/cancel-renewal` | Idempotency key, optional feedback | Durable cancel request; returns pending until provider confirms; no premature “cancelled” success |
| `GET /api/v1/billing/receipts` | Cursor | Account-scoped redacted receipts; receipt amount/tax wording follows actual merchant treatment |
| `POST /api/v1/billing/notifications/payfast` | Provider POST | Authenticates exact provider protocol; durably accepts then acknowledges valid notifications |
| `POST /api/v1/support/requests` | Category, owned transaction reference, user message | Ticket reference; ordinary users cannot perform refunds through this endpoint |

Browser return path only opens the pending screen and triggers refresh. Verify app links and hosted domains. A TV pairing token is random, one-time, expires after 10 minutes, and does not put a Firebase ID token in a QR code/URL. Phone sign-in must match the TV account before payment intent association.

## Minimal records and constraints

```text
products(id, version, amount_minor=2000, currency=ZAR, interval=month,
         launch_state, quota_policy_version, terms_version)
checkout_intents(id, account_id, product_id, product_version,
                 idempotency_key, provider_reference, state, expires_at)
  UNIQUE(account_id, idempotency_key)
subscriptions(id, account_id, provider_subscription_ref, renew_state,
              paid_through, cancel_requested_at, cancelled_at, version)
  UNIQUE(provider_subscription_ref)
payment_facts(id, provider, provider_transaction_ref, fact_type,
              amount_minor, currency, verified_at, billing_period_key, redacted_hash)
  UNIQUE(provider, provider_transaction_ref, fact_type)
entitlements(account_id PRIMARY KEY, features_json, effective_until,
             state, version, product_version, updated_at)
provider_inbox(id, dedupe_key UNIQUE, verification_state, processing_state,
               received_at, redacted_facts, attempts)
outbox(id, command_key UNIQUE, type, account_id, payload, state, attempts)
```

Schema is illustrative, not production SQL. Payment amounts use integer minor units internally. Do not assume every provider endpoint uses the same unit: normalize from the provider's documented decimal/minor-unit contract and test R20 precisely. Currency may need to be established from the stored checkout/merchant contract if absent in the notification.

## Notification processing, retries and reconciliation

1. Limit body size before buffering. Parse with the exact expected content type and canonicalization rules. Verify provider signature/authentication and any required server validation using current official protocol/test vectors. Do not substitute a generic HMAC recipe for Payfast's protocol.
2. Validate merchant identity, stored checkout association, provider transaction reference, amount, currency/merchant currency and approved product version. A subscription-created event alone does not establish a paid period.
3. Derive a deduplication key from stable provider transaction/event facts. If Payfast supplies no globally unique event ID, document and test the canonical transaction+state fact key; raw-body hash alone is insufficient.
4. Persist validated inbox facts. Acknowledge only after durable acceptance; retry-safe processing transitions inbox and ledger/entitlement atomically where possible.
5. Add exactly one grant per verified billing period. Duplicate success never adds another month. Use billing-period boundaries from the verified provider schedule/receipt, not `now + 30 days`.
6. Payment facts are append-only. Refunds/reversals are new facts; compute entitlement deterministically. Delayed “paid” events must not blindly overwrite a later verified reversal or cancellation schedule.
7. Use an outbox for receipts, cancellation calls and access-refresh messages. Inbox success does not require email delivery success.
8. Reconcile provider transaction history at least daily, and every 15 minutes for aged pending intents/cancel requests. Escalate unmatched payments rather than extending access speculatively.

If the provider contract requires a particular response/timeout/retry schedule, the adapter must implement it. The exact schedule is an implementation-spike output, not assumed here. Separate staging and production merchants, databases, callbacks and secrets.

## State and time policy

| State | Access | Exit |
|---|---|---|
| `free` | Free feature set | Explicit verified purchase |
| `checkout_pending` | Existing entitlement unchanged | Verified paid fact, verified failure or intent expiry |
| `active` | Paid until `paid_through` | Cancellation request, failed renewal or verified reversal |
| `cancel_pending` | Existing paid access | Provider confirms future-charge cancellation or support resolves failure |
| `cancel_at_period_end` | Paid until existing period end; no next charge | Period end → free/expired |
| `past_due_grace` | Proposed 72-hour paid-feature grace after an established paid period only | Verified renewal → active; grace expires → expired |
| `expired` | Free plus retained owned data/export/account support | New verified paid period |
| `reversed` | Recompute affected grant; free data remains | Finance resolution; never manufacture a new charge |

Keep renewal state separate from payment-grant state. Cancellation does not refund the current period automatically, and a refund does not prove the provider cancelled future renewals; both operations must be verified. Voluntary grace is a proposed business policy, not a statutory entitlement. Never grant initial grace to an unpaid first checkout.

Store UTC instants; display SAST for the South African product. Test Jan 31, Feb 28, leap years, month ends, cancellation during webhook delay, device date changes and expiry exactly at the boundary. Provider-issued paid periods prevail over local arithmetic.

## Offline and provider outage policy

- Cached UI membership may remain visible for up to 24 hours, bounded by the verified paid-through/grace end. No indefinite offline premium lease. Use signed server snapshots if offline gating matters; do not claim a patched APK is tamper-proof.
- Server AI/sync actions check current entitlement and usage authority. A provider outage returns a service error, not a failed-card message.
- Checkout, cancellation, billing history and data export remain usable when AI is down. If cancellation cannot be confirmed, show its pending state, preserve the request timestamp and notify support.
- If entitlement service is down, retain local data access, avoid repeated purchase prompts, and preserve an already-started permitted playback session. Do not start new billable cloud work without an authorization decision.

## Required verification matrix

Forged callback; edited R20 amount; foreign UID intent; duplicate 20 times; two simultaneous success notifications; paid then delayed failure; reversal then delayed paid; timeout before/after database commit; missing callback recovered by reconciliation; abandoned browser; TV-account mismatch; month-end renewal; cancellation while charge is in flight; repeat cancellation; failed provider cancel; full/partial refund; account deletion with billing retention; expired/revoked Firebase token; same customer starting on two devices.

Release gate: all deterministic fixtures pass; at least ten permitted low-value end-to-end purchase/activation/cancel/refund exercises with an approved merchant are reconciled, including the provider's supported real-world test path. No customer billing launch with only mocked webhooks. Cancellation, failed renewal and a real renewal schedule must be demonstrated in the supported sandbox/controlled pilot before broad sales.
