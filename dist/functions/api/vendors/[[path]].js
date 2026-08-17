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
    if (path === 'products' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM vendor_products WHERE vendor_id = ? ORDER BY created_at DESC', user.id);
      return json({ products: result.results });
    }

    if (path === 'products' && method === 'POST') {
      const { name, description, price, category, commission_rate } = await request.json();
      if (!name || !price) return error('Name and price required');
      const id = uuid();
      await run(context, 'INSERT INTO vendor_products (id, vendor_id, name, description, price, category, commission_rate) VALUES (?, ?, ?, ?, ?, ?, ?)', id, user.id, name, description || null, price, category || null, commission_rate || 15);
      return json({ product: { id, name, description, price, category, status: 'pending' } });
    }

    if (path === 'products' && method === 'PUT') {
      const { id, name, description, price, category } = await request.json();
      if (!id) return error('Product ID required');
      await run(context, 'UPDATE vendor_products SET name = COALESCE(?, name), description = COALESCE(?, description), price = COALESCE(?, price), category = COALESCE(?, category) WHERE id = ? AND vendor_id = ?', name || null, description || null, price || null, category || null, id, user.id);
      return json({ ok: true });
    }

    if (path === 'marketplace' && method === 'GET') {
      const result = await all(context, "SELECT vp.*, u.name as vendor_name FROM vendor_products vp JOIN users u ON vp.vendor_id = u.id WHERE vp.status = ? ORDER BY vp.created_at DESC", 'approved');
      return json({ products: result.results });
    }

    if (path === 'earnings' && method === 'GET') {
      const total = await get(context, "SELECT COALESCE(SUM(price * commission_rate / 100), 0) as total FROM vendor_products WHERE vendor_id = ? AND status = 'active'", user.id);
      const products = await all(context, "SELECT COUNT(*) as total FROM vendor_products WHERE vendor_id = ? AND status = 'active'", user.id);
      return json({ earnings: total.total, activeProducts: products.results[0]?.total || 0 });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
