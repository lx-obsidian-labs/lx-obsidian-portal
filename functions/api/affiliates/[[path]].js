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
    if (path === 'status' && method === 'GET') {
      let affiliate = await get(context, 'SELECT * FROM affiliates WHERE user_id = ?', user.id);
      if (!affiliate) {
        const id = uuid();
        const code = 'LX-' + user.id.slice(0, 6).toUpperCase();
        await run(context, 'INSERT INTO affiliates (id, user_id, referral_code) VALUES (?, ?, ?)', id, user.id, code);
        affiliate = await get(context, 'SELECT * FROM affiliates WHERE user_id = ?', user.id);
      }
      const payouts = await all(context, 'SELECT * FROM affiliate_payouts WHERE affiliate_id = ? ORDER BY created_at DESC', affiliate.id);
      return json({ affiliate, payouts: payouts.results });
    }

    if (path === 'withdraw' && method === 'POST') {
      const affiliate = await get(context, 'SELECT * FROM affiliates WHERE user_id = ?', user.id);
      if (!affiliate || affiliate.balance < 10) return error('Minimum payout: $10', 400);
      const id = uuid();
      await run(context, 'INSERT INTO affiliate_payouts (id, affiliate_id, amount) VALUES (?, ?, ?)', id, affiliate.id, affiliate.balance);
      await run(context, 'UPDATE affiliates SET balance = 0 WHERE id = ?', affiliate.id);
      return json({ payout: { id, amount: affiliate.balance } });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
