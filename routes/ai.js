const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth, optionalAuth } = require('../middleware/auth');
const https = require('https');

const router = express.Router();

router.get('/agents', function (req, res) {
  const db = getDb();
  const agents = db.prepare('SELECT * FROM ai_agents WHERE status = ?').all('active');
  res.json({ agents });
});

router.post('/chats', auth, function (req, res) {
  const { agent_id, title } = req.body;
  if (!agent_id) return res.status(400).json({ error: 'Agent ID is required' });
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO ai_chats (id, user_id, agent_id, title) VALUES (?, ?, ?, ?)').run(id, req.user.id, agent_id, title || null);
  res.json({ chat: { id, agent_id, title } });
});

router.get('/chats', auth, function (req, res) {
  const db = getDb();
  const chats = db.prepare('SELECT c.*, a.name as agent_name, a.icon as agent_icon FROM ai_chats c JOIN ai_agents a ON c.agent_id = a.id WHERE c.user_id = ? ORDER BY c.created_at DESC').all(req.user.id);
  res.json({ chats });
});

router.get('/chats/:id/messages', auth, function (req, res) {
  const db = getDb();
  const messages = db.prepare('SELECT * FROM ai_messages WHERE chat_id = ? ORDER BY created_at ASC').all(req.params.id);
  res.json({ messages });
});

router.post('/chats/:id/messages', auth, function (req, res) {
  const { content } = req.body;
  if (!content) return res.status(400).json({ error: 'Content is required' });
  const db = getDb();
  const chat = db.prepare('SELECT c.*, a.prompt_template FROM ai_chats c JOIN ai_agents a ON c.agent_id = a.id WHERE c.id = ? AND c.user_id = ?').get(req.params.id, req.user.id);
  if (!chat) return res.status(404).json({ error: 'Chat not found' });
  const userId = req.user.id;
  const msgId = uuidv4();
  db.prepare('INSERT INTO ai_messages (id, chat_id, role, content) VALUES (?, ?, ?, ?)').run(msgId, req.params.id, 'user', content);
  const user = db.prepare('SELECT credits FROM users WHERE id = ?').get(userId);
  const agent = db.prepare('SELECT * FROM ai_agents WHERE id = ?').get(chat.agent_id);
  if ((user.credits || 0) < (agent.price_per_credit || 1)) {
    const replyId = uuidv4();
    db.prepare('INSERT INTO ai_messages (id, chat_id, role, content) VALUES (?, ?, ?, ?)').run(replyId, req.params.id, 'assistant', 'You have insufficient credits. Please purchase more credits to continue using AI services.');
    return res.json({ message: { id: replyId, role: 'assistant', content: 'You have insufficient credits.' } });
  }
  const NVIDIA_API_KEY = process.env.NVIDIA_API_KEY || '';
  const responseContent = 'Thank you for your message. Our AI team will review and respond shortly.';
  const replyId = uuidv4();
  db.prepare('INSERT INTO ai_messages (id, chat_id, role, content, credits_used) VALUES (?, ?, ?, ?, ?)').run(replyId, req.params.id, 'assistant', responseContent, agent.price_per_credit || 1);
  db.prepare('UPDATE users SET credits = credits - ? WHERE id = ?').run(agent.price_per_credit || 1, userId);
  res.json({ message: { id: replyId, role: 'assistant', content: responseContent, credits_used: agent.price_per_credit || 1 } });
});

router.get('/credits', auth, function (req, res) {
  const db = getDb();
  const user = db.prepare('SELECT credits FROM users WHERE id = ?').get(req.user.id);
  res.json({ credits: user.credits || 0 });
});

router.post('/credits/purchase', auth, function (req, res) {
  const { amount } = req.body;
  if (!amount || amount < 1) return res.status(400).json({ error: 'Invalid amount' });
  const db = getDb();
  db.prepare('UPDATE users SET credits = credits + ? WHERE id = ?').run(amount, req.user.id);
  res.json({ credits: db.prepare('SELECT credits FROM users WHERE id = ?').get(req.user.id).credits });
});

module.exports = router;
