import { test } from 'node:test';
import assert from 'node:assert/strict';
import worker, { validateClaims, validatePlan, parsePlan } from '../src/worker.mjs';
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
