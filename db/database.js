const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');

const DB_PATH = path.join(__dirname, 'lx_portal.db');

let db;

function getDb() {
  if (!db) {
    db = new Database(DB_PATH);
    db.pragma('journal_mode = WAL');
    db.pragma('foreign_keys = ON');
    initSchema();
  }
  return db;
}

function initSchema() {
  db.exec(`
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

    CREATE TABLE IF NOT EXISTS team_members (
      id TEXT PRIMARY KEY,
      owner_id TEXT NOT NULL REFERENCES users(id),
      member_id TEXT NOT NULL REFERENCES users(id),
      role TEXT DEFAULT 'member',
      created_at TEXT DEFAULT (datetime('now'))
    );

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

    CREATE TABLE IF NOT EXISTS analytics_events (
      id TEXT PRIMARY KEY,
      user_id TEXT,
      event TEXT NOT NULL,
      data TEXT,
      page TEXT,
      ip TEXT,
      created_at TEXT DEFAULT (datetime('now'))
    );
  `);

  // Seed default AI agents
  const agentCount = db.prepare('SELECT COUNT(*) as count FROM ai_agents').get();
  if (agentCount.count === 0) {
    const agents = [
      ['ai-biz-consultant', 'AI Business Consultant', 'Strategic business advice and analysis', 'briefcase', 'You are a business consultant...'],
      ['ai-sw-architect', 'AI Software Architect', 'System design and architecture guidance', 'code', 'You are a software architect...'],
      ['ai-designer', 'AI Graphic Designer', 'Design concepts and visual direction', 'palette', 'You are a graphic designer...'],
      ['ai-marketer', 'AI Marketing Expert', 'Marketing strategy and campaign ideas', 'trending-up', 'You are a marketing expert...'],
      ['ai-writer', 'AI Content Writer', 'Professional content and copywriting', 'edit', 'You are a content writer...'],
      ['ai-legal', 'AI Legal Assistant', 'Legal guidance and document review', 'shield', 'You are a legal assistant...'],
      ['ai-proposal', 'AI Proposal Writer', 'Business proposal generation', 'file-text', 'You are a proposal writer...'],
      ['ai-seo', 'AI SEO Expert', 'SEO optimization and keyword research', 'search', 'You are an SEO expert...'],
      ['ai-coder', 'AI Coding Assistant', 'Code review and development help', 'terminal', 'You are a coding assistant...'],
      ['ai-teacher', 'AI Teacher', 'Educational tutoring and explanations', 'book-open', 'You are a teacher...']
    ];

    const insertAgent = db.prepare('INSERT INTO ai_agents (id, name, description, icon, prompt_template) VALUES (?, ?, ?, ?, ?)');
    for (const a of agents) {
      insertAgent.run(...a);
    }
  }

  // Seed sample store products
  const productCount = db.prepare('SELECT COUNT(*) as count FROM store_products').get();
  if (productCount.count === 0) {
    const products = [
      ['Website Template - Business Pro', 'Professional business website template with dark theme', 49.99, 'templates', 'digital', null, 1],
      ['Mobile App - CRM Lite', 'Lightweight CRM mobile application', 99.99, 'apps', 'digital', null, 1],
      ['Admin Dashboard Pro', 'Complete admin dashboard with analytics', 79.99, 'dashboards', 'digital', null, 1],
      ['Branding Identity Pack', 'Logo, colors, fonts, and brand guidelines', 149.99, 'branding', 'digital', null, 1],
      ['AI Prompt Engineering Kit', '200+ optimized prompts for business', 29.99, 'ai-tools', 'digital', null, 0],
      ['Business Plan Template', 'Professional business plan with financials', 39.99, 'templates', 'digital', null, 0],
      ['Marketing Kit Pro', 'Social media templates, email campaigns, analytics', 89.99, 'marketing', 'digital', null, 1],
      ['Logo Package - Premium', '5 custom logo concepts with source files', 199.99, 'branding', 'digital', null, 0]
    ];

    const insertProduct = db.prepare('INSERT INTO store_products (id, name, description, price, category, type, file_path, featured) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
    for (const p of products) {
      insertProduct.run(p[0].toLowerCase().replace(/\s+/g, '-'), ...p);
    }
  }

  // Seed academy courses
  const courseCount = db.prepare('SELECT COUNT(*) as count FROM academy_courses').get();
  if (courseCount.count === 0) {
    const courses = [
      ['Python Programming Masterclass', 'Learn Python from scratch to advanced. Covers data structures, OOP, APIs, and more.', 'programming', 'LX Team', 0, 40],
      ['AI & Machine Learning Fundamentals', 'Understand neural networks, NLP, computer vision, and build real AI applications.', 'ai', 'LX Team', 0, 35],
      ['Web Development Bootcamp', 'HTML, CSS, JavaScript, React, Node.js — build full-stack web applications.', 'programming', 'LX Team', 0, 50],
      ['Graphic Design for Developers', 'Learn design principles, Figma, color theory, and create stunning UIs.', 'design', 'LX Team', 0, 25],
      ['Microsoft Office Professional', 'Excel, Word, PowerPoint, Outlook — master the Office suite for business.', 'business', 'LX Team', 0, 20],
      ['Software Architecture & Design', 'Design patterns, microservices, system design, and enterprise architecture.', 'software', 'LX Team', 0, 30],
      ['Digital Marketing Strategy', 'SEO, social media, email marketing, analytics, and growth hacking.', 'business', 'LX Team', 0, 15],
      ['Mobile App Development with React Native', 'Build iOS and Android apps with one codebase using React Native.', 'programming', 'LX Team', 0, 35]
    ];
    const insertCourse = db.prepare('INSERT INTO academy_courses (id, title, description, category, instructor, price, duration_hours) VALUES (?, ?, ?, ?, ?, ?, ?)');
    for (const c of courses) {
      insertCourse.run(c[0].toLowerCase().replace(/\s+/g, '-'), ...c);
    }
  }
}

module.exports = { getDb };
