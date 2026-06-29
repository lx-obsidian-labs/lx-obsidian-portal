const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/accounts', auth, function (req, res) {
  const db = getDb();
  const accounts = db.prepare('SELECT * FROM hosting_accounts WHERE user_id = ?').all(req.user.id);
  const ssl = db.prepare('SELECT * FROM ssl_certificates WHERE user_id = ?').all(req.user.id);
  const dns = db.prepare('SELECT * FROM dns_records WHERE user_id = ?').all(req.user.id);
  const backups = db.prepare('SELECT * FROM backups WHERE user_id = ? ORDER BY created_at DESC LIMIT 10').all(req.user.id);
  res.json({ accounts, ssl, dns, backups });
});

router.post('/accounts', auth, function (req, res) {
  const { domain, plan } = req.body;
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO hosting_accounts (id, user_id, domain, plan) VALUES (?, ?, ?, ?)').run(id, req.user.id, domain || null, plan || 'basic');
  res.json({ account: { id, domain, plan, status: 'active' } });
});

router.get('/uptime', auth, function (req, res) {
  const db = getDb();
  const accounts = db.prepare('SELECT id, domain, status FROM hosting_accounts WHERE user_id = ?').all(req.user.id);
  const uptime = accounts.map(a => ({ id: a.id, domain: a.domain, status: a.status, uptime_percentage: 99.9, last_checked: new Date().toISOString() }));
  res.json({ uptime });
});

router.get('/deployments', auth, function (req, res) {
  const db = getDb();
  const deployments = db.prepare('SELECT d.*, p.title as project_name FROM deployments d LEFT JOIN projects p ON d.project_id = p.id WHERE d.user_id = ? ORDER BY d.created_at DESC').all(req.user.id);
  res.json({ deployments });
});

router.post('/deployments', auth, function (req, res) {
  const { project_id, version } = req.body;
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO deployments (id, project_id, user_id, version, status, url) VALUES (?, ?, ?, ?, ?, ?)').run(id, project_id || null, req.user.id, version || '1.0.0', 'deploying', `https://${req.user.id}-${id}.lxportal.dev`);
  setTimeout(function () {
    db.prepare('UPDATE deployments SET status = ? WHERE id = ?').run('live', id);
  }, 3000);
  res.json({ deployment: { id, version, status: 'deploying' } });
});

module.exports = router;
