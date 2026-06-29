const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/products', auth, function (req, res) {
  const db = getDb();
  const products = db.prepare('SELECT * FROM vendor_products WHERE vendor_id = ? ORDER BY created_at DESC').all(req.user.id);
  res.json({ products });
});

router.post('/products', auth, function (req, res) {
  const { name, description, price, category } = req.body;
  if (!name || !price) return res.status(400).json({ error: 'Name and price are required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO vendor_products (id, vendor_id, name, description, price, category) VALUES (?, ?, ?, ?, ?, ?)').run(id, req.user.id, name, description || null, price, category || null);
  res.json({ product: { id, name, description, price, status: 'pending' } });
});

router.get('/marketplace', function (req, res) {
  const db = getDb();
  const products = db.prepare('SELECT vp.*, u.name as vendor_name FROM vendor_products vp JOIN users u ON vp.vendor_id = u.id WHERE vp.status = ? ORDER BY vp.created_at DESC').all('approved');
  res.json({ products });
});

router.get('/earnings', auth, function (req, res) {
  const db = getDb();
  const total = db.prepare('SELECT COALESCE(SUM(price), 0) as total, COUNT(*) as sales FROM vendor_products WHERE vendor_id = ?').get(req.user.id);
  res.json({ earnings: total.total, sales: total.sales, commission_rate: 15 });
});

module.exports = router;
