const test = require('node:test');
const assert = require('node:assert/strict');
const { onRequestPost } = require('../functions/api/resume-suggest.js');

const url = 'https://www.lxobsidianportal.co.za/api/resume-suggest';
const fixture = {
  mode: 'bullets',
  source: 'Built an Excel workbook for 4 volunteers. Contact private@example.com or +27 69 123 4567.',
  context: 'Role or project: Community stock tracker',
  target: 'Junior Analyst',
  advert: 'Experience working with a team of 50 people',
  email: 'never-send@example.com',
  name: 'Example Person'
};

function context({ body = JSON.stringify(fixture), changes = 2, origin = new URL(url).origin } = {}) {
  return {
    request: new Request(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Origin': origin,
        'CF-Connecting-IP': '192.0.2.5'
      },
      body
    }),
    env: {
      JWT_SECRET: 'test-only-secret',
      NVIDIA_API_KEY: 'test-only-key',
      DB: {
        prepare() {
          return { bind() { return { run: async () => ({ success: true, meta: { changes } }) }; } };
        }
      }
    },
    waitUntil() {}
  };
}

test('rejects invalid origin and bounds a request before inference', async () => {
  assert.equal((await onRequestPost(context({ origin: 'https://example.com' }))).status, 403);
  assert.equal((await onRequestPost(context({ body: ' '.repeat(40001) }))).status, 413);
});

test('enforces the quota before contacting the provider', async t => {
  const upstream = t.mock.method(globalThis, 'fetch', () => { throw Error('Unexpected provider request'); });
  const response = await onRequestPost(context({ changes: 0 }));
  assert.equal(response.status, 429);
  assert.ok(Number(response.headers.get('Retry-After')) > 0);
  assert.equal(upstream.mock.callCount(), 0);
});

test('sends only allowed fields, masks common contact data and returns grounded text', async t => {
  const wording = 'Built an Excel workbook to support 4 volunteers.';
  const upstream = t.mock.method(globalThis, 'fetch', async () => Response.json({
    choices: [{ message: { content: JSON.stringify({ suggestion: wording }) } }]
  }));
  const response = await onRequestPost(context());
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { suggestion: wording });
  const [hostname, request] = upstream.mock.calls[0].arguments;
  assert.equal(new URL(hostname).hostname, 'integrate.api.nvidia.com');
  const sent = JSON.parse(JSON.parse(request.body).messages[1].content);
  assert.deepEqual(Object.keys(sent).sort(), ['advert', 'context', 'mode', 'source', 'target']);
  assert.match(sent.source, /\[email removed\].*\[number removed\]/);
  assert.doesNotMatch(request.body, /example\.com|123 4567|Example Person/);
});

test('rejects a metric copied from the job advert instead of the candidate evidence', async t => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({
    choices: [{ message: { content: '{"suggestion":"Built an Excel workbook for 50 people."}' } }]
  }));
  assert.equal((await onRequestPost(context())).status, 422);
});
