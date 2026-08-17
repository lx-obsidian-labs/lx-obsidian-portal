import { json, error, uuid, requireAuth, getSecretFrom } from '../_auth.js';
import { get, all, run } from '../_db.js';

export async function onRequest(context) {
  const { request } = context;
  const method = request.method;
  const path = (Array.isArray(context.params.path) ? context.params.path.join('/') : (context.params.path || '')).replace(/\/+$/, '');
  const secret = getSecretFrom(context);

  if (method === 'OPTIONS') return new Response(null, { status: 204, headers: cors() });

  const user = await requireAuth(request, secret);

  try {
    if (path === 'courses' && method === 'GET') {
      const result = await all(context, "SELECT * FROM academy_courses WHERE status = 'published' ORDER BY created_at DESC");
      return json({ courses: result.results });
    }

    if (path.match(/^courses\/[^\/]+$/) && method === 'GET') {
      const courseId = path.split('/')[1];
      const course = await get(context, 'SELECT * FROM academy_courses WHERE id = ?', courseId);
      if (!course) return error('Course not found', 404);
      const lessons = await all(context, 'SELECT * FROM course_lessons WHERE course_id = ? ORDER BY sort_order ASC', courseId);
      let enrollment = null;
      if (user) enrollment = await get(context, 'SELECT * FROM enrollments WHERE user_id = ? AND course_id = ?', user.id, courseId);
      return json({ course, lessons: lessons.results, enrollment });
    }

    if (path.match(/^courses\/[^\/]+\/enroll$/) && method === 'POST') {
      if (!user) return error('Auth required', 401);
      const courseId = path.split('/')[1];
      const exists = await get(context, 'SELECT id FROM enrollments WHERE user_id = ? AND course_id = ?', user.id, courseId);
      if (exists) return json({ enrollment: exists });
      const id = uuid();
      await run(context, 'INSERT INTO enrollments (id, user_id, course_id) VALUES (?, ?, ?)', id, user.id, courseId);
      return json({ enrollment: { id, course_id: courseId, progress: 0, completed: 0 } });
    }

    if (path.match(/^lessons\/[^\/]+\/complete$/) && method === 'POST') {
      if (!user) return error('Auth required', 401);
      const lessonId = path.split('/')[1];
      await run(context, 'INSERT OR IGNORE INTO lesson_completions (id, user_id, lesson_id) VALUES (?, ?, ?)', uuid(), user.id, lessonId);
      return json({ ok: true });
    }

    if (path === 'my-courses' && method === 'GET') {
      if (!user) return error('Auth required', 401);
      const result = await all(context, 'SELECT e.*, c.title, c.description, c.category, c.instructor, c.duration_hours, c.thumbnail FROM enrollments e JOIN academy_courses c ON e.course_id = c.id WHERE e.user_id = ? ORDER BY e.created_at DESC', user.id);
      return json({ enrollments: result.results });
    }

    return error('Not found', 404);
  } catch (e) {
    return error(e.message, 500);
  }
}

function cors() {
  return { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' };
}
