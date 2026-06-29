const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/', auth, function (req, res) {
  const db = getDb();
  const bookings = db.prepare('SELECT * FROM bookings WHERE user_id = ? ORDER BY start_time DESC').all(req.user.id);
  res.json({ bookings });
});

router.post('/', auth, function (req, res) {
  const { type, title, description, start_time, end_time } = req.body;
  if (!type || !start_time) return res.status(400).json({ error: 'Type and start time are required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO bookings (id, user_id, type, title, description, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?)').run(id, req.user.id, type, title || null, description || null, start_time, end_time || null);
  res.json({ booking: { id, type, start_time, status: 'pending' } });
});

router.put('/:id/cancel', auth, function (req, res) {
  const db = getDb();
  db.prepare("UPDATE bookings SET status = 'cancelled' WHERE id = ? AND user_id = ?").run(req.params.id, req.user.id);
  res.json({ ok: true });
});

module.exports = router;
