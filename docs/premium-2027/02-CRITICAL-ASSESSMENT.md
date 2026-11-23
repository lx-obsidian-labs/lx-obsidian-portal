# Critical assessment of the current product

Evidence: reviewed source on 29 October 2026 and the verified 1.0.9 release. This supersedes stale factual descriptions in the October 23 baseline; it does not retroactively turn the earlier audit into a release report.

## Honest assessment

Vista has a working distribution path and a real conversational AI foundation. It has not yet demonstrated a subscription business. Charging R20 is a change in operations, data durability, permissions and customer expectations, not merely a new paywall screen.

| Finding | Concrete evidence | Consequence | Design response |
|---|---|---|---|
| The launch can announce itself without a launch decision | `app/src/main/java/com/example/vista/ui/components/PremiumAnnouncement.kt:43–47, 111–122`: device-local January 1 date; expired countdown renders “Premium is live” | Wrong timezone/device clock or delayed readiness can produce a false paid promise | Server `launchStatus`, explicit SAST target; date never activates checkout or entitlement |
| Authentication is mistaken for access readiness | `data/session/SessionManager.kt:25–35` and `data/playback/PlaybackAccess.kt:17–27` check guest/auth/network, not subscription | No paid state, expiry, grace or cancellation behaviour | Separate identity, product entitlement and content permission checks |
| Backend has no paid control plane | `backend/src/worker.mjs:114–140`: assistant route, JWT, per-minute limiter only | Any authenticated account can consume provider resources; no durable monthly budget | Server entitlements plus atomic usage reservation/settlement and circuit breakers |
| Assistant grounding is supplied by the client | `ai/AiRepository.kt:44–62`; Worker accepts profile/context strings | Fine for a helpful UI; insufficient as authoritative paid ownership, catalogue facts or child policy | Resolve profile permissions and catalogue facts on server; treat client context as a hint |
| Two sequential generation calls amplify cost/latency | `ai/AiRepository.kt:52,62,118–120`; Worker `generate()` for both modes | Every completed turn normally incurs classifier + answer; both can fall back | Measure total tokens/time per turn, summarize context, deterministic routing only where safe; evaluate one-call/tool loop against baseline |
| Natural-language title preferences are not a full query model | `ai/TasteRecommendations.kt`: movie candidate ranking; `AiRepository.kt:86` passes a text hint | Series, languages and exact runtimes can be ignored; current UI suggestions can overpromise | Typed filters, explicit metadata hydration, unknown values excluded when a hard filter is requested |
| Local account separation is real, cloud continuity is not | `LocalWatchlistRepository.kt`, `LocalHistoryRepository.kt`, `ProfileScope.kt`; Room `AppDatabase.kt` version 6 | Marketing claims about cross-device continuity exceed demonstrated storage | Keep Room; build an explicit outbox, server versions and tombstones before claiming sync |
| Guest/legacy ownership is silently claimed | `ProfileScope.kt:20–25` assigns blank-owner rows to current UID | On a shared device, the next account may claim another person's unspecific legacy data | Explicit one-time import choice and isolated guest namespace; test all upgrade/account-switch paths |
| “Realistic voice” varies by installed engine | `ai/VoiceSession.kt` selects Android voice quality and TTS engine output | A better-scored voice is not measured human preference or universal neural audio | Device matrix, audible previews and fallback; do not sell a guaranteed studio-quality voice |
| Existing voice instrumentation is narrower than the feature | `app/src/androidTest/java/com/example/vista/ai/VoiceLifecycleTest.kt` checks only calls after close | It does not establish live recognition/TTS turn-taking, interruption handling or real microphone release | Expand lifecycle tests with controlled late callbacks and a named physical-device matrix |
| Playback integration is best effort | `data/remote/playback/StreamResolver.kt:29–41` documents rotating providers and source expiry | Cache repair and available metadata cannot establish reliable or authorized streaming | Source-specific reliability instrumentation and rights inventory; distinguish unavailable from broken |
| Profile still labels service as free | `ui/profile/ProfileScreen.kt` SubscriptionCard: “Free - no subscription” | Countdown/profile/website can contradict one another when billing starts | One server product configuration projected into all surfaces |
| Sales promises predate mechanics | `MARKETING.md` and `referral-site/index.html` propose 5 referrals → 12 months | Large unfunded liability and disputed qualification; not supported by a reward ledger | Do not activate that programme for January; bounded future reward with explicit budget |
| Event count is not user count or payment evidence | `data/analytics/VistaAnalytics.kt`: in-memory, best-effort queue; authenticated account count is 22 | Lost telemetry cannot settle purchases or calculate reliable paid cohorts | Durable billing ledger; narrowly scoped measurement with denominators and consent |
| Notification category bypasses preference | `data/remote/firebase/VistaMessagingService.kt:59–69`: all `updates` bypass toggle and request update check | A premium promotion can masquerade as a critical update | Separate essential service/release/security messages from opt-in marketing; never use a mandatory update as a purchase mandate |
| Release/source histories are separate in practice | Android workspace's local history differs from website `master`; signing config exists in old local history | Blind native-folder pushes risk site replacement or secret exposure | Publish selected APK/docs/site paths from current site checkout; no whole-source push or history migration in launch work |

## Features rejected or reduced after criticism

| Initial attractive idea | Strong criticism | Better January feature |
|---|---|---|
| Unlimited conversational AI for R20 | Usage, free-access terms and context growth are unbounded | Visible daily/monthly allocation, measured quality, no charge for system failures |
| Premium neural cloud voice for everyone | At cited character rates, voice alone can exceed R20 | Free installed-device voice; cloud voice stays an opt-in experiment with a separate proven budget |
| Long-term automatic memory | Wrong inferences, shared-account leakage and unclear deletion | Editable explicit taste first; optional memory with provenance, separate consent and deletion |
| Universal playback source ranking | A working link is neither permission nor stable availability | Health by permitted provider/region/version, authorized descriptors and bounded recovery |
| Family profiles and Kids in January | Adds parent controls, content curation, shared-account scope and child-data operations | One account profile and two devices first; Kids research isolated from paid promise |
| Annual plan to lower fees | Improves fee ratio but creates long unearned-service/refund liability before renewal proof | R20 monthly only; assess annual billing after at least two renewal cycles |
| Paywall everything at midnight | Converts a calendar event into surprise loss of customer data | Explicit opt-in purchase; free reading/export of owned library/history and settings retained |

## Priority judgement

**First:** reliable identity/ownership, terms/provider permissions, billing integrity and cost boundaries. **Second:** cross-device library and contextual assistant improvements. **Third:** visual refinement that makes those flows understandable. A new color palette, more animations or additional chat personas does not address the current launch gaps.

## Discovery questions to answer with the existing cohort

Interview at least 8 willing users; report participation counts and exact objections, not percentages alone. Ask them to perform real tasks before asking “would you pay?”

1. Show how you choose something and where the app loses you.
2. What was the last playback failure? Which device/network/source path was involved?
3. What would you lose if you changed phones today?
4. Can you explain what R20 buys, what remains free and when renewal happens?
5. Can you find cancellation without help?
6. Which matters more: reliable resume, better discovery, no first-party ads, or voice?
7. Would a stated assistant allocation meet your actual usage? Observe seven days rather than accepting a guess.

No retention, conversion, payment success or playback reliability level is presently established by the 22-account count.
