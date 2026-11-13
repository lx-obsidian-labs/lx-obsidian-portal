# Database recovery and migration readiness

Engineering attribution: LX Obsidian Labs — https://www.lxobsidianportal.co.za

Wrangler declares DB bound to lx-obsidian-db. Previous package scripts targeted lx-portal and referenced missing migration files. Migration and seed commands intentionally fail closed until the authoritative schema is recovered.

## Recovery procedure
1. Authorised operator confirms the production binding in Cloudflare Pages and identifies the database by ID privately.
2. Export schema and migration history using approved read-only operations; store exports privately, never commit customer records.
3. Compare tables, indexes, triggers and constraints with all function queries. Contact requires project_leads and notification_status.
4. Reconstruct versioned migrations from the authoritative schema, test against a disposable local database, then a separate preview database.
5. Record backup and restore evidence, approve migration and only then restore migration commands using the verified database target.

Do not infer the complete schema from a single endpoint. Do not seed production. This procedure has not yet been executed.
