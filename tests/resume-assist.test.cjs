// Engineering: LX Obsidian Labs
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const source = fs.readFileSync(path.join(__dirname, '../functions/api/resume-assist.js'));
const handler = import('data:text/javascript;base64,' + source.toString('base64')).then(module => module.onRequest);
const origin = 'https://www.lxobsidianportal.co.za';
const valid = { mode: 'bullet', source: 'Organised training materials for a class of 25.', job: 'Training support', consent: true };

function setup({ suggestion = 'Organised training materials for 25 learners.', limited = false } = {}) {
  const sent = [];
  const env = {
    JWT_SECRET: 'local test secret',
    RESUME_USAGE: {
      prepare(sql) {
        return { bind(day, actor) {
          assert.equal(typeof day, 'number');
          if (sql.startsWith('INSERT')) assert.match(actor, /^(global|[a-f0-9]{64})$/);
          return { async run() {}, async first() { return limited ? null : { count: 1 }; } };
        } };
      }
    },
    AI: { async run(_model, payload) { sent.push(payload); return { response: suggestion }; } },
  };
  return {
    sent,
    run: async (payload = valid, requestOrigin = origin) => (await handler)({
      env,
      request: new Request(origin + '/api/resume-assist', {
        method: 'POST',
        headers: { Origin: requestOrigin, 'Content-Type': 'application/json', 'CF-Connecting-IP': '192.0.2.1' },
        body: JSON.stringify(payload),
      }),
    }),
  };
}

test('requires same-origin opt-in and screens common identifiers before sending', async () => {
  const { run, sent } = setup();
  assert.equal((await run(valid, 'https://other.example')).status, 403);
  assert.equal((await run({ ...valid, consent: false })).status, 400);
  assert.equal((await run({ ...valid, mode: 'constructor' })).status, 400);
  assert.equal((await run({ ...valid, source: 'Contact me at learner@example.com' })).status, 400);
  assert.equal(sent.length, 0);
});

test('sends only selected excerpts and returns a non-cacheable suggestion', async () => {
  const { run, sent } = setup();
  const response = await run({ ...valid, fullName: 'Private Name', email: 'private@example.com' });
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('cache-control'), 'no-store');
  assert.deepEqual(JSON.parse(sent[0].messages[1].content), { source: valid.source, jobRequirements: valid.job });
  assert.deepEqual(await response.json(), { suggestion: 'Organised training materials for 25 learners.' });
});

test('rejects new numbers and skips inference after a rate limit', async () => {
  const invented = setup({ suggestion: 'Organised training for 50 learners.' });
  assert.equal((await invented.run()).status, 422);
  const rateLimited = setup({ limited: true });
  assert.equal((await rateLimited.run()).status, 429);
  assert.equal(rateLimited.sent.length, 0);
});
