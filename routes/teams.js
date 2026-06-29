const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/', auth, function (req, res) {
  const db = getDb();
  const members = db.prepare(`
    SELECT tm.*, u.name, u.email, u.avatar, u.role as user_role 
    FROM team_members tm 
    JOIN users u ON tm.member_id = u.id 
    WHERE tm.owner_id = ?
    ORDER BY tm.created_at DESC
  `).all(req.user.id);
  res.json({ members });
});

router.post('/invite', auth, function (req, res) {
  const { email, role } = req.body;
  if (!email) return res.status(400).json({ error: 'Email is required' });
  const db = getDb();
  const user = db.prepare('SELECT id FROM users WHERE email = ?').get(email);
  if (!user) return res.status(404).json({ error: 'User not found' });
  const existing = db.prepare('SELECT id FROM team_members WHERE owner_id = ? AND member_id = ?').get(req.user.id, user.id);
  if (existing) return res.status(409).json({ error: 'Already a team member' });
  const id = uuidv4();
  db.prepare('INSERT INTO team_members (id, owner_id, member_id, role) VALUES (?, ?, ?, ?)').run(id, req.user.id, user.id, role || 'member');
  res.json({ member: { id, email, role: role || 'member' } });
});

router.delete('/:id', auth, function (req, res) {
  const db = getDb();
  db.prepare('DELETE FROM team_members WHERE id = ? AND owner_id = ?').run(req.params.id, req.user.id);
  res.json({ ok: true });
});

router.put('/:id/role', auth, function (req, res) {
  const { role } = req.body;
  const db = getDb();
  db.prepare('UPDATE team_members SET role = ? WHERE id = ? AND owner_id = ?').run(role, req.params.id, req.user.id);
  res.json({ ok: true });
});

module.exports = router;
