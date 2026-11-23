import { readFile } from 'node:fs/promises';
import { randomBytes } from 'node:crypto';
const config = JSON.parse(await readFile(new URL('../app/google-services.json', import.meta.url), 'utf8'));
const key = config.client[0].api_key[0].current_key;
const base = process.env.VISTA_AI_BASE_URL || 'https://vista-assistant.feedpatelmusic.workers.dev';
let idToken;
async function identity(method, data) {
  const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:${method}?key=${key}`, {
    method: 'POST', signal: AbortSignal.timeout(20000), headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
  });
  const result = await response.json();
  if (!response.ok) throw new Error(`Firebase smoke test ${response.status}: ${result.error?.message}`);
  return result;
}
try {
  const denied = await fetch(`${base}/api/assistant`, { method: 'POST', body: '{}' });
  if (denied.status !== 401) throw new Error('Unauthenticated request was not rejected.');
  console.log('Unauthenticated endpoint check: passed.');
  const user = await identity('signUp', { email: `vista-smoke-${Date.now()}@example.com`, password: `Vista!9${randomBytes(24).toString('hex')}`, returnSecureToken: true });
  idToken = user.idToken;
  if (process.argv.includes('--voice')) {
    const response = await fetch(`${base}/api/voice`, {
      method: 'POST', signal: AbortSignal.timeout(30000),
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${idToken}` },
      body: JSON.stringify({ text: 'Hello from Vista.' })
    });
    if (!response.ok || !response.headers.get('Content-Type')?.startsWith('audio/')) {
      throw new Error(`Authenticated voice smoke test failed (HTTP ${response.status}).`);
    }
    const bytes = (await response.arrayBuffer()).byteLength;
    if (!bytes || bytes > 2_000_000) throw new Error('Voice returned empty or oversized audio.');
    console.log(`Authenticated Firebase → Worker → ElevenLabs audio: passed (${bytes} bytes).`);
  }
  async function ask(input) {
    const response = await fetch(`${base}/api/assistant`, { method: 'POST', signal: AbortSignal.timeout(60000), headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${idToken}` }, body: JSON.stringify(input) });
    const result = await response.json();
    if (!response.ok) throw new Error(`Authenticated smoke test ${response.status}: ${JSON.stringify(result)}`);
    if (process.argv.includes('--require-nvidia') && result.provider !== 'nvidia') throw new Error(`Expected NVIDIA, received ${result.provider}.`);
    return result;
  }
  const plan = await ask({ message: 'Please open Data and Storage settings' });
  if (plan.tool !== 'navigate' || plan.query !== 'data_storage') throw new Error(`Page routing failed: ${JSON.stringify(plan)}`);
  console.log(`Authenticated Firebase → Worker → validated page action: passed (provider: ${plan.provider}).`);
  const profile = JSON.stringify({ name: 'Mia', favoriteGenres: ['Comedy'], currentPage: 'Home' });
  const message = 'What is my name, and where can I repair playback in this app?';
  const reply = await ask({ mode: 'reply', message, profile, context: 'No title lookup requested.', spoilers: false });
  if (!reply.text?.includes('Mia') || !/repair|storage/i.test(reply.text)) throw new Error(`App/profile grounding failed: ${JSON.stringify(reply)}`);
  console.log(`Conversational app knowledge + profile name: passed (provider: ${reply.provider}).`);
  const followup = await ask({ mode: 'reply', message: 'What did I just ask you about?', profile, context: 'No title lookup requested.', spoilers: false, history: [{ role: 'user', content: message }, { role: 'assistant', content: reply.text }] });
  if (!/repair|playback/i.test(followup.text) || followup.text === reply.text) throw new Error(`Conversation continuity failed: ${JSON.stringify(followup)}`);
  console.log(`Follow-up conversation memory: passed (provider: ${followup.provider}).`);
  if (process.argv.includes('--agentic')) {
    const recent = JSON.stringify({ name: 'Mia', recentResults: [
      { position: 1, title: 'Inception', type: 'movie' }, { position: 2, title: 'Interstellar', type: 'movie' }
    ] });
    const selected = await ask({ agentic: true, message: 'Download the second one', profile: recent });
    if (selected.tool !== 'download' || (selected.selection !== 2 && selected.query.toLowerCase() !== 'interstellar')) throw new Error(`Numbered download action failed: ${JSON.stringify(selected)}`);
    console.log(`Agentic numbered download review: passed (provider: ${selected.provider}, reference: ${selected.selection === 2 ? 'selection 2' : 'exact title Interstellar'}).`);
    const voice = await ask({ agentic: true, message: 'Open AI and voice settings', profile });
    if (voice.tool !== 'navigate' || voice.query !== 'ai_voice') throw new Error(`AI settings action failed: ${JSON.stringify(voice)}`);
    console.log(`Agentic AI & Voice destination: passed (provider: ${voice.provider}).`);
    const library = await ask({ agentic: true, message: 'Show my watchlist', profile });
    if (library.tool !== 'watchlist') throw new Error(`Watchlist lookup action failed: ${JSON.stringify(library)}`);
    console.log(`Agentic owned-library lookup: passed (provider: ${library.provider}).`);
  }
} catch (error) { console.error(error.message); process.exitCode = 1; }
finally {
  if (idToken) { await identity('delete', { idToken }); console.log('Temporary test identity deleted.'); }
}
