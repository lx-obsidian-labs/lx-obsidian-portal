const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db/database');
const { auth } = require('../middleware/auth');

const router = express.Router();

router.get('/courses', function (req, res) {
  const db = getDb();
  const courses = db.prepare('SELECT * FROM academy_courses WHERE status = ? ORDER BY created_at DESC').all('published');
  res.json({ courses });
});

router.get('/courses/:id', function (req, res) {
  const db = getDb();
  const course = db.prepare('SELECT * FROM academy_courses WHERE id = ?').get(req.params.id);
  if (!course) return res.status(404).json({ error: 'Course not found' });
  const lessons = db.prepare('SELECT * FROM course_lessons WHERE course_id = ? ORDER BY sort_order ASC').all(req.params.id);
  res.json({ course, lessons });
});

router.post('/courses/:id/enroll', auth, function (req, res) {
  const db = getDb();
  const existing = db.prepare('SELECT id FROM enrollments WHERE user_id = ? AND course_id = ?').get(req.user.id, req.params.id);
  if (existing) return res.json({ enrollment: { id: existing.id } });
  const id = uuidv4();
  db.prepare('INSERT INTO enrollments (id, user_id, course_id) VALUES (?, ?, ?)').run(id, req.user.id, req.params.id);
  res.json({ enrollment: { id } });
});

router.get('/my-courses', auth, function (req, res) {
  const db = getDb();
  const enrollments = db.prepare('SELECT e.*, c.title, c.description, c.thumbnail, c.duration_hours, c.category FROM enrollments e JOIN academy_courses c ON e.course_id = c.id WHERE e.user_id = ? ORDER BY e.created_at DESC').all(req.user.id);
  res.json({ enrollments });
});

router.post('/lessons/:id/complete', auth, function (req, res) {
  const db = getDb();
  const existing = db.prepare('SELECT id FROM lesson_completions WHERE user_id = ? AND lesson_id = ?').get(req.user.id, req.params.id);
  if (existing) return res.json({ ok: true });
  db.prepare('INSERT INTO lesson_completions (id, user_id, lesson_id) VALUES (?, ?, ?)').run(uuidv4(), req.user.id, req.params.id);
  const lesson = db.prepare('SELECT course_id FROM course_lessons WHERE id = ?').get(req.params.id);
  if (lesson) {
    const total = db.prepare('SELECT COUNT(*) as count FROM course_lessons WHERE course_id = ?').get(lesson.course_id);
    const done = db.prepare('SELECT COUNT(*) as count FROM lesson_completions lc JOIN course_lessons cl ON lc.lesson_id = cl.id WHERE lc.user_id = ? AND cl.course_id = ?').get(req.user.id, lesson.course_id);
    const progress = total.count > 0 ? (done.count / total.count) * 100 : 0;
    db.prepare('UPDATE enrollments SET progress = ?, completed = ? WHERE user_id = ? AND course_id = ?').run(progress, progress >= 100 ? 1 : 0, req.user.id, lesson.course_id);
  }
  res.json({ ok: true });
});

router.post('/assignments/:id/submit', auth, function (req, res) {
  const { content } = req.body;
  const db = getDb();
  const id = uuidv4();
  db.prepare('INSERT INTO assignment_submissions (id, assignment_id, user_id, content) VALUES (?, ?, ?, ?)').run(id, req.params.id, req.user.id, content || '');
  res.json({ submission: { id } });
});

module.exports = router;
