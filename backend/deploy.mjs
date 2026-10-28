// Deployment credentials stay in the local .env; only the NVIDIA key becomes a Worker secret.
import { readFile } from 'node:fs/promises';
const local = await readFile(new URL('../.env.deploy', import.meta.url), 'utf8');
const vars = Object.fromEntries(local.split(/\r?\n/).filter(line => /^[A-Z_]+=/.test(line)).map(line => { const n = line.indexOf('='); return [line.slice(0, n), line.slice(n + 1).trim()]; }));
const account = process.env.CLOUDFLARE_ACCOUNT_ID || vars.CLOUDFLARE_ACCOUNT_ID;
const token = process.env.CLOUDFLARE_API_TOKEN || vars.CLOUDFLARE_API_TOKEN;
const key = process.env.NVIDIA_API_KEY || vars.NVIDIA_API_KEY;
if (!account || !token) throw new Error('Missing Cloudflare deployment credentials.');
async function api(path, init = {}) {
  const response = await fetch(`https://api.cloudflare.com/client/v4/accounts/${account}/${path}`, {
    ...init, signal: AbortSignal.timeout(30000), headers: { Authorization: `Bearer ${token}`, ...init.headers }
  });
  const body = await response.json();
  if (!response.ok || !body.success) throw new Error(`Cloudflare ${response.status}: ${JSON.stringify(body.errors)}`);
  return body.result;
}
try {
  if (process.argv.includes('--cloudflare-check')) {
    const output = await api('ai/run/@cf/meta/llama-3.1-8b-instruct', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ messages: [{ role: 'system', content: 'Return JSON only: {"tool":"recommend","query":"comedy","type":"movie"}' }, { role: 'user', content: 'Funny movie suggestions please' }], max_tokens: 256 }) });
    console.log(JSON.stringify(output));
    process.exit(0);
  }
  if (process.argv.includes('--models')) {
    const response = await fetch('https://integrate.api.nvidia.com/v1/models', { headers: { Authorization: `Bearer ${key}` }, signal: AbortSignal.timeout(30000) });
    const value = await response.json();
    console.log(JSON.stringify({ status: response.status, models: value.data?.map(x => x.id).filter(x => /llama|nemotron|qwen/i.test(x)) }));
    process.exit(response.ok ? 0 : 1);
  }
  if (process.argv.includes('--provider-check')) {
    const result = await fetch('https://integrate.api.nvidia.com/v1/chat/completions', {
      method: 'POST', signal: AbortSignal.timeout(30000), headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ model: process.argv.find(x => x.startsWith('--model='))?.slice(8) || 'nvidia/nemotron-3.5-lightning-30b-a3b', max_tokens: 100, messages: [{ role: 'user', content: 'Say hello' }] })
    });
    const value = await result.json();
    console.log(JSON.stringify({ providerStatus: result.status, detail: value.detail || value.error?.message || value.choices?.[0]?.message?.content }));
    process.exit(result.ok ? 0 : 1);
  }
  await api('workers/scripts');
  console.log('Workers API permission verified.');
  if (!process.argv.includes('--check')) {
    const form = new FormData();
    form.set('metadata', new Blob([JSON.stringify({ main_module: 'worker.mjs', compatibility_date: '2026-10-01', bindings: [
      { type: 'plain_text', name: 'FIREBASE_PROJECT_ID', text: 'vista-cinema' },
      { type: 'plain_text', name: 'AI_MODEL', text: '@cf/meta/llama-3.3-70b-instruct-fp8-fast' },
      { type: 'ai', name: 'AI' },
      { type: 'ratelimit', name: 'ASSISTANT_LIMITER', namespace_id: '1009', simple: { limit: 15, period: 60 } }
    ] })], { type: 'application/json' }));
    form.set('worker.mjs', new Blob([await readFile(new URL('src/worker.mjs', import.meta.url))], { type: 'application/javascript+module' }), 'worker.mjs');
    await api('workers/scripts/vista-assistant', { method: 'PUT', body: form });
    await api('workers/scripts/vista-assistant/subdomain', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ enabled: true }) });
    const subdomain = await api('workers/subdomain');
    console.log(`Deployed: https://vista-assistant.${subdomain.subdomain}.workers.dev`);
  }
} catch (error) { console.error(error.message); process.exitCode = 1; }
