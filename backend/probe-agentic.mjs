// Actual model-contract probe. No Firebase accounts, local app mutations or push messages.
import { loadEnvironment } from './env.mjs';
import { conversationMessages, generate } from './src/worker.mjs';

const env = await loadEnvironment();
if (!env.NVIDIA_API_KEY) throw new Error('Missing NVIDIA server credential.');
const profile = JSON.stringify({ name: 'Contract test viewer', recentResults: [
  { position: 1, title: 'Inception', type: 'movie' },
  { position: 2, title: 'Interstellar', type: 'movie' }
] });
const checks = [
  { message: 'Download the second one', verify: result => result.tool === 'download' && (result.selection === 2 || result.query.toLowerCase() === 'interstellar') },
  { message: 'Open AI and voice settings', verify: result => result.tool === 'navigate' && result.query === 'ai_voice' },
  { message: 'Show my watchlist', verify: result => result.tool === 'watchlist' }
];
for (const check of checks) {
  const result = await generate(env, conversationMessages({ agentic: true, message: check.message, profile }), false, true);
  if (result.provider !== 'nvidia' || !check.verify(result)) throw new Error(`Agent contract failed: ${JSON.stringify(result)}`);
  console.log(JSON.stringify({ verified: true, prompt: check.message, provider: result.provider, action: result.tool,
    ...(result.selection != null ? { selection: result.selection } : {}), ...(result.tool === 'navigate' ? { destination: result.query } : {}) }));
}
