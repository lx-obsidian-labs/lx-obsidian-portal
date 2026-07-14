-- LX Obsidian Portal — Seed Data
-- Run after migration 0001: npx wrangler d1 execute lx-portal --file=migrations/0002_seed.sql

-- Seed AI Agents
INSERT OR IGNORE INTO ai_agents (id, name, description, icon, prompt_template, price_per_credit) VALUES
  ('ai-biz-consultant', 'AI Business Consultant', 'Strategic business advice and analysis', 'briefcase', 'You are a business consultant...', 1),
  ('ai-sw-architect', 'AI Software Architect', 'System design and architecture guidance', 'code', 'You are a software architect...', 1),
  ('ai-designer', 'AI Graphic Designer', 'Design concepts and visual direction', 'palette', 'You are a graphic designer...', 1),
  ('ai-marketer', 'AI Marketing Expert', 'Marketing strategy and campaign ideas', 'trending-up', 'You are a marketing expert...', 1),
  ('ai-writer', 'AI Content Writer', 'Professional content and copywriting', 'edit', 'You are a content writer...', 1),
  ('ai-legal', 'AI Legal Assistant', 'Legal guidance and document review', 'shield', 'You are a legal assistant...', 1),
  ('ai-proposal', 'AI Proposal Writer', 'Business proposal generation', 'file-text', 'You are a proposal writer...', 1),
  ('ai-seo', 'AI SEO Expert', 'SEO optimization and keyword research', 'search', 'You are an SEO expert...', 1),
  ('ai-coder', 'AI Coding Assistant', 'Code review and development help', 'terminal', 'You are a coding assistant...', 1),
  ('ai-teacher', 'AI Teacher', 'Educational tutoring and explanations', 'book-open', 'You are a teacher...', 1);

-- Seed Store Products
INSERT OR IGNORE INTO store_products (id, name, description, price, category, type, featured) VALUES
  ('website-template-business-pro', 'Website Template - Business Pro', 'Professional business website template with dark theme', 49.99, 'templates', 'digital', 1),
  ('mobile-app-crm-lite', 'Mobile App - CRM Lite', 'Lightweight CRM mobile application', 99.99, 'apps', 'digital', 1),
  ('admin-dashboard-pro', 'Admin Dashboard Pro', 'Complete admin dashboard with analytics', 79.99, 'dashboards', 'digital', 1),
  ('branding-identity-pack', 'Branding Identity Pack', 'Logo, colors, fonts, and brand guidelines', 149.99, 'branding', 'digital', 1),
  ('ai-prompt-engineering-kit', 'AI Prompt Engineering Kit', '200+ optimized prompts for business', 29.99, 'ai-tools', 'digital', 0),
  ('business-plan-template', 'Business Plan Template', 'Professional business plan with financials', 39.99, 'templates', 'digital', 0),
  ('marketing-kit-pro', 'Marketing Kit Pro', 'Social media templates, email campaigns, analytics', 89.99, 'marketing', 'digital', 1),
  ('logo-package-premium', 'Logo Package - Premium', '5 custom logo concepts with source files', 199.99, 'branding', 'digital', 0);

-- Seed Academy Courses
INSERT OR IGNORE INTO academy_courses (id, title, description, category, instructor, price, duration_hours) VALUES
  ('python-programming-masterclass', 'Python Programming Masterclass', 'Learn Python from scratch to advanced. Covers data structures, OOP, APIs, and more.', 'programming', 'LX Team', 0, 40),
  ('ai-machine-learning-fundamentals', 'AI & Machine Learning Fundamentals', 'Understand neural networks, NLP, computer vision, and build real AI applications.', 'ai', 'LX Team', 0, 35),
  ('web-development-bootcamp', 'Web Development Bootcamp', 'HTML, CSS, JavaScript, React, Node.js — build full-stack web applications.', 'programming', 'LX Team', 0, 50),
  ('graphic-design-for-developers', 'Graphic Design for Developers', 'Learn design principles, Figma, color theory, and create stunning UIs.', 'design', 'LX Team', 0, 25),
  ('microsoft-office-professional', 'Microsoft Office Professional', 'Excel, Word, PowerPoint, Outlook — master the Office suite for business.', 'business', 'LX Team', 0, 20),
  ('software-architecture-design', 'Software Architecture & Design', 'Design patterns, microservices, system design, and enterprise architecture.', 'software', 'LX Team', 0, 30),
  ('digital-marketing-strategy', 'Digital Marketing Strategy', 'SEO, social media, email marketing, analytics, and growth hacking.', 'business', 'LX Team', 0, 15),
  ('mobile-app-react-native', 'Mobile App Development with React Native', 'Build iOS and Android apps with one codebase using React Native.', 'programming', 'LX Team', 0, 35);

-- Seed Demo Coupon
INSERT OR IGNORE INTO coupons (id, code, discount_type, discount_value, max_uses) VALUES
  ('coupon-welcome10', 'WELCOME10', 'percentage', 10, 100);
