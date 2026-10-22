-- Newsletter subscribers — written by POST /api/subscribe (Liquid Glass footer).
--
-- Provision with:
--   npx wrangler d1 create <name>
--   npx wrangler d1 execute <name> --remote --file=migrations/0002_subscribers.sql
-- and bind the database as DB (see wrangler.toml).
--
-- Failed writes return an error; a signup is confirmed only after persistence.

CREATE TABLE IF NOT EXISTS subscribers (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  email      TEXT NOT NULL UNIQUE,
  source     TEXT NOT NULL DEFAULT 'footer',
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_subscribers_created_at ON subscribers (created_at);
