/** Persist subscriptions; never acknowledge a failed database write. */
function json(payload, status = 200) {
  return new Response(JSON.stringify(payload), { status, headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' } });
}
export async function onRequest(context) {
  const { request, env } = context;
  if (request.method === 'OPTIONS') return new Response(null, { status: 204 });
  if (request.method !== 'POST') return json({ error: 'Method not allowed' }, 405);
  let body;
  try {
    const raw = await request.text();
    if (raw.length > 2048) return json({ error: 'Request too large.' }, 413);
    body = JSON.parse(raw);
  } catch { return json({ error: 'Invalid JSON body.' }, 400); }
  if (!body || typeof body.email !== 'string') return json({ error: 'Please enter a valid email address.' }, 400);
  const email = body.email.trim().toLowerCase();
  if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return json({ error: 'Please enter a valid email address.' }, 400);
  const source = typeof body.source === 'string' ? body.source.trim().slice(0, 64) : 'footer';
  if (!env.DB) return json({ error: 'Subscriptions are temporarily unavailable. Please try again later.' }, 503);
  try {
    const result = await env.DB.prepare('INSERT INTO subscribers (email, source, created_at) VALUES (?, ?, ?) ON CONFLICT(email) DO NOTHING').bind(email, source, new Date().toISOString()).run();
    if (!result.success) throw new Error('Write failed');
    return json({ ok: true, stored: true });
  } catch {
    console.error('Subscription persistence unavailable');
    return json({ error: 'Your subscription could not be saved. Please try again later.' }, 503);
  }
}
