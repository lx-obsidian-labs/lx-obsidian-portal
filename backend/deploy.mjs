// Deployment credentials stay in the local .env; only the NVIDIA key becomes a Worker secret.
import { fileURLToPath } from 'node:url';
import { loadEnvironment } from './env.mjs';
import { parsePlan } from './src/worker.mjs';
import { build } from 'esbuild';
const vars = await loadEnvironment();
const account = vars.CLOUDFLARE_ACCOUNT_ID;
const token = vars.CLOUDFLARE_API_TOKEN;
const key = vars.NVIDIA_API_KEY || vars.NVIDIA_NIM_API_KEY;
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
      body: JSON.stringify({ model: process.argv.find(x => x.startsWith('--model='))?.slice(8) || vars.NVIDIA_MODEL || 'nvidia/nemotron-3.5-lightning-30b-a3b',
        chat_template_kwargs: { enable_thinking: false }, temperature: 0, max_tokens: 200,
        response_format: { type: 'json_object' },
        messages: [{ role: 'system', content: 'Return JSON only with tool, query, type. For a request to open Data & Storage use {"tool":"navigate","query":"data_storage","type":"all"}.' }, { role: 'user', content: 'Open Data & Storage settings' }] })
    });
    const value = await result.json();
    const plan = result.ok ? parsePlan(value.choices?.[0]?.message?.content) : undefined;
    if (result.ok && (plan.tool !== 'navigate' || plan.query !== 'data_storage')) throw new Error('Provider did not pass the app action contract.');
    console.log(JSON.stringify({ providerStatus: result.status, contractVerified: Boolean(plan), detail: value.detail || value.error?.message || plan }));
    process.exit(result.ok ? 0 : 1);
  }
  await api('workers/scripts');
  console.log('Workers API permission verified.');
  if (!process.argv.includes('--check')) {
    if (!key) throw new Error('Missing NVIDIA_API_KEY in the project .env or environment.');
    const form = new FormData();
    form.set('metadata', new Blob([JSON.stringify({ main_module: 'worker.mjs', compatibility_date: '2026-10-01', bindings: [
      { type: 'plain_text', name: 'FIREBASE_PROJECT_ID', text: 'vista-cinema' },
      { type: 'plain_text', name: 'AI_MODEL', text: '@cf/meta/llama-3.3-70b-instruct-fp8-fast' },
      { type: 'plain_text', name: 'NVIDIA_MODEL', text: vars.NVIDIA_MODEL || 'nvidia/nemotron-3.5-lightning-30b-a3b' },
      { type: 'secret_text', name: 'NVIDIA_API_KEY', text: key },
      ...(vars.NEWSDATA_API_KEY ? [{ type: 'secret_text', name: 'NEWSDATA_API_KEY', text: vars.NEWSDATA_API_KEY }] : []),
      ...(vars.ELEVENLABS_API_KEY ? [{ type: 'secret_text', name: 'ELEVENLABS_API_KEY', text: vars.ELEVENLABS_API_KEY }] : []),
      { type: 'ratelimit', name: 'VOICE_LIMITER', namespace_id: '1010', simple: { limit: 6, period: 60 } },
      { type: 'ai', name: 'AI' },
      { type: 'ratelimit', name: 'ASSISTANT_LIMITER', namespace_id: '1009', simple: { limit: 15, period: 60 } }
    ] })], { type: 'application/json' }));
    const bundle = await build({ entryPoints: [fileURLToPath(new URL('src/worker.mjs', import.meta.url))],
      bundle: true, write: false, format: 'esm', platform: 'browser', target: 'es2022', minify: true });
    form.set('worker.mjs', new Blob([bundle.outputFiles[0].contents], { type: 'application/javascript+module' }), 'worker.mjs');
    await api('workers/scripts/vista-assistant', { method: 'PUT', body: form });
    await api('workers/scripts/vista-assistant/subdomain', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ enabled: true }) });
    const subdomain = await api('workers/subdomain');
    console.log(`Deployed: https://vista-assistant.${subdomain.subdomain}.workers.dev`);
  }
} catch (error) { console.error(error.message); process.exitCode = 1; }
