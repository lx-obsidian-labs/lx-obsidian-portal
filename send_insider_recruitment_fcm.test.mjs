import test from 'node:test';
import assert from 'node:assert/strict';
import {
  APPLY_URL, AUDIENCES, WAVES, buildMessage, complianceIssues, verifyTargets,
  fetchWithRetry, hasBeenSent, recordSend
} from './send_insider_recruitment_fcm.mjs';

test('every wave is compliant, shows the apply address and fits the FCM payload', () => {
  for (const wave of [1, 2, 3]) {
    const message = buildMessage({ wave, audience: 'za' });
    assert.equal(message.topic, AUDIENCES.za);
    assert.equal(message.data.category, 'promotions');
    assert.equal(message.data.check_update, 'false');
    assert.equal(message.data.action_url, APPLY_URL);
    assert.equal(message.data.announcement_id, WAVES[wave].id);
    assert.equal(message.android.collapse_key, WAVES[wave].id);
    assert.ok(message.data.body.includes('lxobsidianportal.co.za/insider'));
    assert.ok(/voluntary/i.test(message.data.body));
    assert.deepEqual(complianceIssues(message.data.title, message.data.body), []);
    assert.ok(Buffer.byteLength(JSON.stringify(message), 'utf8') <= 4096);
  }
});

test('rejects unknown waves, audiences and non-HTTPS apply URLs', () => {
  assert.throws(() => buildMessage({ wave: 9 }), /Unknown wave/);
  assert.throws(() => buildMessage({ audience: 'everyone' }), /Unknown audience/);
  assert.throws(() => buildMessage({ applyUrl: 'http://example.com' }), /HTTPS/);
});

test('forbidden claims are detected before anything can be sent', () => {
  assert.ok(complianceIssues('Win R500 airtime', 'Guaranteed prize for a 5-star review').length >= 3);
  assert.deepEqual(complianceIssues('LX Insider', 'Voluntary testing with verified-work rewards.'), []);
});

test('an overridden apply URL is reflected in both the body and the deep link', () => {
  const message = buildMessage({ wave: 1, applyUrl: 'https://www.lxobsidianportal.co.za/pocket-business' });
  assert.ok(message.data.body.includes('lxobsidianportal.co.za/pocket-business'));
  assert.ok(!message.data.body.includes('/insider'));
  assert.equal(message.data.action_url, 'https://www.lxobsidianportal.co.za/pocket-business');
});

test('verification blocks sending when a destination is down or unbranded', async () => {
  const ok = body => async () => ({ ok: true, status: 200, url: 'https://www.lxobsidianportal.co.za/x', text: async () => body });
  const checks = await verifyTargets({ fetchImpl: url =>
    url.includes('play.google.com')
      ? Promise.resolve({ ok: true, status: 200, url: 'https://accounts.google.com/signin', text: async () => '' })
      : ok(url.includes('pocket-business') ? 'LX Pocket Business beta' : 'LX Insider application')(), retryOptions: { delayMs: 0 } });
  assert.equal(checks.length, 3);
  assert.ok(checks.every(check => check.verified));
  assert.match(checks[2].note, /non-enrolled Google account/);

  await assert.rejects(verifyTargets({ fetchImpl: async () => ({ ok: false, status: 404, url: '', text: async () => '' }), retryOptions: { delayMs: 0 } }), /did not pass verification/);
  await assert.rejects(verifyTargets({ fetchImpl: url =>
    url.includes('play.google.com')
      ? Promise.resolve({ ok: true, status: 200, url: '', text: async () => '' })
      : ok('some unrelated page')(), retryOptions: { delayMs: 0 } }), /did not pass verification/);
  await assert.rejects(verifyTargets({ fetchImpl: url =>
    url.includes('play.google.com')
      ? { ok: false, status: 404, url: '', text: async () => '' }
      : ok(url.includes('pocket-business') ? 'LX Pocket Business beta' : 'LX Insider application')(), retryOptions: { delayMs: 0 } }), /Google Play testing link/);
});

test('transient network failures retry; persistent failures surface clearly', async () => {
  let calls = 0;
  const flaky = async () => { calls++; if (calls < 3) throw new Error('temporary'); return { ok: true }; };
  assert.deepEqual(await fetchWithRetry(flaky, 'https://example.com', {}, { delayMs: 0 }), { ok: true });
  assert.equal(calls, 3);
  await assert.rejects(fetchWithRetry(async () => { throw new Error('down'); }, 'https://example.com', {}, { attempts: 2, delayMs: 0 }), /failed after 2 attempts/);
});

test('the send ledger prevents accidental duplicate broadcasts', async () => {
  let content = '';
  const options = {
    file: 'campaign/fcm-send-log.jsonl',
    read: async () => content,
    append: async (_file, text) => { content += text; },
    makeDir: async () => {}
  };
  assert.equal(await hasBeenSent('lx-insider-recruit-2026-w1', 'za', options), false);
  await recordSend({ announcement_id: 'lx-insider-recruit-2026-w1', audience: 'za', name: 'msg-1' }, options);
  assert.equal(await hasBeenSent('lx-insider-recruit-2026-w1', 'za', options), true);
  assert.equal(await hasBeenSent('lx-insider-recruit-2026-w1', 'row', options), false);
  assert.equal(await hasBeenSent('lx-insider-recruit-2026-w2', 'za', options), false);
  content += '\nnot json\n';
  assert.equal(await hasBeenSent('lx-insider-recruit-2026-w1', 'za', options), true);
});
