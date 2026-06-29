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
    if (path === 'invoices' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM invoices WHERE user_id = ? ORDER BY created_at DESC', user.id);
      return json({ invoices: result.results });
    }

    if (path === 'payments' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM payments WHERE user_id = ? ORDER BY created_at DESC', user.id);
      return json({ payments: result.results });
    }

    if (path === 'cards' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM saved_cards WHERE user_id = ? ORDER BY is_default DESC', user.id);
      return json({ cards: result.results });
    }

    if (path === 'cards' && method === 'POST') {
      const { last_four, brand, expiry_month, expiry_year } = await request.json();
      if (!last_four) return error('Last four digits required');
      const id = uuid();
      const count = await get(context, 'SELECT COUNT(*) as count FROM saved_cards WHERE user_id = ?', user.id);
      await run(context, 'INSERT INTO saved_cards (id, user_id, last_four, brand, expiry_month, expiry_year, is_default) VALUES (?, ?, ?, ?, ?, ?, ?)', id, user.id, last_four, brand || null, expiry_month || null, expiry_year || null, count.count === 0 ? 1 : 0);
      return json({ card: { id, last_four, brand } });
    }

    if (path === 'subscriptions' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM subscriptions WHERE user_id = ? ORDER BY created_at DESC', user.id);
      return json({ subscriptions: result.results });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
