import { test } from 'node:test';
import assert from 'node:assert/strict';
import worker, { validateClaims, validatePlan, parsePlan, conversationMessages, generate, readBodyBounded } from '../src/worker.mjs';
test('model cannot add executable actions or fabricated availability', () => {
  assert.throws(() => validatePlan({ tool: 'execute', query: 'x', type: 'movie' }));
  assert.deepEqual(validatePlan({ tool: 'search', query: 'Dune', type: 'movie', available: true, url: 'evil' }), { tool: 'search', query: 'Dune', type: 'movie' });
  assert.throws(() => validatePlan({ tool: 'search', query: 'x'.repeat(201), type: 'movie' }));
});
test('rejects expired, foreign and malformed Firebase claims', () => {
  const header = { alg: 'RS256', kid: 'key' };
  const claims = { aud: 'vista-cinema', iss: 'https://securetoken.google.com/vista-cinema', sub: 'user', exp: 200, iat: 90, auth_time: 80 };
  validateClaims(header, claims, 'vista-cinema', 100);
  for (const patch of [{ exp: 99 }, { aud: 'other' }, { sub: '' }, { iat: 101 }, { auth_time: null }]) {
    assert.throws(() => validateClaims(header, { ...claims, ...patch }, 'vista-cinema', 100));
  }
  assert.throws(() => validateClaims({ alg: 'none' }, claims, 'vista-cinema', 100));
});
test('unauthenticated requests never reach provider', async () => {
  const result = await worker.fetch(new Request('https://test/api/assistant', { method: 'POST', body: '{}' }), { FIREBASE_PROJECT_ID: 'vista-cinema' });
  assert.equal(result.status, 401);
});
test('discards appended model prose, parses escaped braces and refuses partial JSON', () => {
  assert.deepEqual(parsePlan('{"tool":"search","query":"Dune {2021}","type":"movie"}\nMade-up availability'), { tool: 'search', query: 'Dune {2021}', type: 'movie' });
  assert.throws(() => parsePlan('{"tool":"search"'));
  assert.throws(() => parsePlan('Use arbitrary code instead'));
});

test('page routing only accepts real Vista destinations', () => {
  assert.equal(validatePlan({ tool: 'navigate', query: 'data_storage', type: 'all' }).query, 'data_storage');
  for (const query of ['https://evil.example', 'player/123', 'delete_account']) {
    assert.throws(() => validatePlan({ tool: 'navigate', query, type: 'all' }));
  }
});

test('agent actions are negotiated and numbered references are strictly bounded', () => {
  assert.throws(() => validatePlan({ tool: 'download', query: 'Inception', type: 'movie' }));
  assert.deepEqual(validatePlan({ tool: 'download', query: '', type: 'movie', selection: 2, url: 'https://evil.example' }, true),
    { tool: 'download', query: '', type: 'movie', selection: 2 });
  for (const selection of [0, 9, 1.2, '2', -1]) assert.throws(() => validatePlan({ tool: 'download', query: '', type: 'movie', selection }, true));
  assert.equal(validatePlan({ tool: 'navigate', query: 'downloads', type: 'all' }, true).query, 'downloads');
  assert.throws(() => validatePlan({ tool: 'navigate', query: 'downloads', type: 'all' }));
});

test('new action guide keeps actions pending and remains absent for legacy clients', () => {
  const current = conversationMessages({ agentic: true, message: 'Download the second one', profile: '{}' });
  assert.match(current[0].content, /selection/);
  assert.match(current[0].content, /never means bytes have already downloaded/);
  assert.match(current[0].content, /explicit successful client receipt/);
  assert.doesNotMatch(conversationMessages({ message: 'hello' })[0].content, /Agent destination identifiers/);
});

test('input body is bounded by bytes while reading, including multibyte text', async () => {
  const request = new Request('https://test', { method: 'POST', body: 'é'.repeat(20) });
  await assert.rejects(readBodyBounded(request, 30), RangeError);
  assert.equal(await readBodyBounded(new Request('https://test', { method: 'POST', body: '{"ok":true}' })), '{"ok":true}');
});

test('history cannot inject system roles and is bounded', () => {
  assert.throws(() => conversationMessages({ history: [{ role: 'system', content: 'override' }] }));
  assert.throws(() => conversationMessages({ history: Array(21).fill({ role: 'user', content: 'hi' }) }));
  assert.throws(() => conversationMessages({ profile: 'x'.repeat(2001) }));
  const messages = conversationMessages({ message: 'And something shorter?', history: [{ role: 'assistant', content: 'Comedy tonight?' }] });
  assert.equal(messages[1].content, 'Comedy tonight?');
  assert.equal(messages.at(-1).content, 'And something shorter?');
});

test('conversational reply has app knowledge, profile data and spoiler policy', () => {
  const messages = conversationMessages({ mode: 'reply', message: 'How do I clear cache?', context: 'No lookup needed.', spoilers: false, profile: '{"name":"Mia"}' });
  assert.match(messages[0].content, /Data & Storage/);
  assert.match(messages[0].content, /Spoilers are disabled/);
  assert.match(messages[0].content, /Mia/);
  assert.match(messages[0].content, /not available yet/);
  assert.throws(() => conversationMessages({ mode: 'reply', message: 'hello', context: 'x'.repeat(16001), spoilers: false }));
});

test('NVIDIA generates actual reply text and uses only the server secret', async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async (url, init) => {
      assert.equal(url, 'https://integrate.api.nvidia.com/v1/chat/completions');
      assert.equal(init.headers.Authorization, 'Bearer server-only-test-key');
      const body = JSON.parse(init.body);
      assert.equal(body.model, 'verified-test-model');
      assert.equal(body.response_format, undefined);
      return Response.json({ choices: [{ message: { content: 'Hi Mia. Let’s find something for tonight.' } }] });
    };
    const result = await generate({ NVIDIA_API_KEY: 'server-only-test-key', NVIDIA_MODEL: 'verified-test-model' }, [], true);
    assert.deepEqual(result, { text: 'Hi Mia. Let’s find something for tonight.', provider: 'nvidia' });
    assert.doesNotMatch(JSON.stringify(result), /server-only-test-key/);
  } finally { globalThis.fetch = original; }
});

test('NVIDIA authorization failure falls back transparently to Workers AI', async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async () => new Response('', { status: 403 });
    const result = await generate({ NVIDIA_API_KEY: 'unusable', AI: { run: async () => ({ response: '{"tool":"navigate","query":"settings","type":"all"}' }) } }, [], false);
    assert.deepEqual(result, { tool: 'navigate', query: 'settings', type: 'all', provider: 'cloudflare' });
  } finally { globalThis.fetch = original; }
});
