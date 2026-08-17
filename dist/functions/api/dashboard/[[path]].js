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
    if ((path === '' || path === 'dashboard') && method === 'GET') {
      const [projects, orders, licenses, subscriptions, tickets, totalSpent, uploadedFiles, invoices, recentProjects, notifications] = await Promise.all([
        get(context, 'SELECT COUNT(*) as count FROM projects WHERE user_id = ?', user.id),
        get(context, 'SELECT COUNT(*) as count FROM orders WHERE user_id = ?', user.id),
        get(context, "SELECT COUNT(*) as count FROM licenses WHERE user_id = ? AND status = ?", user.id, 'active'),
        get(context, "SELECT COUNT(*) as count FROM subscriptions WHERE user_id = ? AND status = ?", user.id, 'active'),
        get(context, "SELECT COUNT(*) as count FROM support_tickets WHERE user_id = ? AND status = ?", user.id, 'open'),
        get(context, "SELECT COALESCE(SUM(amount), 0) as total FROM payments WHERE user_id = ? AND status = 'completed'", user.id),
        get(context, 'SELECT COUNT(*) as count FROM project_files WHERE uploaded_by = ?', user.id),
        all(context, 'SELECT * FROM invoices WHERE user_id = ? ORDER BY created_at DESC LIMIT 5', user.id),
        all(context, 'SELECT * FROM projects WHERE user_id = ? ORDER BY updated_at DESC LIMIT 5', user.id),
        all(context, "SELECT * FROM notifications WHERE user_id = ? AND read = 0 ORDER BY created_at DESC LIMIT 10", user.id)
      ]);
      return json({ projects, orders, licenses, subscriptions, tickets, invoices: invoices.results, recentProjects: recentProjects.results, notifications: notifications.results, totalSpent: totalSpent.total, uploadedFiles });
    }

    if (path === 'projects' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM projects WHERE user_id = ? ORDER BY created_at DESC', user.id);
      return json({ projects: result.results });
    }

    if (path === 'projects' && method === 'POST') {
      const { title, description, budget, deadline } = await request.json();
      if (!title) return error('Title is required');
      const id = uuid();
      await run(context, 'INSERT INTO projects (id, user_id, title, description, budget, deadline) VALUES (?, ?, ?, ?, ?, ?)', id, user.id, title, description || null, budget || null, deadline || null);
      return json({ project: { id, title, description, budget, deadline, status: 'active' } });
    }

    if (path.startsWith('projects/') && path.split('/').length === 2 && method === 'GET') {
      const projectId = path.split('/')[1];
      const project = await get(context, 'SELECT * FROM projects WHERE id = ? AND user_id = ?', projectId, user.id);
      if (!project) return error('Project not found', 404);
      const [tasks, milestones, files, messages, deployments] = await Promise.all([
        all(context, 'SELECT * FROM tasks WHERE project_id = ? ORDER BY created_at DESC', projectId),
        all(context, 'SELECT * FROM milestones WHERE project_id = ? ORDER BY due_date ASC', projectId),
        all(context, 'SELECT * FROM project_files WHERE project_id = ? ORDER BY created_at DESC', projectId),
        all(context, 'SELECT m.*, u.name as user_name FROM project_messages m JOIN users u ON m.user_id = u.id WHERE m.project_id = ? ORDER BY m.created_at ASC', projectId),
        all(context, 'SELECT * FROM deployments WHERE project_id = ? ORDER BY created_at DESC', projectId)
      ]);
      return json({ project, tasks: tasks.results, milestones: milestones.results, files: files.results, messages: messages.results, deployments: deployments.results });
    }

    if (path.match(/^projects\/[^\/]+\/tasks$/) && method === 'POST') {
      const projectId = path.split('/')[1];
      const { title, description, priority, due_date, assignee } = await request.json();
      if (!title) return error('Title is required');
      const id = uuid();
      await run(context, 'INSERT INTO tasks (id, project_id, title, description, priority, due_date, assignee) VALUES (?, ?, ?, ?, ?, ?, ?)', id, projectId, title, description || null, priority || 'medium', due_date || null, assignee || null);
      return json({ task: { id, title, description, priority, due_date, status: 'todo' } });
    }

    if (path.match(/^projects\/[^\/]+\/milestones$/) && method === 'POST') {
      const projectId = path.split('/')[1];
      const { title, description, due_date, amount } = await request.json();
      if (!title) return error('Title is required');
      const id = uuid();
      await run(context, 'INSERT INTO milestones (id, project_id, title, description, due_date, amount) VALUES (?, ?, ?, ?, ?, ?)', id, projectId, title, description || null, due_date || null, amount || null);
      return json({ milestone: { id, title, description, due_date, amount, status: 'pending' } });
    }

    if (path.match(/^projects\/[^\/]+\/files$/) && method === 'POST') {
      const projectId = path.split('/')[1];
      const { name, path: filePath, size, type } = await request.json();
      if (!name) return error('Name is required');
      const id = uuid();
      await run(context, 'INSERT INTO project_files (id, project_id, name, path, size, type, uploaded_by) VALUES (?, ?, ?, ?, ?, ?, ?)', id, projectId, name, filePath || '', size || 0, type || '', user.id);
      return json({ file: { id, name, size, type } });
    }

    if (path.match(/^projects\/[^\/]+\/messages$/) && method === 'POST') {
      const projectId = path.split('/')[1];
      const { content } = await request.json();
      if (!content) return error('Content is required');
      const id = uuid();
      await run(context, 'INSERT INTO project_messages (id, project_id, user_id, content) VALUES (?, ?, ?, ?)', id, projectId, user.id, content);
      return json({ message: { id, content, user_id: user.id } });
    }

    if (path.match(/^tasks\/[^\/]+\/status$/) && method === 'PUT') {
      const taskId = path.split('/')[1];
      const { status } = await request.json();
      await run(context, 'UPDATE tasks SET status = ? WHERE id = ?', status, taskId);
      return json({ ok: true });
    }

    if (path === 'licenses' && method === 'GET') {
      const result = await all(context, 'SELECT l.*, p.name as product_name FROM licenses l JOIN store_products p ON l.product_id = p.id WHERE l.user_id = ? ORDER BY l.created_at DESC', user.id);
      return json({ licenses: result.results });
    }

    if (path === 'subscriptions' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM subscriptions WHERE user_id = ? ORDER BY created_at DESC', user.id);
      return json({ subscriptions: result.results });
    }

    if (path === 'support' && method === 'GET') {
      const result = await all(context, 'SELECT * FROM support_tickets WHERE user_id = ? ORDER BY created_at DESC', user.id);
      return json({ tickets: result.results });
    }

    if (path === 'support' && method === 'POST') {
      const { subject, message } = await request.json();
      if (!subject) return error('Subject is required');
      const ticketId = uuid();
      await run(context, 'INSERT INTO support_tickets (id, user_id, subject) VALUES (?, ?, ?)', ticketId, user.id, subject);
      if (message) {
        const msgId = uuid();
        await run(context, 'INSERT INTO ticket_messages (id, ticket_id, user_id, content) VALUES (?, ?, ?, ?)', msgId, ticketId, user.id, message);
      }
      return json({ ticket: { id: ticketId, subject, status: 'open' } });
    }

    if (path === 'messages' && method === 'GET') {
      const result = await all(context, `SELECT DISTINCT m.*, u.name as user_name, p.title as project_title FROM project_messages m JOIN users u ON m.user_id = u.id JOIN projects p ON m.project_id = p.id WHERE p.user_id = ? ORDER BY m.created_at DESC LIMIT 50`, user.id);
      return json({ messages: result.results });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
