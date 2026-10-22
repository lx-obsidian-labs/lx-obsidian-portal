/** Store an enquiry before acknowledging it. Requires migrations/0003_enquiries.sql. */
function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' } });
}
export async function onRequest(context) {
  const { request, env } = context;
  if (request.method === 'OPTIONS') return new Response(null, { status: 204 });
  if (request.method !== 'POST') return json({ error: 'Method not allowed' }, 405);
  let body;
  try {
    const raw = await request.text();
    if (raw.length > 20000) return json({ error: 'Please shorten your enquiry.' }, 413);
    body = JSON.parse(raw);
    if (!body || typeof body !== 'object' || Array.isArray(body)) return json({ error: 'Invalid enquiry.' }, 400);
  } catch {
    return json({ error: 'Invalid JSON body.' }, 400);
  }
  const limits = { name: 120, email: 254, subject: 200, message: 10000, company: 200, projecttype: 200, budget: 100, launch: 100, website: 500, preferred: 200 };
  const fields = {};
  for (const [key, max] of Object.entries(limits)) {
    if (body[key] != null && typeof body[key] !== 'string') return json({ error: 'Invalid enquiry fields.' }, 400);
    fields[key] = (body[key] || '').trim();
    if (fields[key].length > max) return json({ error: 'Please shorten the ' + key + ' field.' }, 400);
  }
  if (!fields.name || !fields.email || !fields.subject || !fields.message) return json({ error: 'Name, email, subject and message are required.' }, 400);
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(fields.email)) return json({ error: 'Please enter a valid email address.' }, 400);
  const intent = body.intent || 'project';
  if (!['project', 'partnership', 'beta'].includes(intent)) return json({ error: 'Invalid enquiry type.' }, 400);
  if (!env.DB) return json({ error: 'Enquiries are temporarily unavailable. Please try again later.' }, 503);
  try {
    const result = await env.DB.prepare(
      'INSERT INTO enquiries (intent, name, email, company, subject, message, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
    ).bind(intent, fields.name, fields.email.toLowerCase(), fields.company, fields.subject, fields.message,
      JSON.stringify({ projecttype: fields.projecttype, budget: fields.budget, launch: fields.launch, website: fields.website, preferred: fields.preferred }), new Date().toISOString()).run();
    if (!result.success) throw new Error('Write failed');
    return json({ ok: true, stored: true }, 201);
  } catch {
    console.error('Enquiry persistence unavailable');
    return json({ error: 'Your enquiry could not be saved. Please try again later.' }, 503);
  }
}
