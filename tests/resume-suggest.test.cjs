const test = require('node:test');
const assert = require('node:assert/strict');
const { onRequestPost } = require('../functions/api/resume-suggest.js');

const url = 'https://www.lxobsidianportal.co.za/api/resume-suggest';
const fixture = {
  mode: 'bullets',
  consent: true,
  source: 'Built an Excel workbook for 4 volunteers. Contact private@example.com or +27 69 123 4567.',
  context: 'Role or project: Community stock tracker',
  target: 'Junior Analyst',
  advert: 'Experience working with a team of 50 people',
  email: 'never-send@example.com',
  name: 'Example Person'
};

function context({ body = JSON.stringify(fixture), changes = 2, origin = new URL(url).origin, run = async () => { throw Error('Unexpected provider request'); } } = {}) {
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
      RESUME_AI: { run },
      RESUME_USAGE: {
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
  assert.equal((await onRequestPost(context({ body: JSON.stringify({ ...fixture, consent: false }) }))).status, 400);
});

test('enforces the quota before contacting the provider', async t => {
  const upstream = t.mock.fn(() => { throw Error('Unexpected provider request'); });
  const response = await onRequestPost(context({ changes: 0, run: upstream }));
  assert.equal(response.status, 429);
  assert.ok(Number(response.headers.get('Retry-After')) > 0);
  assert.equal(upstream.mock.callCount(), 0);
});

test('sends only allowed fields, masks common contact data and returns grounded text', async t => {
  const wording = 'Built an Excel workbook to support 4 volunteers.';
  const upstream = t.mock.fn(async () => ({ response: { suggestion: wording } }));
  const response = await onRequestPost(context({ run: upstream }));
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { suggestion: wording });
  const [model, request] = upstream.mock.calls[0].arguments;
  assert.equal(model, '@cf/meta/llama-3.1-8b-instruct');
  const sent = JSON.parse(request.messages[1].content);
  assert.deepEqual(Object.keys(sent).sort(), ['advert', 'context', 'mode', 'source', 'target']);
  assert.match(sent.source, /\[email removed\].*\[number removed\]/);
  assert.doesNotMatch(request.messages[1].content, /example\.com|123 4567|Example Person/);
});

test('rejects a metric copied from the job advert instead of the candidate evidence', async () => {
  const run = async () => ({ response: '{"suggestion":"Built an Excel workbook for 50 people."}' });
  assert.equal((await onRequestPost(context({ run }))).status, 422);
});

test('template advice sends the application goal and accepts only existing layout IDs', async t => {
  const body = JSON.stringify({
    ...fixture,
    mode: 'template',
    target: 'First internship after my diploma. Example: private@example.com'
  });
  const run = t.mock.fn(async () => ({ response: { suggestion: 'graduate' } }));
  const result = await onRequestPost(context({ body, run }));
  assert.equal(result.status, 200);
  assert.deepEqual(await result.json(), { suggestion: 'graduate' });
  assert.deepEqual(JSON.parse(run.mock.calls[0].arguments[1].messages[1].content), {
    mode: 'template', target: 'First internship after my diploma. Example: [email removed]'
  });
  const unsupported = await onRequestPost(context({ body, run: async () => ({ response: { suggestion: 'creative' } }) }));
  assert.equal(unsupported.status, 422);
});

test('keeps the alternate inference route restricted to zero data retention', async t => {
  const fixtureContext = context({ run: async () => { throw Error('Primary temporarily unavailable'); } });
  fixtureContext.env.OPENROUTER_API_KEY = 'test-only-key';
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => Response.json({
    choices: [{ message: { content: '{"suggestion":"Built an Excel workbook to support 4 volunteers."}' } }]
  }));
  const response = await onRequestPost(fixtureContext);
  assert.equal(response.status, 200);
  const [url, options] = fetchMock.mock.calls[0].arguments;
  assert.equal(url, 'https://openrouter.ai/api/v1/chat/completions');
  assert.deepEqual(JSON.parse(options.body).provider, { data_collection: 'deny', zdr: true });
  assert.doesNotMatch(options.body, /private@example\.com|123 4567/);
});
