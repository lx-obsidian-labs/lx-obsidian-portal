const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/dashboard', auth, function (req, res) {
  const db = getDb();
  const userId = req.user.id;
  const projects = db.prepare('SELECT COUNT(*) as count FROM projects WHERE user_id = ?').get(userId);
  const orders = db.prepare('SELECT COUNT(*) as count FROM orders WHERE user_id = ?').get(userId);
  const licenses = db.prepare('SELECT COUNT(*) as count FROM licenses WHERE user_id = ? AND status = ?').get(userId, 'active');
  const subscriptions = db.prepare('SELECT COUNT(*) as count FROM subscriptions WHERE user_id = ? AND status = ?').get(userId, 'active');
  const tickets = db.prepare('SELECT COUNT(*) as count FROM support_tickets WHERE user_id = ? AND status = ?').get(userId, 'open');
  const invoices = db.prepare('SELECT * FROM invoices WHERE user_id = ? ORDER BY created_at DESC LIMIT 5').all(userId);
  const recentProjects = db.prepare('SELECT * FROM projects WHERE user_id = ? ORDER BY updated_at DESC LIMIT 5').all(userId);
  const notifications = db.prepare('SELECT * FROM notifications WHERE user_id = ? AND read = 0 ORDER BY created_at DESC LIMIT 10').all(userId);
  const totalSpent = db.prepare('SELECT COALESCE(SUM(amount), 0) as total FROM payments WHERE user_id = ? AND status = ?').get(userId, 'completed');
  const uploadedFiles = db.prepare('SELECT COUNT(*) as count FROM project_files WHERE uploaded_by = ?').get(userId);
  res.json({ projects, orders, licenses, subscriptions, tickets, invoices, recentProjects, notifications, totalSpent: totalSpent.total, uploadedFiles });
});

router.get('/projects', auth, function (req, res) {
  const db = getDb();
  const projects = db.prepare('SELECT * FROM projects WHERE user_id = ? ORDER BY created_at DESC').all(req.user.id);
  res.json({ projects });
});

router.post('/projects', auth, function (req, res) {
  const { title, description, budget, deadline } = req.body;
  if (!title) return res.status(400).json({ error: 'Title is required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO projects (id, user_id, title, description, budget, deadline) VALUES (?, ?, ?, ?, ?, ?)').run(id, req.user.id, title, description || null, budget || null, deadline || null);
  res.json({ project: { id, title, description, budget, deadline, status: 'active' } });
});

router.get('/projects/:id', auth, function (req, res) {
  const db = getDb();
  const project = db.prepare('SELECT * FROM projects WHERE id = ? AND user_id = ?').get(req.params.id, req.user.id);
  if (!project) return res.status(404).json({ error: 'Project not found' });
  const tasks = db.prepare('SELECT * FROM tasks WHERE project_id = ? ORDER BY created_at DESC').all(req.params.id);
  const milestones = db.prepare('SELECT * FROM milestones WHERE project_id = ? ORDER BY due_date ASC').all(req.params.id);
  const files = db.prepare('SELECT * FROM project_files WHERE project_id = ? ORDER BY created_at DESC').all(req.params.id);
  const messages = db.prepare('SELECT m.*, u.name as user_name FROM project_messages m JOIN users u ON m.user_id = u.id WHERE m.project_id = ? ORDER BY m.created_at ASC').all(req.params.id);
  const deployments = db.prepare('SELECT * FROM deployments WHERE project_id = ? ORDER BY created_at DESC').all(req.params.id);
  res.json({ project, tasks, milestones, files, messages, deployments });
});

router.post('/projects/:id/tasks', auth, function (req, res) {
  const { title, description, priority, due_date, assignee } = req.body;
  if (!title) return res.status(400).json({ error: 'Title is required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO tasks (id, project_id, title, description, priority, due_date, assignee) VALUES (?, ?, ?, ?, ?, ?, ?)').run(id, req.params.id, title, description || null, priority || 'medium', due_date || null, assignee || null);
  res.json({ task: { id, title, description, priority, due_date, status: 'todo' } });
});

router.put('/tasks/:id/status', auth, function (req, res) {
  const { status } = req.body;
  const db = getDb();
  db.prepare('UPDATE tasks SET status = ? WHERE id = ?').run(status, req.params.id);
  res.json({ ok: true });
});

router.post('/projects/:id/milestones', auth, function (req, res) {
  const { title, description, due_date, amount } = req.body;
  if (!title) return res.status(400).json({ error: 'Title is required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO milestones (id, project_id, title, description, due_date, amount) VALUES (?, ?, ?, ?, ?, ?)').run(id, req.params.id, title, description || null, due_date || null, amount || null);
  res.json({ milestone: { id, title, description, due_date, amount, status: 'pending' } });
});

router.post('/projects/:id/messages', auth, function (req, res) {
  const { content } = req.body;
  if (!content) return res.status(400).json({ error: 'Content is required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO project_messages (id, project_id, user_id, content) VALUES (?, ?, ?, ?)').run(id, req.params.id, req.user.id, content);
  res.json({ message: { id, content, user_id: req.user.id } });
});

router.post('/projects/:id/files', auth, function (req, res) {
  const { name, path: filePath, size, type } = req.body;
  if (!name) return res.status(400).json({ error: 'Name is required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO project_files (id, project_id, name, path, size, type, uploaded_by) VALUES (?, ?, ?, ?, ?, ?, ?)').run(id, req.params.id, name, filePath || '', size || 0, type || '', req.user.id);
  res.json({ file: { id, name, size, type } });
});

router.get('/licenses', auth, function (req, res) {
  const db = getDb();
  const licenses = db.prepare('SELECT l.*, p.name as product_name FROM licenses l JOIN store_products p ON l.product_id = p.id WHERE l.user_id = ? ORDER BY l.created_at DESC').all(req.user.id);
  res.json({ licenses });
});

router.get('/subscriptions', auth, function (req, res) {
  const db = getDb();
  const subs = db.prepare('SELECT * FROM subscriptions WHERE user_id = ? ORDER BY created_at DESC').all(req.user.id);
  res.json({ subscriptions: subs });
});

router.get('/support', auth, function (req, res) {
  const db = getDb();
  const tickets = db.prepare('SELECT * FROM support_tickets WHERE user_id = ? ORDER BY created_at DESC').all(req.user.id);
  res.json({ tickets });
});

router.post('/support', auth, function (req, res) {
  const { subject, message } = req.body;
  if (!subject) return res.status(400).json({ error: 'Subject is required' });
  const db = getDb();
  const ticketId = uuidv4();
  db.prepare('INSERT INTO support_tickets (id, user_id, subject) VALUES (?, ?, ?)').run(ticketId, req.user.id, subject);
  if (message) {
    const msgId = uuidv4();
    db.prepare('INSERT INTO ticket_messages (id, ticket_id, user_id, content) VALUES (?, ?, ?, ?)').run(msgId, ticketId, req.user.id, message);
  }
  res.json({ ticket: { id: ticketId, subject, status: 'open' } });
});

router.get('/messages', auth, function (req, res) {
  const db = getDb();
  const msgs = db.prepare(`
    SELECT DISTINCT m.*, u.name as user_name, p.title as project_title 
    FROM project_messages m 
    JOIN users u ON m.user_id = u.id 
    JOIN projects p ON m.project_id = p.id 
    WHERE p.user_id = ? 
    ORDER BY m.created_at DESC 
    LIMIT 50
  `).all(req.user.id);
  res.json({ messages: msgs });
});

module.exports = router;
