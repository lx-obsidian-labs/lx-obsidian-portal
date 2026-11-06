const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const source = fs.readFileSync(path.join(__dirname, '../functions/api/contact.js'), 'utf8');
const moduleUrl = 'data:text/javascript;base64,' + Buffer.from(source).toString('base64');
const handler = import(moduleUrl).then(mod => mod.onRequest);
const valid = {
  name: 'Test Client', email: 'client@example.com', subject: 'New software platform',
  message: 'We need a new client portal for our business.',
  projecttype: 'Custom software', industry: 'Retail'
};

function context(payload, options = {}) {
  const origin = options.origin || 'https://lxobsidianportal.co.za';
  const request = new Request('https://lxobsidianportal.co.za/api/contact', {
    method: 'POST',
    headers: {'Origin': origin, 'Content-Type': options.contentType || 'application/json'},
    body: typeof payload === 'string' ? payload : JSON.stringify(payload)
  });
  let writes = 0;
  const env = options.noDB ? {} : {DB: {prepare() {
    return {bind() {return {async run() {writes++;}};}};
  }}};
  return {request, env, waitUntil() {}, getWrites: () => writes};
}

test('rejects cross-origin enquiries before storage', async () => {
  const ctx = context(valid, {origin: 'https://untrusted.example'});
  const response = await (await handler)(ctx);
  assert.equal(response.status, 403);
  assert.equal(ctx.getWrites(), 0);
});

test('rejects malformed payloads before storage', async () => {
  const ctx = context({...valid, email: 'not-an-email'});
  const response = await (await handler)(ctx);
  assert.equal(response.status, 400);
  assert.equal(ctx.getWrites(), 0);
});

test('reports unavailable storage instead of claiming success', async () => {
  const ctx = context(valid, {noDB: true});
  const response = await (await handler)(ctx);
  assert.equal(response.status, 503);
});

test('stores a valid enquiry and returns a reference', async () => {
  const ctx = context(valid);
  const response = await (await handler)(ctx);
  assert.equal(response.status, 201);
  assert.equal(ctx.getWrites(), 1);
  const body = await response.json();
  assert.equal(body.saved, true);
  assert.match(body.reference, /^LX-/);
});

test('honeypot does not store an enquiry', async () => {
  const ctx = context({...valid, company_website: 'https://spam.example'});
  const response = await (await handler)(ctx);
  assert.equal(response.status, 200);
  assert.equal(ctx.getWrites(), 0);
});
