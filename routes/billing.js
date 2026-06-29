const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/invoices', auth, function (req, res) {
  const db = getDb();
  const invoices = db.prepare('SELECT * FROM invoices WHERE user_id = ? ORDER BY created_at DESC').all(req.user.id);
  res.json({ invoices });
});

router.get('/invoices/:id', auth, function (req, res) {
  const db = getDb();
  const invoice = db.prepare('SELECT * FROM invoices WHERE id = ? AND user_id = ?').get(req.params.id, req.user.id);
  if (!invoice) return res.status(404).json({ error: 'Invoice not found' });
  res.json({ invoice });
});

router.get('/payments', auth, function (req, res) {
  const db = getDb();
  const payments = db.prepare('SELECT * FROM payments WHERE user_id = ? ORDER BY created_at DESC').all(req.user.id);
  res.json({ payments });
});

router.get('/cards', auth, function (req, res) {
  const db = getDb();
  const cards = db.prepare('SELECT * FROM saved_cards WHERE user_id = ?').all(req.user.id);
  res.json({ cards });
});

router.post('/cards', auth, function (req, res) {
  const { last_four, brand, expiry_month, expiry_year } = req.body;
  if (!last_four) return res.status(400).json({ error: 'Last four digits required' });
  const db = getDb();
  const id = uuidv4();
  const existing = db.prepare('SELECT COUNT(*) as count FROM saved_cards WHERE user_id = ?').get(req.user.id);
  db.prepare('INSERT INTO saved_cards (id, user_id, last_four, brand, expiry_month, expiry_year, is_default) VALUES (?, ?, ?, ?, ?, ?, ?)').run(id, req.user.id, last_four, brand || null, expiry_month || null, expiry_year || null, existing.count === 0 ? 1 : 0);
  res.json({ card: { id, last_four, brand } });
});

router.delete('/cards/:id', auth, function (req, res) {
  const db = getDb();
  db.prepare('DELETE FROM saved_cards WHERE id = ? AND user_id = ?').run(req.params.id, req.user.id);
  res.json({ ok: true });
});

router.get('/coupons/:code', function (req, res) {
  const db = getDb();
  const coupon = db.prepare('SELECT * FROM coupons WHERE code = ? AND (expires_at IS NULL OR expires_at > datetime(\'now\'))').get(req.params.code);
  if (!coupon) return res.status(404).json({ error: 'Invalid or expired coupon' });
  if (coupon.max_uses && coupon.used_count >= coupon.max_uses) return res.status(400).json({ error: 'Coupon usage limit reached' });
  res.json({ coupon });
});

module.exports = router;
