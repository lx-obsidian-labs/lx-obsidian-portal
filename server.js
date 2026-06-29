const express = require('express');
const cors = require('cors');
const path = require('path');
const https = require('https');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;

try {
  const envPath = path.join(__dirname, '.env');
  if (fs.existsSync(envPath)) {
    const envContent = fs.readFileSync(envPath, 'utf8');
    const match = envContent.match(/NVIDIA_API_KEY=([^\s]+)/);
    if (match) process.env.NVIDIA_API_KEY = match[1];
  }
} catch (err) {}

const NVIDIA_API_KEY = process.env.NVIDIA_API_KEY || '';

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.static(path.join(__dirname)));

// API Routes
app.use('/api/auth', require('./routes/auth'));
app.use('/api/dashboard', require('./routes/dashboard'));
app.use('/api/store', require('./routes/store'));
app.use('/api/ai', require('./routes/ai'));
app.use('/api/billing', require('./routes/billing'));
app.use('/api/academy', require('./routes/academy'));
app.use('/api/hosting', require('./routes/hosting'));
app.use('/api/insights', require('./routes/insights'));
app.use('/api/teams', require('./routes/teams'));
app.use('/api/notifications', require('./routes/notifications'));
app.use('/api/bookings', require('./routes/bookings'));
app.use('/api/affiliates', require('./routes/affiliates'));
app.use('/api/vendors', require('./routes/vendors'));

app.get('/api/health', function (req, res) {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.post('/api/chat', function (req, res) {
  var userMessage = req.body.message;
  if (!userMessage || typeof userMessage !== 'string') {
    return res.status(400).json({ error: 'Message is required' });
  }
  var systemPrompt = {
    role: 'system',
    content: 'You are LX Assistant, the official AI assistant for LX Obsidian Labs. You help users with questions about software development, AI, cloud computing, mobile apps, enterprise systems, and digital transformation. Be concise, professional, and helpful. Keep responses under 150 words. Do not mention that you are an AI model. Sign off with "-- LX Assistant" at the end of longer responses.'
  };
  var postData = JSON.stringify({
    model: 'meta/llama3-70b-instruct',
    messages: [systemPrompt, { role: 'user', content: userMessage }],
    max_tokens: 512,
    temperature: 0.7,
    top_p: 0.9
  });
  var options = {
    hostname: 'integrate.api.nvidia.com',
    port: 443,
    path: '/v1/chat/completions',
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + NVIDIA_API_KEY,
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(postData)
    }
  };
  var apiReq = https.request(options, function (apiRes) {
    var body = '';
    apiRes.on('data', function (chunk) { body += chunk; });
    apiRes.on('end', function () {
      try {
        var parsed = JSON.parse(body);
        if (parsed.error) {
          return res.status(500).json({ reply: 'I apologize, but I\'m having trouble connecting right now. Please try again.' });
        }
        var reply = parsed.choices && parsed.choices[0] && parsed.choices[0].message
          ? parsed.choices[0].message.content.trim()
          : 'I understand your question. Let me connect you with our team for a more detailed answer.';
        res.json({ reply: reply });
      } catch (e) {
        res.status(500).json({ reply: 'I encountered an error processing your request.' });
      }
    });
  });
  apiReq.on('error', function () {
    res.status(500).json({ reply: 'I\'m having a temporary connectivity issue. Please try again in a moment.' });
  });
  apiReq.write(postData);
  apiReq.end();
});

app.post('/api/contact', function (req, res) {
  var body = req.body;
  var name = (body.name || '').trim();
  var email = (body.email || '').trim();
  var subject = (body.subject || '').trim();
  var message = (body.message || '').trim();
  if (!name || !email || !subject || !message) {
    return res.status(400).json({ error: 'All fields are required' });
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ error: 'Invalid email' });
  }
  console.log('Contact submission:', { name, email, subject, messageLength: message.length });
  res.json({ ok: true });
});

// Dashboard SPA - serve dashboard.html for /dashboard/* routes
app.get('/dashboard*', function (req, res) {
  res.sendFile(path.join(__dirname, 'dashboard', 'index.html'));
});

app.listen(PORT, function () {
  console.log('LX Obsidian Portal server running on http://localhost:' + PORT);
  console.log('Platform features: Dashboard, Store, AI Hub, Projects, Billing, Academy, Hosting, Insights, Teams, Bookings, Affiliates, Vendors');
});
