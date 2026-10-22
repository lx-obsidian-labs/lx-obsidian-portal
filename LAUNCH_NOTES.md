# Workflow-focused website refresh

Prepared against master e4540d8e65de752726ace37b6acd13abb776a74e.

## What changed

- A responsive company homepage with focused service copy, existing product imagery, a labelled platform concept and direct project/partner/early-access routes.
- A dedicated platform page distinguishes planned app connections from the existing AI builder.
- A single accessible enquiry form replaces the wizard that acknowledged submissions without sending them. Failed requests retain the brief; success is shown only after persistence.
- Project, partnership and beta enquiries are stored in D1. The full brief is saved, rather than personal details being logged.
- Newsletter failures are surfaced instead of being acknowledged as successful signups.
- Generic social links removed and contact links no longer labelled Login.
- Removed homepage pricing and availability guarantees that were not substantiated in this review.
- The build includes the platform page and new styles. The service-worker version is advanced.

## Apply the patch

Run from a clean checkout of the repository at the base commit above:

```sh
git switch -c improve/workflow-site-refresh
git apply --check /path/to/lx-obsidian-refresh.patch
git apply /path/to/lx-obsidian-refresh.patch
npm test
npm run build
```

Review the diff and commit the changes before opening a pull request. The source folder in the ZIP contains changed files only; it is not a standalone repository or deployable site.

## Required before production rollout

The Cloudflare Pages project must have the existing D1 database bound as `DB`. Apply the subscriber and enquiry tables to the database bound to the target environment:

```sh
npx wrangler d1 execute lx-obsidian-db --remote --file=migrations/0002_subscribers.sql
npx wrangler d1 execute lx-obsidian-db --remote --file=migrations/0003_enquiries.sql
```

These additive statements use IF NOT EXISTS. Confirm the target database matches the Pages binding before running them. Preview environments need their own DB binding and these tables too. Without them, the forms deliberately return an error rather than claiming that details were received.

The `db:migrate` command now targets the configured database and the actual migrations directory. `db:init` names the existing AI schema bootstrap accurately; this refresh does not require rerunning that schema.

Enquiries are persisted, not emailed. Monitor the `enquiries` table in Cloudflare D1; only authorised operators should access it. A monitored inbox notification or authenticated lead dashboard remains a follow-up. Subscriber removal is manual through an authorised operator; the footer describes contacting the company to unsubscribe.

## Validation

- `npm test`: five backend tests cover persistence, normalisation, invalid input, and unavailable/failed storage.
- `npm run build`: passes.
- Subscriber/enquiry SQL applied successfully to an in-memory SQLite database; a complete enquiry was inserted and read back.
- Local links, image paths, labels and unique IDs in the three refreshed built pages checked.
- Browser visual QA could not run in this environment because the Chromium download failed. Before merging, inspect desktop and mobile layouts, keyboard focus, the three enquiry routes, submission success/failure and newsletter handling in a Cloudflare preview.
- No production forms were submitted; no production data or deployment was modified.

## Scope

The connected AI workspace and Gmail/GitHub/Cloudflare/Shopify integrations remain planned. The refreshed pages market that direction without claiming those workflows already execute. Existing product pages remain intact apart from correcting generic social/contact links.
