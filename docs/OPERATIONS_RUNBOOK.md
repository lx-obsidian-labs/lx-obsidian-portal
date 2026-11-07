# LX Obsidian Portal — Operations Runbook

**Reviewed:** 2026-11-07 · **Status:** Operational instructions documented; production execution not verified.

## Scope and deployment topology

The repository is a company marketing website with Cloudflare-oriented deployment tooling. The root manifest declares a build pipeline, Wrangler Pages development/deploy commands, D1 migration and seed commands, and Node test scripts. The README identifies a Cloudflare Pages Functions contact endpoint and D1-backed lead storage. Confirm the exact production project, routes and bindings before changes.

## Local verification

Use the checked-in npm lockfile and a compatible Node.js version.

```bash
npm ci
npm run test
npm run check
npm run build
```

The build script itself invokes `npm run check`. These are **available commands**, not evidence of passing results.

For local Cloudflare Pages emulation, the manifest defines `npm run dev`. Check local Wrangler bindings and configuration before running.

## Release procedure

1. Identify target commit and compare with the last known working release.
2. Confirm all required Pages and D1 bindings, API secrets and notification destinations **by name**, not by printing values.
3. Run tests, verification and build. Save outputs and CI run URLs.
4. Validate the generated `dist/` and check that private files are excluded.
5. Deploy only after reviewing production target and obtaining the appropriate release approval. The declared `npm run deploy` command targets the Wrangler Pages project `lxobsidianmainsite`.
6. Smoke-test homepage, navigation, product pages, mobile layout, contact form, database persistence and email/notification delivery using controlled test data.
7. Verify security headers, redirects and any downloadable resources.
8. Record release SHA, deployment URL, test timestamp and outcome.

## Database changes

The manifest exposes `db:migrate` and `db:seed` using Wrangler D1 execute. **Do not run either command against a production database without first reviewing SQL, target environment, backup/restore plan and idempotency.** Schema migration is not the same as a harmless local test.

## Incident response

- **Contact form fails:** Verify HTTP status, rate limits, validation, Cloudflare Function logs and D1 binding. Do not log message bodies or personal information unnecessarily.
- **Lead stored but no notification:** Check provider credentials, configured recipient and provider errors. Confirm that no lead is silently lost.
- **Site inaccessible:** Check Cloudflare deployment status, DNS, routing and latest changes.
- **Regression after release:** Revert to the last verified deployment using the hosting platform's documented rollback mechanism. Preserve evidence of the incident.
- **Security incident:** Rotate affected credentials, restrict access and follow [portfolio security reporting](https://github.com/lx-obsidian-labs/lx-obsidian-labs/blob/main/SECURITY.md).

## Evidence record

```md
Release SHA:
Reviewer / approver:
CI run URL and conclusion:
Deployment ID and URL:
D1 migration/version:
Form submission test (synthetic reference only):
Notification receipt verified:
Rollback target:
Open issues:
```

[README](../README.md) · [Lead delivery verification](LEAD_DELIVERY_VERIFICATION.md)
