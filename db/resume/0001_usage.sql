-- LX Obsidian Labs. Daily counters for public CV writing assistance.
CREATE TABLE IF NOT EXISTS resume_ai_usage (
  day INTEGER NOT NULL,
  actor TEXT NOT NULL,
  count INTEGER NOT NULL,
  PRIMARY KEY (day, actor)
);
