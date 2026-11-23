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

`node send_fcm_cli.mjs --send` uses the existing Firebase CLI login for the **1.0.10 available** announcement to `vista_updates`. It refuses validation/broadcast until the public manifest advertises 17/1.0.10 with hash and size. It is data-only, optional and not a forced update. Without `--send`, FCM only validates. The festive coming-soon campaign was already accepted once via `send_festive_fcm.mjs`; do not repeat either campaign as a deployment check. See `docs/PUBLIC_RELEASE_1_0_10.md`.

## 1.0.10 agent actions

New clients send `agentic: true`; legacy clients retain the old plan/destination allowlist. The new contract adds details, download review, watchlist lookup, actual resume and exact-title comparison, with optional bounded 1-based selection from recent results. Android supplies catalog-backed routes and confirmation; the model never returns executable routes or performs arbitrary actions. Action receipts are carried into subsequent conversation history.

`node --test` verifies both contracts. `node backend/probe-agentic.mjs` from the workspace root exercises the actual NVIDIA plan contract with fictional profile/results, without app mutations or notifications. Deploy source and verify authenticated legacy/new requests as part of the public-release sequence.
