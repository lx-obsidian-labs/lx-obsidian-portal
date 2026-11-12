# Product endpoint reference

Engineering attribution: LX Obsidian Labs — https://www.lxobsidianportal.co.za

## Synapse chat
Source: functions/api/chat.js. POST /api/chat; OPTIONS supported. JSON fields: message, mode (chat, generate, refine, explain), provider (default nvidia), model, code, history, temperature. Actual request body limit 1 MiB (413 on excess). Message limit 6000 characters; code limit 200000. Temperature defaults to 0.4 unless numeric in [0,2]. History is truncated by mode. Successful responses include reply, artifact (nullable), provider and model. Generation can return template fallback when providers fail. Non-generation provider failures return 502 or 503; malformed JSON and invalid schema return 400. Provider credentials are NVIDIA_API_KEY and OPENROUTER_API_KEY. CORS is not authentication. Provider attempts have a 15-second timeout; a 25-second aggregate upstream deadline is enforced; quota enforcement remains outstanding. Never treat generated output as trusted code or fallback sample data as real metrics.

## Vista download
Source: functions/api/vista-download.js. GET or HEAD /api/vista-download?platform=android or tv; default android. Returns 302 to allowlisted GitHub asset CDN or public release URL. Invalid platform returns 400; other methods 405. GitHub lookup timeout 10 seconds; CDN redirect cache 120 seconds. APK bytes are not proxied. Current source pins v1.0.10; verify release metadata and checksum before updating. Update asset IDs, filenames and version together with vista-version.json and download-page checks.

## Contact
See [contact API reference](CONTACT_API_REFERENCE.md). This reference covers inspected endpoints only, not an exhaustive API inventory.
