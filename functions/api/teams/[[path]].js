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
    if (path === '' && method === 'GET') {
      const members = await all(context, 'SELECT tm.*, u.name, u.email, u.avatar FROM team_members tm JOIN users u ON tm.member_id = u.id WHERE tm.owner_id = ?', user.id);
      return json({ members: members.results });
    }

    if (path === 'invite' && method === 'POST') {
      const { email, role } = await request.json();
      if (!email) return error('Email required');
      const member = await get(context, 'SELECT id, name, email FROM users WHERE email = ?', email);
      if (!member) return error('User not found', 404);
      const exists = await get(context, 'SELECT id FROM team_members WHERE owner_id = ? AND member_id = ?', user.id, member.id);
      if (exists) return error('Already a member', 409);
      const id = uuid();
      await run(context, 'INSERT INTO team_members (id, owner_id, member_id, role) VALUES (?, ?, ?, ?)', id, user.id, member.id, role || 'member');
      return json({ member: { id, name: member.name, email: member.email, role: role || 'member' } });
    }

    if (path.match(/^[^\/]+$/) && method === 'DELETE') {
      await run(context, 'DELETE FROM team_members WHERE id = ? AND owner_id = ?', path, user.id);
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
