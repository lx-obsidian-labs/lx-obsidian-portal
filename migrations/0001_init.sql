-- LX Obsidian Portal — D1 Schema
-- Apply: npx wrangler d1 execute lx-portal --file=./migrations/0001_init.sql
-- Or: npx wrangler d1 migrations apply lx-portal

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
  user_id TEXT NOT NULL,
  token TEXT UNIQUE NOT NULL,
  expires_at TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
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
  project_id TEXT NOT NULL,
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
  project_id TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  status TEXT DEFAULT 'pending',
  due_date TEXT,
  amount REAL,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS project_files (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL,
  name TEXT NOT NULL,
  path TEXT NOT NULL,
  size INTEGER,
  type TEXT,
  uploaded_by TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS project_messages (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  content TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

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
  product_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  rating INTEGER NOT NULL,
  content TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS wishlists (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS orders (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  total REAL NOT NULL,
  status TEXT DEFAULT 'pending',
  payment_method TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS order_items (
  id TEXT PRIMARY KEY,
  order_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  price REAL NOT NULL,
  license_key TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS licenses (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  license_key TEXT UNIQUE NOT NULL,
  status TEXT DEFAULT 'active',
  expires_at TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS subscriptions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  plan TEXT NOT NULL,
  status TEXT DEFAULT 'active',
  amount REAL NOT NULL,
  interval TEXT DEFAULT 'monthly',
  next_billing TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS invoices (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  amount REAL NOT NULL,
  status TEXT DEFAULT 'pending',
  due_date TEXT,
  paid_at TEXT,
  invoice_number TEXT UNIQUE,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS payments (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  invoice_id TEXT,
  amount REAL NOT NULL,
  method TEXT,
  status TEXT DEFAULT 'completed',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS saved_cards (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
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
  user_id TEXT NOT NULL,
  agent_id TEXT NOT NULL,
  title TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ai_messages (
  id TEXT PRIMARY KEY,
  chat_id TEXT NOT NULL,
  role TEXT NOT NULL,
  content TEXT NOT NULL,
  credits_used INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

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
  course_id TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  video_url TEXT,
  duration_minutes INTEGER,
  sort_order INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS enrollments (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  course_id TEXT NOT NULL,
  progress REAL DEFAULT 0,
  completed INTEGER DEFAULT 0,
  certificate_url TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS lesson_completions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  lesson_id TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS assignments (
  id TEXT PRIMARY KEY,
  lesson_id TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  due_days INTEGER,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS assignment_submissions (
  id TEXT PRIMARY KEY,
  assignment_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  content TEXT,
  file_url TEXT,
  grade INTEGER,
  feedback TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS hosting_accounts (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
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
  user_id TEXT NOT NULL,
  domain TEXT NOT NULL,
  issuer TEXT,
  expires_at TEXT,
  status TEXT DEFAULT 'active',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS dns_records (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  domain TEXT NOT NULL,
  type TEXT NOT NULL,
  name TEXT NOT NULL,
  value TEXT NOT NULL,
  ttl INTEGER DEFAULT 3600,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS backups (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  hosting_id TEXT,
  size REAL,
  status TEXT DEFAULT 'completed',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS deployments (
  id TEXT PRIMARY KEY,
  project_id TEXT,
  user_id TEXT NOT NULL,
  version TEXT,
  status TEXT DEFAULT 'pending',
  url TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS team_members (
  id TEXT PRIMARY KEY,
  owner_id TEXT NOT NULL,
  member_id TEXT NOT NULL,
  role TEXT DEFAULT 'member',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS notifications (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  type TEXT,
  title TEXT NOT NULL,
  content TEXT,
  read INTEGER DEFAULT 0,
  link TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS bookings (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  type TEXT NOT NULL,
  title TEXT,
  description TEXT,
  start_time TEXT NOT NULL,
  end_time TEXT NOT NULL,
  status TEXT DEFAULT 'pending',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS affiliates (
  id TEXT PRIMARY KEY,
  user_id TEXT UNIQUE NOT NULL,
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
  affiliate_id TEXT NOT NULL,
  amount REAL NOT NULL,
  status TEXT DEFAULT 'pending',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS support_tickets (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  subject TEXT NOT NULL,
  status TEXT DEFAULT 'open',
  priority TEXT DEFAULT 'normal',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ticket_messages (
  id TEXT PRIMARY KEY,
  ticket_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  content TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS vendor_products (
  id TEXT PRIMARY KEY,
  vendor_id TEXT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  price REAL NOT NULL,
  category TEXT,
  commission_rate REAL DEFAULT 15,
  status TEXT DEFAULT 'pending',
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS analytics_events (
  id TEXT PRIMARY KEY,
  user_id TEXT,
  event TEXT NOT NULL,
  data TEXT,
  page TEXT,
  ip TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Seed AI agents
INSERT OR IGNORE INTO ai_agents (id, name, description, icon, prompt_template, price_per_credit, status) VALUES
  ('agent-1', 'LX Code Assistant', 'Helps write and debug code', 'code', 'You are a senior software engineer...', 1, 'active'),
  ('agent-2', 'LX Marketing Guru', 'Generates marketing copy and strategies', 'megaphone', 'You are a marketing expert...', 1, 'active'),
  ('agent-3', 'LX Data Analyst', 'Analyzes data and creates visualizations', 'chart', 'You are a data scientist...', 2, 'active'),
  ('agent-4', 'LX Business Coach', 'Provides business advice and planning', 'briefcase', 'You are a business consultant...', 2, 'active');

-- Seed store products
INSERT OR IGNORE INTO store_products (id, name, description, price, category, type, featured, status, rating, sales_count) VALUES
  ('prod-1', 'LX Starter Theme', 'A modern, responsive HTML template perfect for startups', 29.99, 'templates', 'digital', 1, 'active', 4.5, 120),
  ('prod-2', 'LX Pro Dashboard', 'Advanced admin dashboard with charts and analytics', 49.99, 'dashboards', 'digital', 1, 'active', 4.8, 85),
  ('prod-3', 'LX AI Chat Integration', 'Add AI-powered chat to any website', 79.99, 'plugins', 'digital', 1, 'active', 4.2, 45),
  ('prod-4', 'LX E-commerce Kit', 'Complete e-commerce solution with payment processing', 99.99, 'plugins', 'digital', 0, 'active', 4.6, 30),
  ('prod-5', 'LX Portfolio Builder', 'Beautiful portfolio template for creatives', 19.99, 'templates', 'digital', 0, 'active', 4.3, 200),
  ('prod-6', 'LX Security Suite', 'Security tools including SSL, firewall, and monitoring', 149.99, 'security', 'digital', 0, 'active', 4.9, 60);

-- Seed academy courses
INSERT OR IGNORE INTO academy_courses (id, title, description, category, instructor, price, duration_hours, status) VALUES
  ('course-1', 'Web Development Bootcamp', 'Learn full-stack web development from scratch', 'Development', 'Alex Rivera', 199.99, 40, 'published'),
  ('course-2', 'UI/UX Design Fundamentals', 'Master the art of user interface design', 'Design', 'Sarah Chen', 149.99, 30, 'published'),
  ('course-3', 'Cloud Architecture on AWS', 'Deploy scalable applications on Amazon Web Services', 'Cloud', 'Marcus Johnson', 249.99, 35, 'published'),
  ('course-4', 'AI & Machine Learning Basics', 'Introduction to artificial intelligence and ML', 'AI', 'Dr. Priya Patel', 299.99, 45, 'published');

-- Seed course lessons
INSERT OR IGNORE INTO course_lessons (id, course_id, title, description, duration_minutes, sort_order) VALUES
  ('lesson-1', 'course-1', 'Introduction to HTML', 'Learn the basics of HTML5', 45, 1),
  ('lesson-2', 'course-1', 'CSS Styling', 'Master CSS layouts and animations', 60, 2),
  ('lesson-3', 'course-1', 'JavaScript Fundamentals', 'Core JS concepts', 90, 3),
  ('lesson-4', 'course-2', 'Design Principles', 'Learn color theory and typography', 50, 1),
  ('lesson-5', 'course-2', 'Wireframing', 'Create effective wireframes', 40, 2),
  ('lesson-6', 'course-3', 'AWS Basics', 'Overview of AWS services', 55, 1),
  ('lesson-7', 'course-4', 'What is AI?', 'History and fundamentals', 35, 1),
  ('lesson-8', 'course-4', 'Neural Networks', 'Understanding deep learning', 65, 2);

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
