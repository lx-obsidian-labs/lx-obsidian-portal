// Use only the SDK's TTS HTTP resource; root exports include Node-only playback wrappers.
import { TextToSpeechClient } from '@elevenlabs/elevenlabs-js/api/resources/textToSpeech/client/Client.js';

export function speechText(input) {
  if (!input || typeof input.text !== 'string' || !input.text.trim() || input.text.length > 1800) {
    throw new TypeError('Use between 1 and 1,800 characters for speech.');
  }
  return input.text.replace(/https?:\/\/\S+/gi, '')
    .replace(/\[\d+\]/g, '').replace(/[*#`]/g, '').trim();
}

export async function synthesizeSpeech(env, text, signal, makeClient = options => ({ textToSpeech: new TextToSpeechClient(options) })) {
  if (!env.ELEVENLABS_API_KEY) return Response.json({ error: 'Cloud voice is unavailable. Use the device voice.' }, { status: 503, headers: { 'Cache-Control': 'no-store' } });
  try {
    const client = makeClient({ apiKey: env.ELEVENLABS_API_KEY });
    const audio = await client.textToSpeech.convert(env.ELEVENLABS_VOICE_ID || 'JBFqnCBsd6RMkjVDRZzb', {
      text, modelId: 'eleven_flash_v2_5', outputFormat: 'mp3_44100_128'
    }, { abortSignal: signal, timeoutInSeconds: 12, maxRetries: 0 });
    // Bound the stream without buffering it. Cancel provider reads when the viewer disconnects.
    const reader = audio.getReader();
    let bytes = 0;
    const stream = new ReadableStream({
      async pull(controller) {
        try {
          const { done, value } = await reader.read();
          if (done) { reader.releaseLock(); controller.close(); return; }
          bytes += value.byteLength;
          if (bytes > 2_000_000) { await reader.cancel(); controller.error(new Error('Speech response too large')); return; }
          controller.enqueue(value);
        } catch { controller.error(new Error('Speech stream interrupted')); }
      },
      cancel(reason) { return reader.cancel(reason); }
    });
    return new Response(stream, { headers: { 'Content-Type': 'audio/mpeg', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' } });
  } catch {
    return Response.json({ error: 'Cloud voice is unavailable. Use the device voice.' }, { status: 502, headers: { 'Cache-Control': 'no-store' } });
  }
}
