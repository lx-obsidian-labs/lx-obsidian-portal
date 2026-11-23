# Vista Watch 1.0.13: AI entertainment retrieval and spoken conversation

VersionCode **20**. Development work; not a published APK or backend deployment.

## Implemented

- Chat/Entertainment Brief sections, compact welcome, preferences sheet, selectable answers,
  Copy/Read aloud, source cards with publisher dates and links, contextual news followups.
- Session-only recent conversations, draft preservation across screen recreation, owner reset,
  cancellation, and explicit jump-to-latest instead of always moving a reader's scroll position.
- Entertainment-news retrieval uses NewsData's HTTP API with a server-only `X-ACCESS-KEY`.
  Two fixed feeds (English/global and English/South Africa), bounded response sizes, article
  normalization/deduplication, seven-day freshness, lexical reranking, five-source evidence limit.
- News provider excerpts are untrusted user-message data, not system instructions. Source links
  come from retrieval rather than model output. Current-news questions abstain without evidence.
- Edge cache: 15 minutes for snapshots, one minute for provider failure. This is **per data centre**,
  not a global quota ledger, distributed single-flight lock, or guaranteed daily credit cap.
- ElevenLabs official JavaScript SDK installed in `backend/`, using its narrow TTS resource import
  to avoid Node-only playback wrappers in Workers. Deployment bundles dependencies with esbuild.
- Authenticated `/api/voice`, independent six-requests/minute/user limiter, bounded text/audio,
  cancellation, `audio/mpeg` response, no public audio cache and no credential in the APK.
- Android spoken turns: device speech recognition → existing Vista AI/tool flow → ElevenLabs
  audio → resume listening after playback. Android TTS is an explicit alternative, not a silent fallback. Stop/background/profile
  changes invalidate pending audio; temporary audio is deleted after completion or cancellation.
- Long spoken responses point users to the remaining on-screen text. Citation markers and URLs
  are removed from speech. The interface exposes Stop voice and provider status.

## Configuration

Onboarding owns the gitignored root `.env.local`. Do not print or commit it. `loadEnvironment()`
loads `.env.deploy`, `.env`, then `.env.local`. Worker secrets are `ELEVENLABS_API_KEY` and
`NEWSDATA_API_KEY`. The existing pasted NewsData key format is accepted during migration;
conventional `NEWSDATA_API_KEY=...` is preferred. The default voice is George and the speech model
is `eleven_flash_v2_5`. Neither NewsData nor ElevenLabs credentials are Android build constants.

`node backend/deploy.mjs` bundles the Worker and uploads its secret bindings. Wrangler deployments
need those secrets configured separately. Both deployment paths require `VOICE_LIMITER`.

## Still required before public rollout

- Device microphone/audio testing and a user-approved live ElevenLabs synthesis test.
- Provider entitlement/latency validation; mock tests do not establish credit availability.
- Durable article IDs/snapshots for long-lived story conversations and notification deep links.
- Scheduled editorial selection, global ingestion budget, opt-in news topics/quiet hours,
  idempotent FCM delivery ledger and a notification-to-brief route.
- General bounded multi-tool orchestration beyond existing catalog actions and news retrieval.
- Persistent conversation history, full-duplex interruption/barge-in and TV-specific voice UX.

The new voice path is foreground turn-taking, not a managed ElevenLabs Conversational Agent.
It deliberately retains Vista's current catalog actions and news grounding. No live speech test
or unsolicited news broadcast should be run as a static verification step.
