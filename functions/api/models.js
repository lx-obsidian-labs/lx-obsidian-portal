const MODELS = {
  nvidia: {
    name: 'NVIDIA NIM',
    tier: 'free',
    models: [
      { id: 'nvidia/nemotron-3-ultra-550b-a55b', name: 'Nemotron 3 Ultra 550B', desc: 'Largest, most capable', tier: 'free', context: 128000, capabilities: ['reasoning', 'code', 'general'], recommended: false },
      { id: 'nvidia/nemotron-3-super-120b-a12b', name: 'Nemotron 3 Super 120B', desc: 'High quality, fast', tier: 'free', context: 128000, capabilities: ['code', 'general'], recommended: true },
      { id: 'nvidia/nemotron-3.5-lightning-30b-a3b', name: 'Nemotron Lightning', desc: 'Fast reasoning, 30B params', tier: 'free', context: 128000, capabilities: ['reasoning', 'general'], recommended: false },
      { id: 'meta/llama-3.2-90b-vision-instruct', name: 'Llama 3.2 90B Vision', desc: 'Multimodal, largest Llama', tier: 'free', context: 128000, capabilities: ['vision', 'general'], recommended: false },
      { id: 'meta/llama-3.2-11b-vision-instruct', name: 'Llama 3.2 11B Vision', desc: 'Fast multimodal model', tier: 'free', context: 128000, capabilities: ['vision', 'fast'], recommended: false }
    ]
  },
  openrouter: {
    name: 'OpenRouter',
    tier: 'free',
    models: [
      { id: 'nvidia/nemotron-3-ultra-550b-a55b:free', name: 'Nemotron 3 Ultra 550B', desc: 'Largest free model', tier: 'free', context: 128000, capabilities: ['reasoning', 'code', 'general'], recommended: false },
      { id: 'nvidia/nemotron-3.5-lightning:free', name: 'Nemotron Lightning', desc: 'NVIDIA fast reasoning', tier: 'free', context: 128000, capabilities: ['reasoning', 'fast'], recommended: true },
      { id: 'nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free', name: 'Nemotron Nano Omni', desc: 'Reasoning + multimodal', tier: 'free', context: 128000, capabilities: ['reasoning', 'vision'], recommended: false },
      { id: 'minimax/minimax-m3:free', name: 'MiniMax M3', desc: 'General purpose', tier: 'free', context: 200000, capabilities: ['general'], recommended: false },
      { id: 'poolside/laguna-s-2.1:free', name: 'Laguna S 2.1', desc: 'Poolside code model', tier: 'free', context: 128000, capabilities: ['code'], recommended: false },
      { id: 'openrouter/free', name: 'Auto Router', desc: 'Best available free model', tier: 'free', context: 128000, capabilities: ['auto', 'general'], recommended: true },
      { id: 'meta-llama/llama-3.3-70b-instruct', name: 'Llama 3.3 70B Instruct', desc: 'High-capability Llama, Pro tier', tier: 'pro', context: 131072, capabilities: ['general', 'code'], recommended: true },
      { id: 'nvidia/nemotron-3-ultra-550b-a55b', name: 'Nemotron 3 Ultra 550B', desc: 'Largest Nemotron, Pro tier', tier: 'pro', context: 128000, capabilities: ['reasoning', 'code', 'general'], recommended: false },
      { id: 'openrouter/auto', name: 'Auto Router Pro', desc: 'Best available model — premium routing', tier: 'enterprise', context: 200000, capabilities: ['auto', 'reasoning', 'general'], recommended: true }
    ]
  }
};

const TIERS = {
  free: 'Free NVIDIA + selected OpenRouter :free models. 100 credits / month.',
  pro: 'All free models plus premium OpenRouter models (Llama 3.3 70B, Nemotron 3 Ultra). 5,000 credits / month.',
  enterprise: 'AI Auto Router — best available model per request, premium routing and higher limits. Custom quota.'
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
      tier: provider.tier,
      configured: !!(env && env[keyName]),
      models: provider.models
    };
  }

  return new Response(JSON.stringify({ models: result, tiers: TIERS }), {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': allowed
    }
  });
}