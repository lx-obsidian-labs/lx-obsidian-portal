# Vista assistant

Android calls the authenticated Cloudflare Worker. **The NVIDIA key stays in a Worker secret; it is never returned to the app or compiled into the APK.**

## Deployment

Use Node 22. Process environment is loaded first, then root `.env.deploy`, then root `.env` takes precedence so the project's saved key is used. Values are parsed with Node's dotenv parser, including quoted values. `NVIDIA_NIM_API_KEY` is also accepted as an alias for `NVIDIA_API_KEY`.

Required variables: `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`, `NVIDIA_API_KEY`. Optional: `NVIDIA_MODEL`. See `.env.example`.

```powershell
node backend/deploy.mjs --models
node backend/deploy.mjs --provider-check --model=nvidia/nemotron-3.5-lightning-30b-a3b
node backend/deploy.mjs
node backend/smoke.mjs --require-nvidia
```

Verify a candidate with `--provider-check` before setting `NVIDIA_MODEL`; appearing in `/models` does not establish authorization or contract compatibility. This probe checks an actual completion and parses the app action contract. Deployment uploads `NVIDIA_API_KEY` as a `secret_text` binding. Wrangler deployments instead require `wrangler secret put NVIDIA_API_KEY`.

The deployed endpoint is `https://vista-assistant.feedpatelmusic.workers.dev`; Android's `VISTA_AI_BASE_URL` points there. Firebase ID tokens and per-user rate limiting protect both request modes. Cloud AI requires sign-in; it does not silently impersonate a guest or expose the key.

## Conversation and actions

The 1.0.10 client negotiates `agentic: true` for catalog-backed Details, download review, Watch List lookup, actual Resume and exact-title comparison. Numbered follow-ups are bounded to current results. Legacy clients retain their original allowlist. Android constructs routes and performs confirmed app operations; the model does not run arbitrary code or certify playback/download availability.

- `mode: plan` returns a validated catalog action or an allowlisted page destination.
- Android resolves catalog metadata and prepares title cards and navigation actions.
- `mode: reply` generates a natural answer using recent conversation, catalog context, profile alias and enabled taste preferences. Email, passwords and tokens are not included in the profile context.
- App instructions live in `src/app-guide.mjs`; update them when Vista's features change.
- Page actions open native routes; title searches can open Search with the query filled in. Watchlist modifications still use the app's confirmation action.
- NVIDIA is attempted first. Failures use the Cloudflare Workers AI binding. Every response includes the provider actually used. `/health` reports secret configuration, not proof of provider health.

## Verification status

An inherited environment credential initially returned **403 Authorization failed**. Project `.env` precedence fixes that shadowing. With the project's saved key, **`nvidia/nemotron-3.5-lightning-30b-a3b` passed a live HTTP 200 completion and the page-action contract**, and is the configured default. Other tested Nemotron models were unavailable to this account. The old `meta/llama-3.3-70b-instruct` model is retired. Do not switch based solely on the model listing.

The deployed NVIDIA endpoint passed authenticated checks for page routing, app knowledge, profile name and follow-up conversation with `--require-nvidia` (fallback explicitly rejected). The Cloudflare fallback was also verified separately. The smoke script creates and deletes a temporary Firebase identity.

## Voice

The free Android text-to-speech engine chooses the highest-quality installed voice for the device language, with a locale fallback. Voice quality depends on the installed engine; Settings → AI & Voice opens Android's voice/download settings. No premium TTS subscription is added.

Tap the microphone once to start foreground hands-free turn-taking. Listening pauses during generation and speech, then resumes after speech completes. Say “stop listening” or tap Stop to end. Leaving/dismissing chat stops the session. Repeated silence or recognition errors pause it. Physical-device checks are needed for microphone permissions, acoustic behavior and engine-specific voice quality.

## Announcements

The release sender now targets **optional 1.0.10 availability** and refuses to send until the public manifest has version 17/1.0.10, hash and size. The festive coming-soon greeting was already accepted once. Do not repeat announcements as deployment checks. APK installation still requires Android user approval; FCM acceptance is not proof of delivery to every device.
