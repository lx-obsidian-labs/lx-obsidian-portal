# Contact API reference

Engineering attribution: LX Obsidian Labs — https://www.lxobsidianportal.co.za

Source: functions/api/contact.js. POST /api/contact accepts JSON with an allowed Origin. Required fields: name (2–120 chars), email (valid format, max 254), subject (4–180), message (15–4000), projecttype and industry (nonempty, max 120). Optional: business_name, phone, budget, launch, nda (boolean), company_website (honeypot). Strings are trimmed and truncated. Email lowercased.

Success: HTTP 201, JSON {ok:true, reference:"LX-...", saved:true}. Honeypot returns 200 with reference RECEIVED and does not persist. Errors: 400 malformed/invalid input, 403 origin, 405 method, 413 declared content length above 12000, 415 content type, 503 missing DB or failed write. Responses are no-store.

D1 DB binding writes project_leads with parameterised SQL. Optional HTTPS LEADS_WEBHOOK_URL and LEADS_WEBHOOK_TOKEN trigger asynchronous notification; notification status updated sent/failed. A saved lead does not guarantee notification delivery. Do not expose webhook secrets or real lead data.

Limitations: Content-Length alone does not bound chunked bodies; origin checks are not bot authentication. Verify rate limiting, retention, access control and schema before production. Tests use mocked storage, not live D1.
