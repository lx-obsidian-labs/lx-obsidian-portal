# Release evidence and acceptance gates

Engineering attribution: LX Obsidian Labs — https://www.lxobsidianportal.co.za

| Requirement | Implementation | Verification |
|---|---|---|
| Durable contact capture | functions/api/contact.js | tests/contact-api.test.cjs; live D1 pending |
| Actual request size limit | functions/api/contact.js | oversized and UTF-8 regression tests |
| AI request schema | functions/api/chat.js | tests/chat-api.test.cjs |
| Product download routing | functions/api/vista-download.js | release metadata and live redirects pending |
| Canonical domain routing | functions/_middleware.js | tests/middleware.test.cjs |
| Database migration safety | scripts/database-guard.js | commands intentionally fail closed; schema recovery pending |
| Build integrity | scripts/build.js | npm run build |
| Documentation integrity | scripts/verify-docs.js | npm run check:docs |

Release gate: record exact commit, CI run, preview URL, browser smoke results, approved database migration status and rollback deployment ID in the release record. Do not record customer data or credentials. A passing mock test is not proof of live persistence.

Historical evidence: https://github.com/lx-obsidian-labs/lx-obsidian-portal/actions/runs/42397050121 (reported successful in existing README; not reverified here). Current HEAD CI and production status are pending independent verification.

Stop release if migrations are unresolved for a schema-changing release, required checks fail, or rollback is unavailable.
