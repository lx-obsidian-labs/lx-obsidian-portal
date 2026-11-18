-- Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
-- Counts only. No CV text, contact details or raw IP addresses.
CREATE TABLE IF NOT EXISTS resume_ai_quota (
  day TEXT NOT NULL,
  bucket TEXT NOT NULL,
  used INTEGER NOT NULL,
  PRIMARY KEY (day, bucket)
);
