-- Project briefs, partnership enquiries and platform early-access interest.
CREATE TABLE IF NOT EXISTS enquiries (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  intent TEXT NOT NULL CHECK (intent IN ('project', 'partnership', 'beta')),
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  company TEXT NOT NULL DEFAULT '',
  subject TEXT NOT NULL,
  message TEXT NOT NULL,
  details TEXT NOT NULL DEFAULT '{}',
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_enquiries_intent_created ON enquiries (intent, created_at);
