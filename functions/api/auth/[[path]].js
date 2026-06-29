import { json, error, uuid, hashPassword, comparePassword, createToken, verifyToken, requireAuth, getSecretFrom } from '../_auth.js';
import { get, all, run } from '../_db.js';

export async function onRequest(context) {
  const { request } = context;
  const method = request.method;
  const path = (Array.isArray(context.params.path) ? context.params.path.join('/') : (context.params.path || '')).replace(/\/+$/, '');
  const secret = getSecretFrom(context);

  if (method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: cors() });
  }

  try {
    if (path === 'register' && method === 'POST') {
      const { email, password, name, company, phone } = await request.json();
      if (!email || !password || !name) return error('Name, email, and password are required');
      const existing = await get(context, 'SELECT id FROM users WHERE email = ?', email);
      if (existing) return error('Email already registered', 409);
      const id = uuid();
      const hashed = await hashPassword(password);
      await run(context, 'INSERT INTO users (id, email, password, name, company, phone) VALUES (?, ?, ?, ?, ?, ?)', id, email, hashed, name, company || null, phone || null);
      const token = await createToken({ id, email, name, role: 'customer' }, secret);
      return json({ token, user: { id, email, name, company, role: 'customer' } });
    }

    if (path === 'login' && method === 'POST') {
      const { email, password } = await request.json();
      if (!email || !password) return error('Email and password are required');
      const user = await get(context, 'SELECT * FROM users WHERE email = ?', email);
      if (!user || !(await comparePassword(password, user.password))) return error('Invalid email or password', 401);
      const token = await createToken(user, secret);
      return json({ token, user: { id: user.id, email: user.email, name: user.name, company: user.company, phone: user.phone, role: user.role, credits: user.credits, avatar: user.avatar } });
    }

    if (path === 'me' && method === 'GET') {
      const auth = await requireAuth(request, secret);
      if (!auth) return error('Auth required', 401);
      const user = await get(context, 'SELECT id, email, name, company, phone, role, credits, avatar, created_at FROM users WHERE id = ?', auth.id);
      if (!user) return error('User not found', 404);
      return json({ user });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
