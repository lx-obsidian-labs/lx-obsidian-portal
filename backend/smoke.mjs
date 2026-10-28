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
  const response = await fetch(`${base}/api/assistant`, { method: 'POST', signal: AbortSignal.timeout(30000), headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${idToken}` }, body: JSON.stringify({ message: 'Please find a funny movie for tonight' }) });
  const result = await response.json();
  if (!response.ok || !['recommend', 'search'].includes(result.tool)) throw new Error(`Authenticated smoke test ${response.status}: ${JSON.stringify(result)}`);
  console.log(`Authenticated Firebase → Worker → provider → validated plan: passed (${result.tool}).`);
} catch (error) { console.error(error.message); process.exitCode = 1; }
finally {
  if (idToken) { await identity('delete', { idToken }); console.log('Temporary test identity deleted.'); }
}
