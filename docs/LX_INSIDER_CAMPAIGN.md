# LX Insider — Beta Tester Rewards Programme (FCM-led recruitment)

**Slogan:** Test. Shape. Earn. Repeat.
**Owner:** Community Growth / QA Recruitment, LX Obsidian Labs.
**Primary product:** LX Pocket Business (`com.obsidianlabs.lxpocketbusiness`).
**Targets:** 100 applicants → 30 active testers short-term → 100 reliable testers long-term; ≥12 continuously opted-in testers for ≥14 days where Google Play requires it; 90-day retention programme.

Recruitment lead channel is **Firebase Cloud Messaging to existing LX Obsidian Labs app users** (Vista Watch install base), per management direction. Other channels support it. Everything here is organic; no purchased testers, no incentivised reviews, no metric manipulation.

---

## 1. What exists now

| Asset | Location | State |
|---|---|---|
| FCM recruitment sender (3 waves, validate-by-default) | `send_insider_recruitment_fcm.mjs` | Built, unit-tested |
| Sender tests | `send_insider_recruitment_fcm.test.mjs` | `node --test send_insider_recruitment_fcm.test.mjs` |
| Notification deep-link support (`action_url`, allowlisted, TV-safe fallback) | `app/.../util/NotificationLinks.kt`, `VistaMessagingService.kt` | Built, unit-tested; **ships in the next app release** |
| Landing page + application form + share kit | `campaign/insider/index.html` | Built; **must be published to the portal repo at `/insider`** |
| Tester feedback / bug report form | `campaign/feedback/index.html` | Built; publish at `/insider-feedback` |
| Recruitment dashboard (register → daily report) | `campaign/insider-dashboard.mjs` + `.test.mjs` | Built, unit-tested |
| Social artwork (feed / story / wide), SVG + PNG exports | `campaign/artwork/insider-*.svg`, `.png` | Built and dimension-verified |
| Social copy kit (FB, LinkedIn, WhatsApp, TikTok, Reddit, campuses) | `campaign/social/posts.md` | Ready; post only after Gates A/B |
| Email templates (invite, welcome, challenge, reward, re-engagement) | `campaign/emails.md` | Ready |
| Weekly challenge schedule + operator checklist | `campaign/weekly-challenges.md` | Ready; dates set at intake open |
| Tester register template | `campaign/insider-register.csv` | Header only; no real data yet |
| Reward approval ledger | `campaign/reward-ledger.csv` | Header only; amounts blank until management approval |
| Retention programme (30/60/90, re-engagement) | `campaign/retention.md` | Ready |
| Community outreach plan + tracker | `campaign/community-outreach.md` | Ready; permissions still to be obtained |
| This plan | `docs/LX_INSIDER_CAMPAIGN.md` | Current |

### Verified facts (23 November 2026)

- `https://www.lxobsidianportal.co.za/pocket-business` → **HTTP 200**, live, states Android 8.0+, free/ad-supported, beta by email request.
- `https://play.google.com/apps/testing/com.obsidianlabs.lxpocketbusiness` → reachable but redirects to **Google sign-in**. Reachability ≠ active programme. Must be confirmed with a real, non-enrolled Google account before any installation instructions are distributed.
- `https://www.lxobsidianportal.co.za/insider` → **404**. The sender refuses to broadcast until this page is live (preflight verification).
- Vista Watch FCM topics: `vista_updates` (all), `vista_updates_za` (South Africa), `vista_updates_row` (rest of world). Promotions-category messages respect the in-app notification toggles and OS permission; announcements persist in the in-app bell.

---

## 2. BEFORE IT GOES LIVE TO USERS — launch gates

Do not run `--send` until every gate is green. Order matters.

### Gate A — Management approvals (blocking)
- [ ] Approve cross-promotion of LX Pocket Business to Vista Watch users via the promotions channel.
- [ ] Approve the rewards framework (§6) and any budget before any reward type is publicly confirmed. No amounts, prizes or employment are promised anywhere until signed off.
- [ ] Confirm intake dates (wave 1 send date, intake close date) and the onboarding group size (suggested: 10–15 per group).
- [ ] Confirm who operates the register and replies to applications (response SLA: 2 working days).

### Gate B — Destinations live and verified (blocking)
- [ ] Publish `campaign/insider/index.html` to the portal repo (`lx-obsidian-labs/lx-obsidian-portal`) as `/insider`, and `campaign/feedback/index.html` as `/insider-feedback`; deploy via its Cloudflare Pages workflow; confirm HTTP 200 and that the pages contain “LX Insider”.
- [ ] Test both forms end-to-end on a real Android phone: validation, mailto composition, copy fallback, share buttons, consent wording.
- [ ] Verify the Play testing link with a **non-enrolled Google account**: it must show the opt-in page for `com.obsidianlabs.lxpocketbusiness`. Confirm the closed track exists, the tester email list is the mechanism used, and capacity ≥ planned intake.
- [ ] Confirm the 12-tester / 14-day closed-testing requirement applicability for the developer account and plan opt-in tracking accordingly.

### Gate C — App behaviour (blocking for deep links, non-blocking for send)
- [ ] Release a Vista Watch build containing `NotificationLinks` + `action_url` handling (next release; current 1.0.12 does not deep-link).
- [ ] Device QA: notification tap opens the browser on phone; on Android TV without a browser it falls back to opening the app; older builds fall back to the app and the bell entry. The apply address is always printed in the body, so the campaign works even before the new build rolls out.
- [ ] Confirm promotions toggles: users with promotions disabled will not see the push (by design — do not bypass).

### Gate D — Compliance and data (blocking)
- [ ] POPIA: form collects only programme-necessary data; consent captured; access/correction/deletion route (`info@lxobsidianportal.co.za`) published.
- [ ] Register stored access-controlled (not a public sheet); emails used only for Play track enrolment and programme comms.
- [ ] Copy audit: no monetary amounts, no guaranteed prizes, no employment claims, no review/rating incentives (the sender enforces this automatically).
- [ ] Community rules checked for every secondary channel before posting.

### Gate E — Dry run (blocking)
- [ ] `node send_insider_recruitment_fcm.mjs --wave=1 --audience=za` → preflight passes and FCM returns `validated_only`.
- [ ] Management signs off the exact printed title/body.
- [ ] Send once: `node send_insider_recruitment_fcm.mjs --wave=1 --audience=za --send`. Record the FCM message name in §9 log. **Never re-send a wave as a test.**

---

## 3. Push campaign runbook

Audience default is **South Africa first** (`vista_updates_za`). `row`/`global` sends need separate approval after wave results are reviewed.

| Wave | When | ID | Purpose |
|---|---|---|---|
| 1 | Launch day (Tue–Thu, 18:00–20:00 SAST) | `lx-insider-recruit-2026-w1` | Invitation |
| 2 | +4 days, only if intake still open | `lx-insider-recruit-2026-w2` | Reassurance + reminder; tells applicants not to re-apply |
| 3 | +9 days, final reminder | `lx-insider-recruit-2026-w3` | Last call |

Rules:
- Max 3 recruitment pushes per intake; collapse keys prevent stacking; TTLs 3d/2d/1d.
- Topic broadcasts cannot exclude people who already applied — wave 2/3 copy handles this explicitly.
- FCM acceptance is not delivery or conversion. Measure applications, not sends.
- After each wave, update the daily report (§8) and decide whether the next wave is still needed. If 30 active testers are reached early, stop the series.

Commands:
```
node --test send_insider_recruitment_fcm.test.mjs          # unit tests
node send_insider_recruitment_fcm.mjs --wave=1 --audience=za          # validate only
node send_insider_recruitment_fcm.mjs --wave=1 --audience=za --send   # broadcast once
```
The sender refuses `--send` without explicit `--wave` and `--audience`, verifies the product page, the `/insider` page and the Play link first, checks the 4 KB payload limit, and blocks forbidden claims.

---

## 4. Application → onboarding flow

1. Applicant submits the `/insider` form (mailto application; consent captured in the text).
2. Operator logs them in `campaign/insider-register.csv` as **Interested** (or **Prospect** if sourced but not applied).
3. Eligibility check: Android 8.0+ device, Play account email matches device, availability, consent.
4. Reply with welcome email (§7 kit) → status **Consent received** only after explicit consent is on file.
5. Add the Google account to the closed testing track email list → **Added to Google Play**.
6. Send the Play testing link + install instructions (only after Gate B verification) → track **Opted in**, then **Application installed** (confirmed by the tester’s first feedback or screenshot).
7. First assignment completed → **Actively testing**.

Statuses (exact): Prospect, Invited, Interested, Consent received, Added to Google Play, Opted in, Application installed, Actively testing, Testing completed, Reward approved, Inactive.
**Never mark anyone Actively testing without verification.** Opted-in dates are recorded so the 12-tester/14-day requirement can be evidenced. `node campaign/insider-dashboard.mjs --validate` rejects unverified active statuses, missing consent, duplicate IDs, bad dates and rewards without points before any report is filed.

Form fields (implemented on the page): full name; Google Play email; country; device model; Android version; testing experience; weekly availability; preferred categories; source channel; participation consent (required); communications consent (optional). Never request passwords, banking details or ID documents.

---

## 5. Testing assignment pack (LX Pocket Business)

All testing uses **fictional business records** (e.g. “Mzansi Cleaning Co.”, invented customers and amounts). Each assignment: objective, steps, expected result, what to report (screenshot + reproduction steps + device/Android version).

1. Install & onboarding — Play opt-in, install, first launch, permissions.
2. Navigation & usability — find every core screen without help.
3. Income & expenses — record, edit, delete entries; totals correct.
4. Customer management — add/edit customers; search; validation.
5. Debt tracking — record amounts owed; part-payments; balances.
6. Invoices & quotations — create, edit, PDF/share output correctness.
7. Offline functionality — airplane mode: core records still work; clear behaviour for online-only features.
8. Backup & restore — export, wipe/reinstall or restore path, data integrity.
9. Performance — cold start, large record sets, scrolling, low-end device behaviour.
10. Accessibility — TalkBack, font scaling, contrast, touch targets.
11. Error handling & stability — invalid inputs, interrupted operations, rotation, process death.
12. Regression — re-test previously reported bugs after each new build.

Weekly challenges (weeks 3–6) pick one assignment each, with a submission deadline and a short feedback form: assignment ID, device, Android version, steps taken, result, screenshots, severity, reproducible yes/no.

---

## 6. Rewards framework (pending Gate A approval)

Principle: rewards pay for **verified work**, never for reviews, ratings or installs.

- **Points:** assignment completed = 2; reproducible bug accepted = 3; high-impact/crash bug = 5; useful usability feedback = 1; weekly challenge on time = +1 bonus. Points are logged per tester in the register.
- **Tiers (90 days):** Insider (any verified work) → Contributor (≥10 points) → Core Insider (≥25 points + 4 consecutive active weeks).
- **Reward types, subject to approval and availability:** early access to upcoming LX apps; eligible premium features free for 3 months where available; gift vouchers; airtime/data; cash only for separately approved, contracted testing assignments; public recognition (with consent); invitation to future paid testing projects.
- **No published amounts** until management approves the budget. Every reward is logged (`reward_points`, `reward_approved`) and reported as expenditure.
- Community testers are volunteers; paid contractors are a separate, approved arrangement. No employment advertising.

---

## 7. Message kit (secondary channels — post only after Gate A/B)

**WhatsApp (community groups, with admin permission):**
> LX Obsidian Labs is looking for Android beta testers for LX Pocket Business — a free app for income, expenses, customer debts and invoices. Volunteer 1–2 hours a week, send honest feedback, earn rewards for verified testing work. Android 8.0+, Google Play account needed. Apply: https://www.lxobsidianportal.co.za/insider — Test. Shape. Earn. Repeat.

**Facebook / LinkedIn post:**
> South African software needs South African testers. LX Insider is our new beta testing community, starting with LX Pocket Business (business recordkeeping, offline-first, Android). No experience needed — just an Android 8.0+ phone, a Google Play account and honest feedback. Rewards recognise real testing work, not reviews. Voluntary, free to join. Apply: https://www.lxobsidianportal.co.za/insider

**TikTok script (30–40s):** Hook: “Your phone can help build South African software — and get rewarded for it.” → show Pocket Business screens (fictional data) → “Join LX Insider: test early builds, report what breaks, earn rewards for verified work.” → requirements on screen (Android 8.0+, Play account, honest feedback) → CTA: link in bio / type lxobsidianportal.co.za/insider. No money amounts on screen.

**Email invitation (to applicants who consented to comms):** subject “You’re invited: LX Insider beta intake” — what the programme is, what week 1 looks like, the consent step, the Play link only after verification, and the unsubscribe line.

**Welcome email (after consent):** subject “Welcome to LX Insider — your first steps” — status, install instructions, assignment 1, how to submit feedback, confidentiality reminder, fictional-data rule, support contact.

---

## 8. Daily recruitment report (template)

Date / intake day N:
- New applicants (by source channel) · total applicants vs 100 target
- Confirmed testers (consent received) · added to Play · opted in · installed · actively testing vs 30 target
- Continuous opt-in count and days enrolled (12-tester/14-day evidence)
- Feedback items received · bugs reported · bugs verified reproducible
- Assignment completion rate (completed / assigned)
- Rewards approved and expenditure (only after budget approval)
- Channel performance: applications per channel; push waves sent vs applications attributed (“Vista Watch notification” source field)
- Drop-offs / inactive reasons · recommended improvements for tomorrow

Vanity metrics (sends, impressions, installs without activity) are reported only as context, never as success.

Generate from the register (counts only, no identities):
```
node campaign/insider-dashboard.mjs --validate                    # register integrity gate
node campaign/insider-dashboard.mjs --markdown                    # daily report
node campaign/insider-dashboard.mjs --date=2026-12-01             # JSON
node --test campaign/insider-dashboard.test.mjs                   # unit tests
```
The dashboard computes the funnel, channel attribution, verified work, reward totals, retention alerts (inactivity, day-30/60/90 check-ins, recognition) and the 12-tester/14-day opt-in evidence from `opted_in_at` dates. Approved rewards are additionally recorded in `campaign/reward-ledger.csv`.

---

## 9. Send log

| Date | Wave | Audience | FCM message name | Result |
|---|---|---|---|---|
| — | — | — | — | No sends yet. Dry run only. |

---

## 10. Remaining backlog (after launch gates)

1. ~~Community outreach plan~~ — built (`campaign/community-outreach.md`); permissions and per-community rule checks still required before posting.
2. ~~Retention programme~~ — built (`campaign/retention.md`) with dashboard triggers and re-engagement template.
3. ~~Reward ledger~~ — built (`campaign/reward-ledger.csv`); no amounts until management approval.
4. ~~Register validation~~ — built (`--validate`); run before every daily report.
5. Optional: managed form backend (replaces mailto) once application volume justifies it.
