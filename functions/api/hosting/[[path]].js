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
    if (path === 'accounts' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM hosting_accounts WHERE user_id = ?', user.id);
      return json({ accounts: result.results });
    }

    if (path === 'accounts' && method === 'POST') {
      const { domain, plan } = await request.json();
      const id = uuid();
      await run(context, 'INSERT INTO hosting_accounts (id, user_id, domain, plan) VALUES (?, ?, ?, ?)', id, user.id, domain || null, plan || 'basic');
      return json({ account: { id, domain, plan } });
    }

    if (path === 'ssl' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM ssl_certificates WHERE user_id = ?', user.id);
      return json({ certificates: result.results });
    }

    if (path === 'dns' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM dns_records WHERE user_id = ?', user.id);
      return json({ records: result.results });
    }

    if (path === 'dns' && method === 'POST') {
      const { domain, type, name, value, ttl } = await request.json();
      if (!domain || !type || !name || !value) return error('Domain, type, name, and value required');
      const id = uuid();
      await run(context, 'INSERT INTO dns_records (id, user_id, domain, type, name, value, ttl) VALUES (?, ?, ?, ?, ?, ?, ?)', id, user.id, domain, type, name, value, ttl || 3600);
      return json({ record: { id, domain, type, name, value } });
    }

    if (path === 'backups' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM backups WHERE user_id = ? ORDER BY created_at DESC', user.id);
      return json({ backups: result.results });
    }

    if (path === 'deployments' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM deployments WHERE user_id = ? ORDER BY created_at DESC', user.id);
      return json({ deployments: result.results });
    }

    if (path === 'deployments' && method === 'POST') {
      const { project_id, version } = await request.json();
      const id = uuid();
      await run(context, "INSERT INTO deployments (id, project_id, user_id, version, status, url) VALUES (?, ?, ?, ?, 'pending', ?)", id, project_id || null, user.id, version || '1.0.0', `https://${project_id ? 'project-' + project_id.slice(0, 8) : 'app'}.lxobsidian.com`);
      return json({ deployment: { id, version, status: 'pending' } });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
