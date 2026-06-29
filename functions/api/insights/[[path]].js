import { json, error, requireAuth, getSecretFrom } from '../_auth.js';
import { get, all } from '../_db.js';

export async function onRequest(context) {
  const { request } = context;
  const method = request.method;
  const path = (Array.isArray(context.params.path) ? context.params.path.join('/') : (context.params.path || '')).replace(/\/+$/, '');
  const secret = getSecretFrom(context);

  if (method === 'OPTIONS') return new Response(null, { status: 204, headers: cors() });
  if (method !== 'GET') return error('Method not allowed', 405);

  const user = await requireAuth(request, secret);
  if (!user) return error('Auth required', 401);

  try {
    if (path === '' || path === 'overview') {
      const [revenue, orders, projects, tickets, recentOrders, monthlyData] = await Promise.all([
        get(context, "SELECT COALESCE(SUM(amount), 0) as total FROM payments WHERE user_id = ? AND status = 'completed'", user.id),
        get(context, 'SELECT COUNT(*) as total, COALESCE(SUM(total), 0) as value FROM orders WHERE user_id = ?', user.id),
        get(context, "SELECT COUNT(*) as total FROM projects WHERE user_id = ? AND status = 'active'", user.id),
        get(context, "SELECT COUNT(*) as total FROM support_tickets WHERE user_id = ? AND status = 'open'", user.id),
        all(context, 'SELECT o.*, COUNT(oi.id) as items FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id WHERE o.user_id = ? GROUP BY o.id ORDER BY o.created_at DESC LIMIT 10', user.id),
        all(context, "SELECT strftime('%Y-%m', created_at) as month, COUNT(*) as orders, COALESCE(SUM(total), 0) as revenue FROM orders WHERE user_id = ? GROUP BY month ORDER BY month DESC LIMIT 12", user.id)
      ]);
      return json({ revenue, orders, projects, tickets, recentOrders: recentOrders.results, monthlyData: monthlyData.results });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
