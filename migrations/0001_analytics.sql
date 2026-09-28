-- Vista Cinema first-party analytics (Cloudflare D1)
-- Applied with: npx wrangler d1 execute vista-analytics --remote --file=migrations/0001_analytics.sql

CREATE TABLE IF NOT EXISTS vista_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  event TEXT NOT NULL,
  app_version TEXT,
  is_guest INTEGER NOT NULL DEFAULT 0,
  locale TEXT,
  country TEXT,
  payload TEXT NOT NULL DEFAULT '{}',
  received_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vista_events_event ON vista_events(event);
CREATE INDEX IF NOT EXISTS idx_vista_events_received_at ON vista_events(received_at);