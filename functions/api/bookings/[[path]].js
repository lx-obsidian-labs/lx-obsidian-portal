import { json, error, uuid, requireAuth, getSecretFrom } from '../_auth.js';
import { get, all, run } from '../_db.js';

export async function onRequest(context) {
  const { request } = context;
  const method = request.method;
  const path = (Array.isArray(context.params.path) ? context.params.path.join('/') : (context.params.path || '')).replace(/\/+$/, '');
  const secret = getSecretFrom(context);

  if (method === 'OPTIONS') return new Response(null, { status: 204, headers: cors() });

  const user = await requireAuth(request, secret);
  if (!user) return error('Auth required', 401);

  try {
    if (method === 'GET') {
      const result = await all(context, 'SELECT * FROM bookings WHERE user_id = ? ORDER BY start_time DESC', user.id);
      return json({ bookings: result.results });
    }

    if (method === 'POST') {
      const { type, title, description, start_time, end_time } = await request.json();
      if (!type || !start_time || !end_time) return error('Type, start_time, end_time required');
      const id = uuid();
      await run(context, 'INSERT INTO bookings (id, user_id, type, title, description, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?)', id, user.id, type, title || null, description || null, start_time, end_time);
      return json({ booking: { id, type, title, start_time, end_time, status: 'pending' } });
    }

    return error('Method not allowed', 405);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
