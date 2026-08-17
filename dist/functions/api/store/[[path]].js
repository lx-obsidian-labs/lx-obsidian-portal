import { json, error, uuid, requireAuth, getSecretFrom } from '../_auth.js';
import { get, all, run } from '../_db.js';

export async function onRequest(context) {
  const { request } = context;
  const method = request.method;
  const path = (Array.isArray(context.params.path) ? context.params.path.join('/') : (context.params.path || '')).replace(/\/+$/, '');
  const secret = getSecretFrom(context);

  if (method === 'OPTIONS') return new Response(null, { status: 204, headers: cors() });

  try {
    if (path === 'products' && method === 'GET') {
      return handleProducts(context, request);
    }

    if (path.startsWith('products/') && path.split('/').length === 2 && method === 'GET') {
      const productId = path.split('/')[1];
      return handleProductDetail(context, productId);
    }

    if (path.match(/^products\/[^\/]+\/reviews$/) && method === 'POST') {
      const productId = path.split('/')[1];
      return handleReview(context, request, productId, secret);
    }

    if (path === 'wishlist' && method === 'GET') {
      return handleWishlist(context, request, secret);
    }

    if (path.startsWith('wishlist/') && method === 'POST') {
      return handleToggleWishlist(context, request, path.split('/')[1], secret);
    }

    if (path === 'checkout' && method === 'POST') {
      return handleCheckout(context, request, secret);
    }

    if (path === 'orders' && method === 'GET') {
      return handleOrders(context, request, secret);
    }

    if (path === 'downloads' && method === 'GET') {
      return handleDownloads(context, request, secret);
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

async function handleProducts(context, request) {
  const url = new URL(request.url);
  const category = url.searchParams.get('category');
  const search = url.searchParams.get('search');
  const featured = url.searchParams.get('featured');
  let query = "SELECT * FROM store_products WHERE status = ?";
  const params = ['active'];
  if (category) { query += ' AND category = ?'; params.push(category); }
  if (featured === 'true') query += ' AND featured = 1';
  if (search) { query += ' AND (name LIKE ? OR description LIKE ?)'; params.push(`%${search}%`, `%${search}%`); }
  query += ' ORDER BY featured DESC, sales_count DESC';
  const products = await all(context, query, ...params);
  const categories = await all(context, "SELECT DISTINCT category FROM store_products WHERE status = ? ORDER BY category", 'active');
  return json({ products: products.results, categories: categories.results.map(c => c.category) });
}

async function handleProductDetail(context, id) {
  const product = await get(context, 'SELECT * FROM store_products WHERE id = ?', id);
  if (!product) return error('Product not found', 404);
  const reviews = await all(context, 'SELECT r.*, u.name as user_name FROM product_reviews r JOIN users u ON r.user_id = u.id WHERE r.product_id = ? ORDER BY r.created_at DESC', id);
  return json({ product, reviews: reviews.results });
}

async function handleReview(context, request, productId, secret) {
  const user = await requireAuth(request, secret);
  if (!user) return error('Auth required', 401);
  const { rating, content } = await request.json();
  if (!rating) return error('Rating is required');
  const id = uuid();
  await run(context, 'INSERT INTO product_reviews (id, product_id, user_id, rating, content) VALUES (?, ?, ?, ?, ?)', id, productId, user.id, rating, content || null);
  const avg = await get(context, 'SELECT AVG(rating) as avg FROM product_reviews WHERE product_id = ?', productId);
  await run(context, 'UPDATE store_products SET rating = ? WHERE id = ?', avg.avg, productId);
  return json({ review: { id, rating, content } });
}

async function handleWishlist(context, request, secret) {
  const user = await requireAuth(request, secret);
  if (!user) return error('Auth required', 401);
  const items = await all(context, 'SELECT w.*, p.name, p.price, p.category FROM wishlists w JOIN store_products p ON w.product_id = p.id WHERE w.user_id = ? ORDER BY w.created_at DESC', user.id);
  return json({ items: items.results });
}

async function handleToggleWishlist(context, request, productId, secret) {
  const user = await requireAuth(request, secret);
  if (!user) return error('Auth required', 401);
  const existing = await get(context, 'SELECT id FROM wishlists WHERE user_id = ? AND product_id = ?', user.id, productId);
  if (existing) { await run(context, 'DELETE FROM wishlists WHERE id = ?', existing.id); return json({ wishlisted: false }); }
  await run(context, 'INSERT INTO wishlists (id, user_id, product_id) VALUES (?, ?, ?)', uuid(), user.id, productId);
  return json({ wishlisted: true });
}

async function handleCheckout(context, request, secret) {
  const user = await requireAuth(request, secret);
  if (!user) return error('Auth required', 401);
  const { items, coupon_code } = await request.json();
  if (!items || !items.length) return error('No items in cart');
  const orderId = uuid();
  let total = 0;
  const orderItems = [];
  for (const item of items) {
    const product = await get(context, 'SELECT * FROM store_products WHERE id = ?', item.product_id);
    if (!product) return error(`Product ${item.product_id} not found`, 404);
    total += product.price;
    const licenseKey = 'LX-' + uuid().split('-')[0].toUpperCase() + '-' + uuid().split('-')[1].toUpperCase();
    orderItems.push({ product_id: product.id, price: product.price, license_key: licenseKey });
  }
  let discount = 0;
  if (coupon_code) {
    const coupon = await get(context, "SELECT * FROM coupons WHERE code = ? AND (expires_at IS NULL OR expires_at > datetime('now'))", coupon_code);
    if (coupon && coupon.used_count < coupon.max_uses) {
      discount = coupon.discount_type === 'percentage' ? total * coupon.discount_value / 100 : coupon.discount_value;
      await run(context, 'UPDATE coupons SET used_count = used_count + 1 WHERE id = ?', coupon.id);
    }
  }
  total = Math.max(0, total - discount);
  await run(context, 'INSERT INTO orders (id, user_id, total, status) VALUES (?, ?, ?, ?)', orderId, user.id, total, 'completed');
  for (const oi of orderItems) {
    const itemId = uuid();
    await run(context, 'INSERT INTO order_items (id, order_id, product_id, price, license_key) VALUES (?, ?, ?, ?, ?)', itemId, orderId, oi.product_id, oi.price, oi.license_key);
    await run(context, 'INSERT INTO licenses (id, user_id, product_id, license_key) VALUES (?, ?, ?, ?)', uuid(), user.id, oi.product_id, oi.license_key);
    await run(context, 'UPDATE store_products SET sales_count = sales_count + 1 WHERE id = ?', oi.product_id);
  }
  await run(context, "INSERT INTO invoices (id, user_id, amount, status, due_date, invoice_number) VALUES (?, ?, ?, ?, ?, ?)", uuid(), user.id, total, 'paid', new Date().toISOString(), 'INV-' + Date.now());
  return json({ order: { id: orderId, total, status: 'completed' }, items: orderItems });
}

async function handleOrders(context, request, secret) {
  const user = await requireAuth(request, secret);
  if (!user) return error('Auth required', 401);
  const ordersResult = await all(context, 'SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC', user.id);
  const orders = ordersResult.results;
  for (const order of orders) {
    const result = await all(context, 'SELECT oi.*, p.name as product_name FROM order_items oi JOIN store_products p ON oi.product_id = p.id WHERE oi.order_id = ?', order.id);
    order.items = result.results;
  }
  return json({ orders });
}

async function handleDownloads(context, request, secret) {
  const user = await requireAuth(request, secret);
  if (!user) return error('Auth required', 401);
  const result = await all(context, 'SELECT l.*, p.name as product_name, p.file_path FROM licenses l JOIN store_products p ON l.product_id = p.id WHERE l.user_id = ? ORDER BY l.created_at DESC', user.id);
  return json({ downloads: result.results });
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
