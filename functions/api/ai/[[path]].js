import { json, error, uuid, requireAuth, getSecretFrom } from '../_auth.js';
import { get, all, run } from '../_db.js';

export async function onRequest(context) {
  const { request } = context;
  const method = request.method;
  const path = (Array.isArray(context.params.path) ? context.params.path.join('/') : (context.params.path || '')).replace(/\/+$/, '');
  const secret = getSecretFrom(context);

  if (method === 'OPTIONS') return new Response(null, { status: 204, headers: cors() });

  try {
    if (path === 'agents' && method === 'GET') {
      const result = await all(context, "SELECT * FROM ai_agents WHERE status = 'active' ORDER BY name");
      return json({ agents: result.results });
    }

    if (path === 'chats' && method === 'GET') {
      const user = await requireAuth(request, secret);
      if (!user) return error('Auth required', 401);
      const result = await all(context, 'SELECT c.*, a.name as agent_name FROM ai_chats c JOIN ai_agents a ON c.agent_id = a.id WHERE c.user_id = ? ORDER BY c.created_at DESC', user.id);
      return json({ chats: result.results });
    }

    if (path === 'chats' && method === 'POST') {
      const user = await requireAuth(request, secret);
      if (!user) return error('Auth required', 401);
      const { agent_id, title } = await request.json();
      if (!agent_id) return error('Agent ID required');
      const id = uuid();
      await run(context, 'INSERT INTO ai_chats (id, user_id, agent_id, title) VALUES (?, ?, ?, ?)', id, user.id, agent_id, title || null);
      return json({ chat: { id, agent_id, title } });
    }

    if (path.match(/^chats\/[^\/]+\/messages$/) && method === 'GET') {
      const user = await requireAuth(request, secret);
      if (!user) return error('Auth required', 401);
      const chatId = path.split('/')[1];
      const result = await all(context, 'SELECT * FROM ai_messages WHERE chat_id = ? ORDER BY created_at ASC', chatId);
      return json({ messages: result.results });
    }

    if (path.match(/^chats\/[^\/]+\/messages$/) && method === 'POST') {
      const user = await requireAuth(request, secret);
      if (!user) return error('Auth required', 401);
      const chatId = path.split('/')[1];
      const { content } = await request.json();
      if (!content) return error('Content required');
      const id = uuid();
      await run(context, "INSERT INTO ai_messages (id, chat_id, role, content) VALUES (?, ?, 'user', ?)", id, chatId, content);
      const agent = await get(context, 'SELECT a.* FROM ai_chats c JOIN ai_agents a ON c.agent_id = a.id WHERE c.id = ?', chatId);
      const replyId = uuid();
      const reply = `Thanks for your message! Our AI will get back to you shortly. (Using ${agent ? agent.name : 'AI'} assistant)`;
      await run(context, "INSERT INTO ai_messages (id, chat_id, role, content) VALUES (?, ?, 'assistant', ?)", replyId, chatId, reply);
      return json({ message: { id, content, role: 'user' }, reply: { id: replyId, content: reply, role: 'assistant' } });
    }

    if (path === 'credits' && method === 'GET') {
      const user = await requireAuth(request, secret);
      if (!user) return error('Auth required', 401);
      const u = await get(context, 'SELECT credits FROM users WHERE id = ?', user.id);
      return json({ credits: u ? u.credits : 0 });
    }

    if (path === 'credits/purchase' && method === 'POST') {
      const user = await requireAuth(request, secret);
      if (!user) return error('Auth required', 401);
      const { amount } = await request.json();
      await run(context, 'UPDATE users SET credits = credits + ? WHERE id = ?', amount || 10, user.id);
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
