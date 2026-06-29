const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth, optionalAuth } = require('../middleware/auth');

const router = express.Router();

router.get('/products', function (req, res) {
  const db = getDb();
  const { category, search, featured } = req.query;
  let query = 'SELECT * FROM store_products WHERE status = ?';
  const params = ['active'];
  if (category) { query += ' AND category = ?'; params.push(category); }
  if (featured === 'true') { query += ' AND featured = 1'; }
  if (search) { query += ' AND (name LIKE ? OR description LIKE ?)'; params.push(`%${search}%`, `%${search}%`); }
  query += ' ORDER BY featured DESC, sales_count DESC';
  const products = db.prepare(query).all(...params);
  const categories = db.prepare('SELECT DISTINCT category FROM store_products WHERE status = ? ORDER BY category').all('active');
  res.json({ products, categories: categories.map(c => c.category) });
});

router.get('/products/:id', function (req, res) {
  const db = getDb();
  const product = db.prepare('SELECT * FROM store_products WHERE id = ?').get(req.params.id);
  if (!product) return res.status(404).json({ error: 'Product not found' });
  const reviews = db.prepare('SELECT r.*, u.name as user_name FROM product_reviews r JOIN users u ON r.user_id = u.id WHERE r.product_id = ? ORDER BY r.created_at DESC').all(req.params.id);
  res.json({ product, reviews });
});

router.post('/products/:id/reviews', auth, function (req, res) {
  const { rating, content } = req.body;
  if (!rating) return res.status(400).json({ error: 'Rating is required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO product_reviews (id, product_id, user_id, rating, content) VALUES (?, ?, ?, ?, ?)').run(id, req.params.id, req.user.id, rating, content || null);
  const avg = db.prepare('SELECT AVG(rating) as avg FROM product_reviews WHERE product_id = ?').get(req.params.id);
  db.prepare('UPDATE store_products SET rating = ? WHERE id = ?').run(avg.avg, req.params.id);
  res.json({ review: { id, rating, content } });
});

router.post('/wishlist/:productId', auth, function (req, res) {
  const db = getDb();
  const existing = db.prepare('SELECT id FROM wishlists WHERE user_id = ? AND product_id = ?').get(req.user.id, req.params.productId);
  if (existing) {
    db.prepare('DELETE FROM wishlists WHERE id = ?').run(existing.id);
    return res.json({ wishlisted: false });
  }
  db.prepare('INSERT INTO wishlists (id, user_id, product_id) VALUES (?, ?, ?)').run(uuidv4(), req.user.id, req.params.productId);
  res.json({ wishlisted: true });
});

router.get('/wishlist', auth, function (req, res) {
  const db = getDb();
  const items = db.prepare('SELECT w.*, p.name, p.price, p.category FROM wishlists w JOIN store_products p ON w.product_id = p.id WHERE w.user_id = ? ORDER BY w.created_at DESC').all(req.user.id);
  res.json({ items });
});

router.post('/checkout', auth, function (req, res) {
  const { items, coupon_code } = req.body;
  if (!items || !items.length) return res.status(400).json({ error: 'No items in cart' });
  const db = getDb();
  const orderId = uuidv4();
  let total = 0;
  const orderItems = [];
  const getProduct = db.prepare('SELECT * FROM store_products WHERE id = ?');
  for (const item of items) {
    const product = getProduct.get(item.product_id);
    if (!product) return res.status(404).json({ error: `Product ${item.product_id} not found` });
    total += product.price;
    const licenseKey = 'LX-' + uuidv4().split('-')[0].toUpperCase() + '-' + uuidv4().split('-')[1].toUpperCase();
    orderItems.push({ product_id: product.id, price: product.price, license_key: licenseKey });
  }
  let discount = 0;
  if (coupon_code) {
    const coupon = db.prepare('SELECT * FROM coupons WHERE code = ? AND (expires_at IS NULL OR expires_at > datetime(\'now\'))').get(coupon_code);
    if (coupon && coupon.used_count < coupon.max_uses) {
      discount = coupon.discount_type === 'percentage' ? total * coupon.discount_value / 100 : coupon.discount_value;
      db.prepare('UPDATE coupons SET used_count = used_count + 1 WHERE id = ?').run(coupon.id);
    }
  }
  total = Math.max(0, total - discount);
  db.prepare('INSERT INTO orders (id, user_id, total, status) VALUES (?, ?, ?, ?)').run(orderId, req.user.id, total, 'completed');
  const insertItem = db.prepare('INSERT INTO order_items (id, order_id, product_id, price, license_key) VALUES (?, ?, ?, ?, ?)');
  const insertLicense = db.prepare('INSERT INTO licenses (id, user_id, product_id, license_key) VALUES (?, ?, ?, ?)');
  for (const oi of orderItems) {
    const itemId = uuidv4();
    insertItem.run(itemId, orderId, oi.product_id, oi.price, oi.license_key);
    insertLicense.run(uuidv4(), req.user.id, oi.product_id, oi.license_key);
    db.prepare('UPDATE store_products SET sales_count = sales_count + 1 WHERE id = ?').run(oi.product_id);
  }
  db.prepare('INSERT INTO invoices (id, user_id, amount, status, due_date, invoice_number) VALUES (?, ?, ?, ?, ?, ?)').run(uuidv4(), req.user.id, total, 'paid', new Date().toISOString(), 'INV-' + Date.now());
  res.json({ order: { id: orderId, total, status: 'completed' }, items: orderItems });
});

router.get('/orders', auth, function (req, res) {
  const db = getDb();
  const orders = db.prepare('SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC').all(req.user.id);
  const getItems = db.prepare('SELECT oi.*, p.name as product_name FROM order_items oi JOIN store_products p ON oi.product_id = p.id WHERE oi.order_id = ?');
  for (const order of orders) {
    order.items = getItems.all(order.id);
  }
  res.json({ orders });
});

router.get('/downloads', auth, function (req, res) {
  const db = getDb();
  const downloads = db.prepare('SELECT l.*, p.name as product_name, p.file_path FROM licenses l JOIN store_products p ON l.product_id = p.id WHERE l.user_id = ? ORDER BY l.created_at DESC').all(req.user.id);
  res.json({ downloads });
});

module.exports = router;
