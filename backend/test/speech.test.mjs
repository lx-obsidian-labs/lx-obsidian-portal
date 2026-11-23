import { test } from 'node:test';
import assert from 'node:assert/strict';
import { speechText, synthesizeSpeech } from '../src/speech.mjs';
import worker from '../src/worker.mjs';

test('speech accepts only bounded readable text and strips spoken URLs/citations', () => {
  for (const input of [null, {}, { text: '' }, { text: 'x'.repeat(1801) }]) assert.throws(() => speechText(input));
  assert.equal(speechText({ text: '**A good movie** [1] https://publisher.org/story' }), 'A good movie');
});

test('cloud voice streams SDK audio without exposing the key or buffering output', async () => {
  let observed;
  const response = await synthesizeSpeech({ ELEVENLABS_API_KEY: 'test-secret' }, 'Hello Vista', undefined, options => {
    assert.equal(options.apiKey, 'test-secret');
    return { textToSpeech: { convert: async (voice, request, settings) => {
      observed = { voice, request, settings };
      return new ReadableStream({ start(controller) { controller.enqueue(new Uint8Array([1, 2, 3])); controller.close(); } });
    } } };
  });
  assert.equal(response.headers.get('Content-Type'), 'audio/mpeg');
  assert.equal(response.headers.get('Cache-Control'), 'no-store');
  assert.deepEqual([...new Uint8Array(await response.arrayBuffer())], [1, 2, 3]);
  assert.equal(observed.request.modelId, 'eleven_flash_v2_5');
  assert.equal(observed.settings.maxRetries, 0);
});

test('unconfigured voice and provider failure degrade without leaking credentials', async () => {
  assert.equal((await synthesizeSpeech({}, 'Hello')).status, 503);
  const response = await synthesizeSpeech({ ELEVENLABS_API_KEY: 'test-secret' }, 'Hello', undefined,
    () => ({ textToSpeech: { convert: async () => { throw new Error('test-secret'); } } }));
  assert.equal(response.status, 502);
  assert.doesNotMatch(await response.text(), /test-secret/);
});

test('voice endpoint requires Firebase authentication before any synthesis', async () => {
  const response = await worker.fetch(new Request('https://vista.test/api/voice', { method: 'POST', body: '{"text":"Hello"}' }), {});
  assert.equal(response.status, 401);
});

test('cancelling client audio cancels the upstream stream', async () => {
  let cancelled = false;
  const response = await synthesizeSpeech({ ELEVENLABS_API_KEY: 'test-secret' }, 'Hello', undefined,
    () => ({ textToSpeech: { convert: async () => new ReadableStream({ cancel() { cancelled = true; } }) } }));
  await response.body.cancel();
  assert.equal(cancelled, true);
});

test('installed SDK sends the bounded speech request with server-side authentication', async () => {
  const original = globalThis.fetch;
  let calls = 0;
  try {
    globalThis.fetch = async (url, init) => {
      calls++;
      assert.match(String(url), /^https:\/\/api\.elevenlabs\.io\/v1\/text-to-speech\/JBFqnCBsd6RMkjVDRZzb/);
      assert.equal(new Headers(init.headers).get('xi-api-key'), 'sdk-test-key');
      const body = JSON.parse(init.body);
      assert.equal(body.model_id, 'eleven_flash_v2_5');
      assert.equal(body.text, 'Mocked SDK request');
      return new Response(new Uint8Array([1, 2]), { headers: { 'Content-Type': 'audio/mpeg' } });
    };
    const response = await synthesizeSpeech({ ELEVENLABS_API_KEY: 'sdk-test-key' }, 'Mocked SDK request');
    assert.equal(response.status, 200);
    assert.equal((await response.arrayBuffer()).byteLength, 2);
    assert.equal(calls, 1);
  } finally { globalThis.fetch = original; }
});
