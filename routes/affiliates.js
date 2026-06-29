const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/status', auth, function (req, res) {
  const db = getDb();
  let affiliate = db.prepare('SELECT * FROM affiliates WHERE user_id = ?').get(req.user.id);
  if (!affiliate) {
    const id = uuidv4();
    const code = 'LX-' + req.user.name.toUpperCase().replace(/\s+/g, '').slice(0, 6) + '-' + Math.random().toString(36).slice(2, 6).toUpperCase();
    db.prepare('INSERT INTO affiliates (id, user_id, referral_code) VALUES (?, ?, ?)').run(id, req.user.id, code);
    affiliate = db.prepare('SELECT * FROM affiliates WHERE user_id = ?').get(req.user.id);
  }
  res.json({ affiliate });
});

router.get('/payouts', auth, function (req, res) {
  const db = getDb();
  const affiliate = db.prepare('SELECT id FROM affiliates WHERE user_id = ?').get(req.user.id);
  if (!affiliate) return res.json({ payouts: [] });
  const payouts = db.prepare('SELECT * FROM affiliate_payouts WHERE affiliate_id = ? ORDER BY created_at DESC').all(affiliate.id);
  res.json({ payouts });
});

router.post('/withdraw', auth, function (req, res) {
  const { amount } = req.body;
  if (!amount || amount < 10) return res.status(400).json({ error: 'Minimum withdrawal is $10' });
  const db = getDb();
  const affiliate = db.prepare('SELECT * FROM affiliates WHERE user_id = ?').get(req.user.id);
  if (!affiliate) return res.status(404).json({ error: 'Affiliate account not found' });
  if ((affiliate.balance || 0) < amount) return res.status(400).json({ error: 'Insufficient balance' });
  const id = uuidv4();
  db.prepare('INSERT INTO affiliate_payouts (id, affiliate_id, amount) VALUES (?, ?, ?)').run(id, affiliate.id, amount);
  db.prepare('UPDATE affiliates SET balance = balance - ? WHERE id = ?').run(amount, affiliate.id);
  res.json({ payout: { id, amount, status: 'pending' } });
});

module.exports = router;
