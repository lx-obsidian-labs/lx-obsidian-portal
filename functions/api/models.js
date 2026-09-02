const MODELS = {
  nvidia: {
    name: 'NVIDIA NIM',
    models: [
      { id: 'nvidia/nemotron-3-ultra-550b-a55b', name: 'Nemotron 3 Ultra 550B', desc: 'Largest, most capable', tier: 'free' },
      { id: 'nvidia/nemotron-3-super-120b-a12b', name: 'Nemotron 3 Super 120B', desc: 'High quality, fast', tier: 'free' },
      { id: 'nvidia/nemotron-3.5-lightning-30b-a3b', name: 'Nemotron Lightning', desc: 'Fast reasoning, 30B params', tier: 'free' },
      { id: 'meta/llama-3.2-90b-vision-instruct', name: 'Llama 3.2 90B Vision', desc: 'Multimodal, largest Llama', tier: 'free' },
      { id: 'meta/llama-3.2-11b-vision-instruct', name: 'Llama 3.2 11B Vision', desc: 'Fast multimodal model', tier: 'free' }
    ]
  },
  openrouter: {
    name: 'OpenRouter',
    models: [
      { id: 'nvidia/nemotron-3-ultra-550b-a55b:free', name: 'Nemotron 3 Ultra 550B', desc: 'Largest free model', tier: 'free' },
      { id: 'nvidia/nemotron-3.5-lightning:free', name: 'Nemotron Lightning', desc: 'NVIDIA fast reasoning', tier: 'free' },
      { id: 'nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free', name: 'Nemotron Nano Omni', desc: 'Reasoning + multimodal', tier: 'free' },
      { id: 'minimax/minimax-m3:free', name: 'MiniMax M3', desc: 'General purpose', tier: 'free' },
      { id: 'poolside/laguna-s-2.1:free', name: 'Laguna S 2.1', desc: 'Poolside code model', tier: 'free' },
      { id: 'openrouter/free', name: 'Auto Router', desc: 'Best available free model', tier: 'free' }
    ]
  }
};

const ALLOWED_ORIGINS = [
  'https://www.lxobsidianportal.co.za',
  'https://lxobsidianportal.co.za',
  'http://localhost:8788',
  'http://localhost:8789'
];

export async function onRequest(context) {
  const { request, env } = context;
  const origin = request?.headers?.get('Origin') || '';
  const allowed = ALLOWED_ORIGINS.includes(origin) ? origin : ALLOWED_ORIGINS[0];

  const result = {};
  for (const [key, provider] of Object.entries(MODELS)) {
    const keyName = key.toUpperCase() + '_API_KEY';
    result[key] = {
      name: provider.name,
      configured: !!(env && env[keyName]),
      models: provider.models
    };
  }

  return new Response(JSON.stringify({ models: result }), {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': allowed
    }
  });
}
