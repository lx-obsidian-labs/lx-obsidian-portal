import { json, error, requireAuth, getSecretFrom } from '../_auth.js';
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
    if (path === '' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50', user.id);
      return json({ notifications: result.results });
    }

    if (path === '' && method === 'POST') {
      await run(context, 'UPDATE notifications SET read = 1 WHERE user_id = ? AND read = 0', user.id);
      return json({ ok: true });
    }

    const match = path.match(/^([^\/]+)\/read$/);
    if (match && method === 'PUT') {
      await run(context, 'UPDATE notifications SET read = 1 WHERE id = ? AND user_id = ?', match[1], user.id);
      return json({ ok: true });
    }

    if (path === 'read-all' && method === 'PUT') {
      await run(context, 'UPDATE notifications SET read = 1 WHERE user_id = ? AND read = 0', user.id);
      return json({ ok: true });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
