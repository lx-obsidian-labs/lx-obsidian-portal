-- LX Obsidian Portal — D1 Database Schema
-- Run: npx wrangler d1 execute lx-portal --file=migrations/0001_initial.sql

-- Users & Auth
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password TEXT NOT NULL,
  name TEXT NOT NULL,
  company TEXT,
  phone TEXT,
  avatar TEXT,
  role TEXT DEFAULT 'customer',
  credits INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS sessions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  token TEXT UNIQUE NOT NULL,
  expires_at TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Projects & Workspace
CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  title TEXT NOT NULL,
  description TEXT,
  status TEXT DEFAULT 'active',
  budget REAL,
  deadline TEXT,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS tasks (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL REFERENCES projects(id),
  title TEXT NOT NULL,
  description TEXT,
  status TEXT DEFAULT 'todo',
  assignee TEXT,
  priority TEXT DEFAULT 'medium',
  due_date TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS milestones (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL REFERENCES projects(id),
  title TEXT NOT NULL,
  description TEXT,
  status TEXT DEFAULT 'pending',
  due_date TEXT,
  amount REAL,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS project_files (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL REFERENCES projects(id),
  name TEXT NOT NULL,
  path TEXT NOT NULL,
  size INTEGER,
  type TEXT,
  uploaded_by TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS project_messages (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL REFERENCES projects(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  content TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Store / Marketplace
CREATE TABLE IF NOT EXISTS store_products (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  price REAL NOT NULL,
  category TEXT,
  type TEXT DEFAULT 'digital',
  file_path TEXT,
  featured INTEGER DEFAULT 0,
  status TEXT DEFAULT 'active',
  rating REAL DEFAULT 0,
  sales_count INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS product_reviews (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL REFERENCES store_products(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  rating INTEGER NOT NULL,
  content TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS wishlists (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  product_id TEXT NOT NULL REFERENCES store_products(id),
  created_at TEXT DEFAULT (datetime('now'))
);

-- Orders & Licenses
CREATE TABLE IF NOT EXISTS orders (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  total REAL NOT NULL,
  status TEXT DEFAULT 'pending',
  payment_method TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS order_items (
  id TEXT PRIMARY KEY,
  order_id TEXT NOT NULL REFERENCES orders(id),
  product_id TEXT NOT NULL REFERENCES store_products(id),
  price REAL NOT NULL,
  license_key TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS licenses (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  product_id TEXT NOT NULL REFERENCES store_products(id),
  license_key TEXT UNIQUE NOT NULL,
  status TEXT DEFAULT 'active',
  expires_at TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Billing
CREATE TABLE IF NOT EXISTS subscriptions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  plan TEXT NOT NULL,
  status TEXT DEFAULT 'active',
  amount REAL NOT NULL,
  interval TEXT DEFAULT 'monthly',
  next_billing TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS invoices (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  amount REAL NOT NULL,
  status TEXT DEFAULT 'pending',
  due_date TEXT,
  paid_at TEXT,
  invoice_number TEXT UNIQUE,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS payments (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  invoice_id TEXT REFERENCES invoices(id),
  amount REAL NOT NULL,
  method TEXT,
  status TEXT DEFAULT 'completed',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS saved_cards (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  last_four TEXT NOT NULL,
  brand TEXT,
  expiry_month INTEGER,
  expiry_year INTEGER,
  is_default INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS coupons (
  id TEXT PRIMARY KEY,
  code TEXT UNIQUE NOT NULL,
  discount_type TEXT DEFAULT 'percentage',
  discount_value REAL NOT NULL,
  max_uses INTEGER,
  used_count INTEGER DEFAULT 0,
  expires_at TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

-- AI Hub
CREATE TABLE IF NOT EXISTS ai_agents (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  icon TEXT,
  prompt_template TEXT,
  price_per_credit INTEGER DEFAULT 1,
  status TEXT DEFAULT 'active',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ai_chats (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  agent_id TEXT NOT NULL REFERENCES ai_agents(id),
  title TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ai_messages (
  id TEXT PRIMARY KEY,
  chat_id TEXT NOT NULL REFERENCES ai_chats(id),
  role TEXT NOT NULL,
  content TEXT NOT NULL,
  credits_used INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Academy / LMS
CREATE TABLE IF NOT EXISTS academy_courses (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  category TEXT,
  instructor TEXT,
  price REAL DEFAULT 0,
  thumbnail TEXT,
  duration_hours REAL,
  status TEXT DEFAULT 'published',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS course_lessons (
  id TEXT PRIMARY KEY,
  course_id TEXT NOT NULL REFERENCES academy_courses(id),
  title TEXT NOT NULL,
  description TEXT,
  video_url TEXT,
  duration_minutes INTEGER,
  sort_order INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS enrollments (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  course_id TEXT NOT NULL REFERENCES academy_courses(id),
  progress REAL DEFAULT 0,
  completed INTEGER DEFAULT 0,
  certificate_url TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS lesson_completions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  lesson_id TEXT NOT NULL REFERENCES course_lessons(id),
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS assignments (
  id TEXT PRIMARY KEY,
  lesson_id TEXT NOT NULL REFERENCES course_lessons(id),
  title TEXT NOT NULL,
  description TEXT,
  due_days INTEGER,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS assignment_submissions (
  id TEXT PRIMARY KEY,
  assignment_id TEXT NOT NULL REFERENCES assignments(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  content TEXT,
  file_url TEXT,
  grade INTEGER,
  feedback TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Hosting
CREATE TABLE IF NOT EXISTS hosting_accounts (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  domain TEXT,
  plan TEXT,
  storage_used REAL DEFAULT 0,
  storage_limit REAL DEFAULT 5120,
  bandwidth_used REAL DEFAULT 0,
  bandwidth_limit REAL DEFAULT 10240,
  status TEXT DEFAULT 'active',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ssl_certificates (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  domain TEXT NOT NULL,
  issuer TEXT,
  expires_at TEXT,
  status TEXT DEFAULT 'active',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS dns_records (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  domain TEXT NOT NULL,
  type TEXT NOT NULL,
  name TEXT NOT NULL,
  value TEXT NOT NULL,
  ttl INTEGER DEFAULT 3600,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS backups (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  hosting_id TEXT REFERENCES hosting_accounts(id),
  size REAL,
  status TEXT DEFAULT 'completed',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS deployments (
  id TEXT PRIMARY KEY,
  project_id TEXT REFERENCES projects(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  version TEXT,
  status TEXT DEFAULT 'pending',
  url TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Team
CREATE TABLE IF NOT EXISTS team_members (
  id TEXT PRIMARY KEY,
  owner_id TEXT NOT NULL REFERENCES users(id),
  member_id TEXT NOT NULL REFERENCES users(id),
  role TEXT DEFAULT 'member',
  created_at TEXT DEFAULT (datetime('now'))
);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  type TEXT,
  title TEXT NOT NULL,
  content TEXT,
  read INTEGER DEFAULT 0,
  link TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Bookings
CREATE TABLE IF NOT EXISTS bookings (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  type TEXT NOT NULL,
  title TEXT,
  description TEXT,
  start_time TEXT NOT NULL,
  end_time TEXT NOT NULL,
  status TEXT DEFAULT 'pending',
  created_at TEXT DEFAULT (datetime('now'))
);

-- Affiliates
CREATE TABLE IF NOT EXISTS affiliates (
  id TEXT PRIMARY KEY,
  user_id TEXT UNIQUE NOT NULL REFERENCES users(id),
  referral_code TEXT UNIQUE NOT NULL,
  commission_rate REAL DEFAULT 20,
  balance REAL DEFAULT 0,
  total_earned REAL DEFAULT 0,
  clicks INTEGER DEFAULT 0,
  signups INTEGER DEFAULT 0,
  sales INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS affiliate_payouts (
  id TEXT PRIMARY KEY,
  affiliate_id TEXT NOT NULL REFERENCES affiliates(id),
  amount REAL NOT NULL,
  status TEXT DEFAULT 'pending',
  created_at TEXT DEFAULT (datetime('now'))
);

-- Support
CREATE TABLE IF NOT EXISTS support_tickets (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  subject TEXT NOT NULL,
  status TEXT DEFAULT 'open',
  priority TEXT DEFAULT 'normal',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ticket_messages (
  id TEXT PRIMARY KEY,
  ticket_id TEXT NOT NULL REFERENCES support_tickets(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  content TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Vendor Marketplace
CREATE TABLE IF NOT EXISTS vendor_products (
  id TEXT PRIMARY KEY,
  vendor_id TEXT NOT NULL REFERENCES users(id),
  name TEXT NOT NULL,
  description TEXT,
  price REAL NOT NULL,
  category TEXT,
  commission_rate REAL DEFAULT 15,
  status TEXT DEFAULT 'pending',
  created_at TEXT DEFAULT (datetime('now'))
);

-- Analytics
CREATE TABLE IF NOT EXISTS analytics_events (
  id TEXT PRIMARY KEY,
  user_id TEXT,
  event TEXT NOT NULL,
  data TEXT,
  page TEXT,
  ip TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_projects_user ON projects(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_project ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_licenses_user ON licenses(user_id);
CREATE INDEX IF NOT EXISTS idx_invoices_user ON invoices(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_chats_user ON ai_chats(user_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_user ON enrollments(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings(user_id);
