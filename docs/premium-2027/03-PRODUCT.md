# Product specification: Vista 1.1

Proposed customer promise: **“R20 a month for a more personal, connected cinema companion.”** Pricing is ZAR, one account, cancel future renewals from the app. Do not market premium as film ownership, universal licensed streaming access, or unlimited model usage.

## Product boundary

| Capability | Free/expired account | Proposed R20 member |
|---|---|---|
| Browse/search/metadata and manual local watchlist | Retained where provider terms permit | Retained |
| Existing library/history read, local export, profile/security and cancellation | Always accessible; no hostage data | Accessible |
| Cross-device library/progress | Existing cached data readable; new paid sync can pause after expiry, without deleting data | One profile, two registered devices; sync with visible status |
| Cloud assistant | Proposed trial: 10 successful turns total, 3/day; stop if abuse/cost test fails | Proposed 150 successful turns per billing period, max 10/day; subject to cost/usage validation before promise |
| Device read-aloud/hands-free input | Device capability, not a paid artificial restriction; cloud reply allowance still applies | Same voice engine; allowance covers cloud turns |
| First-party promotional ads | Existing free policy accurately disclosed | Suppressed after entitlement; third-party embeds are not promised ad-free |
| Playback | Only where authorized, available and supported | Same permissions; subscription never grants third-party content rights |
| Support | Published basic contact route | Billing/account support with staffed response target |

The owner must approve this free/paid boundary after interviews by **12 November**. It deliberately replaces “the entire app becomes inaccessible” with a defined recurring product. Do not announce these proposed quotas until pricing, production provider and measured costs pass.

## P0 feature specifications

### F01 — honest membership and account center

Screens: Profile → Membership → Compare plans, Checkout, Payment pending, Active, Renewal issue, Cancel renewal, Cancel confirmed, Receipts/support. Account center retains password reset, signed-in identity, data export/deletion and device management.

- Show R20/month, currency, tax wording appropriate to the merchant, renewal date, what is included, quota reset, cancellation and refund link before purchase.
- One server product record drives Android, TV, website and AI knowledge. States: `planned`, `beta`, `live`, `paused`; launch date is informational.
- Display local time with a timezone label for billing dates; backend stores UTC.
- Checkout pending must remain pending until server verification. An app restart or deep link is not proof of payment.
- A member can cancel renewal in at most three meaningful interactions after opening Membership. No mandatory retention offer or support ticket.
- On TV, offer a short-lived QR/pairing checkout link usable on a phone; confirm on TV from server state. Never ask the user to enter a card through the TV remote.

Acceptance: server-state fixtures render correctly across phone/TV; device clock shifts do not activate membership; double-tapping Pay creates one checkout intent; cancellation preserves paid-through access and prevents a future charge once confirmed.

### F02 — dependable cross-device library

- One account profile for January, two devices; no household-subprofile selector yet.
- Watchlist and Continue Watching show `Saved on this device`, `Syncing`, `Synced`, or actionable `Needs attention`.
- Keep local functionality while disconnected. Signing into another account never displays the previous account's cached content.
- Explicit guest import explains which rows move. Dismissing import leaves guest records separate.
- Removing a title on one device must not reappear from a stale device; intentional “watch again” can lower progress without being overwritten by a max-position merge.

Acceptance: two-device conflict/deletion/account-switch fixtures in architecture doc pass; migration from every shipped Room schema preserves data; 95% of measured online sync operations converge within 30 seconds in the pilot.

### F03 — paid features that fail gracefully

- Provider/network failure does not say “upgrade” or consume a successful-turn allowance.
- “Restore access” re-fetches server entitlements; it does not trust a local receipt flag.
- An expired account can cancel, export, read its library and get billing help without a cloud AI turn.
- Show a specific reason for `quota exhausted`, `billing pending`, `offline`, `provider unavailable`, or `requires newer app`.

Acceptance: decline/timeout/refund/token refresh/entitlement service outage all have a distinct tested UI state; a server outage never becomes an instruction to pay twice.

## P1 features: meaningful intelligence

### F04 — contextual assistant, not another static chat box

Typed context includes route, selected title identity, media type, episode boundary, spoiler state and stable result references. “The second one” resolves against a retained result set, not a guessed title name. A request to open a page yields an allowlisted action; destructive or financial actions are never executed from prose.

Desired tasks: “a comedy under 90 minutes”, “series rather than movies”, “open storage settings”, “why did you choose this?”, “save the second result”, “what does my membership include?”. Missing runtime/language is unknown, not a passing filter. Pure app help/usage/cancellation shortcuts remain deterministic and available during provider failure.

Acceptance: versioned evaluation set in architecture/operations docs; no fabricated tool success; user/account/content policy cannot be changed by a prompt; assistant cannot purchase, cancel or refund on the user's behalf without entering the deterministic account flow.

### F05 — voice that respects the user

Visible state machine: `idle → permission → listening → transcribing → working → speaking → ready`, plus `paused`, `interrupted`, `error`. A single start enables foreground turns; stop/background/dismissal revokes capture. Supply voice preview, language, installed-engine label, network requirement and a “use device default” fallback.

No wake-word/background-listening service for January. No cloud voice upcharge surprise. Capture stops while the assistant speaks. “Stop listening” ends capture; a visible Stop remains available while speech recognition cannot hear the user.

Acceptance: permission denial/revocation, calls/audio-focus loss, headphones, noisy room, TTS errors, repeated silence, rotation and activity exit tested on representative physical devices. Text remains usable. Human voice preference results name the actual engine/voice/device; quality is not inferred from an API enum.

### F06 — playback confidence

Measure time to first frame, startup outcome, resumed-position error, source expiry, retries and failure reason for permitted direct/embed paths separately. Show source-specific progress and a bounded Repair action; preserve position; never rotate providers to bypass authorization denial. “Unavailable” is a valid outcome, not a repair failure hidden from the user.

Acceptance: conditions and denominators in the delivery gates. Do not infer movie availability from TMDB metadata or a successful HTML fetch.

## UX acceptance across all features

Keep the five primary tabs. FAB opens floating chat; no surprise tab switch. Prefer an accessible dismissible sheet on compact phones and a bounded panel on tablets. Test TalkBack names, 48dp targets, 200% font size, reduced motion, focus restoration, low-memory recreation and a TV D-pad. Readable errors and status matter more than new gradients.

## Explicitly deferred from the January promise

Vista Kids launch, multiple household profiles, offline video downloads, broad social features, referral reward automation, annual billing, multiple payment gateways, self-hosted GPUs, biometric voice identity and unlimited cloud TTS. None is a dependency of a sound R20 membership.
