/**
 * POST /api/subscribe
 * Newsletter signup for the Liquid Glass footer.
 *
 * Persists to the D1 `subscribers` table when the DB binding is provisioned
 * (see migrations/0002_subscribers.sql). Until then it validates, logs the
 * signup and still returns 200 so the footer keeps working.
 *
 * CORS headers are added by functions/api/_middleware.js.
 */
import { run } from './_db.js';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MAX_EMAIL = 254;

function json(payload, status) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

export async function onRequest(context) {
  const { request } = context;

  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204 });
  }
  if (request.method !== 'POST') {
    return json({ error: 'Method not allowed' }, 405);
  }

  let body;
  try {
    body = await request.json();
  } catch (_) {
    return json({ error: 'Invalid JSON body' }, 400);
  }

  const email = String((body && body.email) || '').trim().toLowerCase();
  const source = String((body && body.source) || 'footer').trim().slice(0, 64);

  if (!email || email.length > MAX_EMAIL || !EMAIL_RE.test(email)) {
    return json({ error: 'Please enter a valid email address.' }, 400);
  }

  if (context.env.DB) {
    try {
      await run(
        context,
        'INSERT INTO subscribers (email, source, created_at) VALUES (?, ?, ?) ' +
          'ON CONFLICT(email) DO NOTHING',
        email,
        source,
        new Date().toISOString()
      );
      return json({ ok: true, stored: true }, 200);
    } catch (err) {
      // Table missing or database unreachable — fall through to logging so the
      // form still succeeds rather than showing the visitor an error.
      console.log('subscribe: DB write failed:', err && err.message);
    }
  }

  console.log('subscribe:', { email, source });
  return json({ ok: true, stored: false }, 200);
}
