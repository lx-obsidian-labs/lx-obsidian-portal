import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const load = async path => import('data:text/javascript;base64,' + Buffer.from(await readFile(new URL(path, import.meta.url))).toString('base64'));
const { onRequest: contact } = await load('../functions/api/contact.js');
const { onRequest: subscribe } = await load('../functions/api/subscribe.js');
const brief = { intent: 'partnership', name: 'Test User', email: 'TEST@example.com', subject: 'Integration pilot', message: 'Please discuss a small pilot.', company: 'Test Company' };
function context(body, DB, method = 'POST') {
  return { request: new Request('https://example.test/api/contact', { method, ...(method === 'POST' ? { body: JSON.stringify(body) } : {}) }), env: { DB } };
}
function database(result = { success: true }) {
  const calls = [];
  return { calls, prepare(sql) { return { bind(...values) { calls.push({ sql, values }); return { async run() { if (result instanceof Error) throw result; return result; } }; } }; } };
}
test('contact persists the full brief and route before confirming', async () => {
  const db = database();
  const response = await contact(context(brief, db));
  assert.equal(response.status, 201);
  assert.deepEqual(await response.json(), { ok: true, stored: true });
  assert.deepEqual(db.calls[0].values.slice(0, 6), ['partnership', 'Test User', 'test@example.com', 'Test Company', brief.subject, brief.message]);
});
test('contact never acknowledges absent, failed or unsuccessful storage', async () => {
  for (const db of [undefined, database(new Error('offline')), database({ success: false })]) {
    const response = await contact(context(brief, db));
    assert.equal(response.status, 503);
    assert.equal((await response.json()).ok, undefined);
  }
});
test('contact rejects malformed or incomplete input without writing', async () => {
  for (const body of [null, [], { ...brief, email: 'invalid' }, { ...brief, message: '' }, { ...brief, name: 7 }, { ...brief, intent: 'admin' }, { ...brief, message: 'x'.repeat(10001) }]) {
    const db = database();
    assert.equal((await contact(context(body, db))).status, 400);
    assert.equal(db.calls.length, 0);
  }
  assert.equal((await contact(context({}, undefined, 'GET'))).status, 405);
});
test('subscriber confirms storage and normalises the email', async () => {
  const db = database();
  const response = await subscribe(context({ email: ' TEST@example.com ', source: 'footer' }, db));
  assert.equal(response.status, 200);
  assert.equal((await response.json()).stored, true);
  assert.equal(db.calls[0].values[0], 'test@example.com');
});
test('subscriber rejects invalid input and never claims a failed write succeeded', async () => {
  for (const db of [undefined, database(new Error('missing table')), database({ success: false })]) {
    const response = await subscribe(context({ email: 'test@example.com' }, db));
    assert.equal(response.status, 503);
    assert.equal((await response.json()).ok, undefined);
  }
  for (const body of [null, {}, { email: 123 }, { email: 'invalid' }]) assert.equal((await subscribe(context(body, database()))).status, 400);
});
