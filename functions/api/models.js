const MODELS = {
  nvidia: {
    name: 'NVIDIA NIM',
    models: [
      { id: 'nvidia/nemotron-3.5-lightning-30b-a3b', name: 'Nemotron 3.5 Lightning', desc: 'Fast reasoning, 30B params', tier: 'free' },
      { id: 'nvidia/nemotron-3-nano-omni-30b-a3b-reasoning', name: 'Nemotron 3 Nano Omni', desc: 'Reasoning specialist', tier: 'free' },
      { id: 'meta/llama-3.2-90b-vision-instruct', name: 'Llama 3.2 90B Vision', desc: 'Multimodal, largest Llama', tier: 'free' },
      { id: 'meta/llama-3.2-11b-vision-instruct', name: 'Llama 3.2 11B Vision', desc: 'Fast multimodal model', tier: 'free' }
    ]
  },
  openrouter: {
    name: 'OpenRouter',
    models: [
      { id: 'google/gemma-4-31b-it:free', name: 'Gemma 4 31B', desc: 'Google latest, best free model', tier: 'free' },
      { id: 'nvidia/nemotron-3.5-lightning:free', name: 'Nemotron Lightning', desc: 'NVIDIA fast reasoning', tier: 'free' },
      { id: 'nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free', name: 'Nemotron Nano Omni', desc: 'Reasoning + multimodal', tier: 'free' },
      { id: 'poolside/laguna-s-2.1:free', name: 'Laguna S 2.1', desc: 'Poolside code model', tier: 'free' },
      { id: 'minimax/minimax-m3:free', name: 'MiniMax M3', desc: 'General purpose', tier: 'free' },
      { id: 'openrouter/free', name: 'Auto Router', desc: 'Best available free model', tier: 'free' }
    ]
  }
};

export async function onRequest(context) {
  const { env } = context;

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
      'Access-Control-Allow-Origin': '*'
    }
  });
}
