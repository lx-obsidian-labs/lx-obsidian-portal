# Vista Cinema - Marketing

> **Current planning authority (29 October 2026):** [`docs/premium-2027/README.md`](docs/premium-2027/README.md). Version 1.0.9 is released; R20/month in January 2027 is a gated, opt-in proposal, not active billing. The old 1.0.6 campaign and referral offer below are historical material. Do not launch the 5-friends/12-month reward: the new plan defers it until funded rewards and qualification exist. The public `/vista` product page is already live; the unpublished item is the separate referral campaign, not that product page. Subscription copy must clearly state the approved free/paid boundary and avoid unverified content or unlimited-AI claims.

Campaign copy, channel strategy, and launch runbooks. Everything here is free to execute: organic
posting on platforms the brand already has, plus Firebase Cloud Messaging.

---

## Where things stand

| | Status |
|---|---|
| 1.0.6 in `deploy/`, staged, not yet pushed to Cloudflare | Ready |
| Global + country FCM topics | Shipped in 1.0.6 |
| Share link fixed, WhatsApp share added | Shipped in 1.0.6 |
| `/vista` referral landing page | Built, **deliberately unpublished** — see below |
| Referral codes, counting, backend | Phase 1, not started |

> **The `/vista` page must not go live yet.** It advertises "5 friends = 12 months free". The app
> has no referral system, so publishing it collects signups against a promise that cannot be kept.
> It ships with Phase 1.

---

## Channel priority

South Africa, in this order. The first is not a close call.

1. **WhatsApp** — dominant, highest trust, and the only channel where a share lands in a private
   conversation rather than a public feed. Private feels personal; public feels like an ad.
2. **Facebook** — where the older install base and family groups live.
3. **TikTok / Instagram Reels** — discovery. Good for reach, weaker for the conversion that
   actually matters here (a referral code entered at signup).
4. **X** — low priority. Not where this audience is.

Every link in every post should point at a specific `/vista` path carrying the poster's referral
code, not the bare homepage. A post with no code gets downloads but earns nothing.

---

## Announcement copy - 1.0.6

Maintenance release. Do not oversell it: the honest headline is that sharing now works, which is
worth saying out loud because it did not before.

### Push (Firebase Cloud Messaging)

Topic: `vista_updates` (global). 1.0.5+ only.

```
title: Sharing works now
body:  Sharing a movie or show in Vista actually sends people to Vista. Tap to update.
data:  { "category": "updates", "check_update": "true" }
```

### Alternatives if that reads flat

```
title: Vista Cinema 1.0.6
body:  Sharing now sends people to the right app. WhatsApp sharing included. Update inside.
```

```
title: Fixed
body:  Sharing a title in Vista was sending people to the wrong site. That's fixed. Tap to update.
```

`check_update: true` is worth keeping — it bypasses the 30-second throttle and takes the viewer
straight into the download.

### After 1.0.6 has been out a while

Once the country topics have real subscribers, the first genuinely market-specific send is the
referral launch. Save the "coming soon" framing for that.

---

## Social copy

### WhatsApp broadcast (primary)

Personal, first person, short. Write it as a viewer, not a company.

```
Hey — if you haven't seen it yet, Vista Cinema is a free streaming app for Android phone and TV.
No subscription, no card. Here's the link: <LINK>

If it helps, grab it and send it to someone who'd use it.
```

**Broadcast vs. individual.** A broadcast gets one view per contact and reads like a brand. A
one-to-one message to 30 real contacts gets a reply and a real install. Volume is cheap either
way, replies are not — do both, favour the one-to-one.

### Facebook

```
Free streaming for Android — phone and smart TV.

Vista Cinema has thousands of movies and series, and right now there's no subscription and no
card needed. It runs on your phone, your tablet, or your TV box.

Download: <LINK>

Know anyone who keeps saying they'll "get round to it"? Send them this.
```

Short version for group posts — one link, one ask, no feature list.

### TikTok / Instagram Reels

Caption:

```
POV: you find out there's a free streaming app and nobody told you. 🇨🇿
Android phone + smart TV. No card.
Link in bio.
```

**Format that actually works here** — 15-30 seconds, screen recording, no talking head:
1. Home screen, scroll the catalogue fast (0-3s)
2. Open a title, hit play (3-8s)
3. Cast to the TV / show the TV build (8-14s)
4. Tap Share → WhatsApp, message pre-written (14-20s)
5. End card: logo, `<LINK>`, "free for now"

Step 4 is the whole campaign in one frame — it shows the mechanic instead of describing it.

---

## Referral campaign (Phase 1 - copy is ready, do not run yet)

**Offer:** Invite 5 friends. Get 12 months of Premium free.

| Friends joined | Reward |
|---|---|
| 1 | Supporter badge |
| 3 | 1 month free |
| 5 | **12 months free** |
| 10 | 12 months + founder badge |

A friend counts when they create an account, set a display name, and watch 10 minutes on a second
day. Held 7 days before counting. One person, one count.

**Why 12 months and not "free forever":** at R20/month a free year is R240 of exposure. Lifetime
is unbounded and can never pay for itself. A bounded reward is fundable; a limitless one is a
liability that grows every time somebody farms it.

### Launch announcement

```
title: Invite 5 friends. Get 12 months free.
body:  Your code's in your profile now. Send it to 5 people and Premium's on us for a year.
```

Post it to `vista_updates_za` first. The offer is priced in rand, so the international audience
gets a different message or none at all.

### Referral nudge (send at day 3, if they have not shared)

```
title: 2 more friends = 1 month free
body:  Pick up where you left off — your invite link's still in your profile.
```

Milestone pings, one per tier. Never batch them; the reward landing is the moment to be present for.

---

## Honesty guardrails

- **Free means free.** Do not write "free" anywhere near "cancel anytime" unless a paid tier is
  actually purchasable. Nothing is on sale yet, so there is nothing to cancel.
- **Do not claim a feature that is not in the shipped build.** The referral programme, Premium
  benefits, and iOS are all announcements, not products. Describe them as coming.
- **Do not advertise a number of titles** unless it was counted. "Thousands" is defensible from the
  catalogue; a specific figure ages badly and is checkable.
- **Say what the app needs**: Android 7.0+, and the user has to allow installs from their browser.
  Pre-empting "it's not on the Play Store" cuts the drop-off more than any feature pitch.

---

## Metrics worth watching

Free, and the only ones that matter here:

| Metric | Why |
|---|---|
| Shares per active viewer | The referral engine's fuel. If this is near zero, nothing downstream works. |
| Install source | Which channel produced installs, from the APK link used |
| Qualified referrals per referrer | The real number. Pending invites are vanity. |
| Cost per qualified referral | Bandwidth only. Should stay in cents. |

Raw aggregate usage (installs, opens, countries) comes from the app's own first-party analytics
instead of Firebase: `GET /api/vista-stats?days=30` with the `VISTA_STATS_KEY` header — per-event,
per-country, per-day, per-version, per-guest-state. It records `app_open`, `playback_started` /
`playback_completed`, each share kind, and the Movie of the Day card + reminder events. No PII,
and the whole pipeline is disabled if the build was made without the key (see `DEPLOYMENT.md`).

Do not track raw install counts as the headline. They measure marketing, not the referral loop.

---

## Runbook: shipping an announcement

1. Copy `deploy/` to Cloudflare Pages. The manifest and APK must land together or the updater
   offers a download that 404s.
2. Send the push to `vista_updates` (all of 1.0.5+), or a country topic for a targeted send.
   Never both for the same message.
3. Same copy into the social channels, within the hour, so the push and the feed agree.
4. Watch for install-source spread in the first 48 hours and adjust the channel mix after that,
   not before.
