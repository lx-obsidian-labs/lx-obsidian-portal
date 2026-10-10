-- LX Obsidian Labs — 0001 AI platform schema
-- D1 database: lx-obsidian-db

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password TEXT,
  name TEXT,
  company TEXT,
  phone TEXT,
  role TEXT DEFAULT 'customer',
  credits INTEGER DEFAULT 0,
  avatar TEXT,
  created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE TABLE IF NOT EXISTS ai_agents (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  status TEXT DEFAULT 'active',
  created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE TABLE IF NOT EXISTS ai_chats (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  agent_id TEXT,
  title TEXT,
  created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE TABLE IF NOT EXISTS ai_messages (
  id TEXT PRIMARY KEY,
  chat_id TEXT NOT NULL,
  role TEXT DEFAULT 'user',
  content TEXT NOT NULL,
  created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

INSERT OR IGNORE INTO ai_agents (id, name, description, status) VALUES
  ('agent-lx-builder', 'LX Builder', 'Generates complete web apps from a prompt', 'active'),
  ('agent-lx-qa', 'LX QA', 'Reviews and debugs generated and existing code', 'active'),
  ('agent-lx-ux', 'LX UX', 'Refines layouts, spacing and design polish', 'active');