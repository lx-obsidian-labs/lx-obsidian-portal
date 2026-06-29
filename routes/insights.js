const express = require('express');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/overview', auth, function (req, res) {
  const db = getDb();
  const userId = req.user.id;
  const orders = db.prepare('SELECT COUNT(*) as count, COALESCE(SUM(total), 0) as revenue FROM orders WHERE user_id = ?').get(userId);
  const projects = db.prepare('SELECT COUNT(*) as count FROM projects WHERE user_id = ?').get(userId);
  const downloads = db.prepare('SELECT COUNT(*) as count FROM licenses WHERE user_id = ?').get(userId);
  const tickets = db.prepare('SELECT COUNT(*) as count FROM support_tickets WHERE user_id = ?').get(userId);
  const growth = db.prepare("SELECT COUNT(*) as count FROM analytics_events WHERE user_id = ? AND event = 'visit' AND created_at > datetime('now', '-30 days')").get(userId);
  res.json({
    totalOrders: orders.count,
    totalRevenue: orders.revenue,
    totalProjects: projects.count,
    totalDownloads: downloads.count,
    openTickets: tickets.count,
    visitorGrowth: growth.count,
    seoScore: 78,
    performanceScore: 85,
    securityScore: 92,
    marketingScore: 65,
    customerGrowth: Math.floor(Math.random() * 30) + 10
  });
});

router.get('/visitors', auth, function (req, res) {
  const db = getDb();
  const visits = db.prepare("SELECT DATE(created_at) as date, COUNT(*) as count FROM analytics_events WHERE event = 'visit' AND created_at > datetime('now', '-30 days') GROUP BY DATE(created_at) ORDER BY date").all();
  res.json({ visitors: visits });
});

router.get('/sales', auth, function (req, res) {
  const db = getDb();
  const sales = db.prepare("SELECT DATE(created_at) as date, COUNT(*) as count, COALESCE(SUM(amount), 0) as revenue FROM payments WHERE status = 'completed' AND created_at > datetime('now', '-30 days') GROUP BY DATE(created_at) ORDER BY date").all();
  res.json({ sales });
});

module.exports = router;
