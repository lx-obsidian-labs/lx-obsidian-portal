const SYSTEM_PROMPT = `You are LX Assistant, an advanced AI coding assistant built by LX Obsidian Labs. You help users build, debug, and ship web applications.

CAPABILITIES:
- Generate complete, production-ready HTML/CSS/JS code
- Debug and fix code issues
- Explain code clearly
- Create full web apps from descriptions
- Search the web for documentation and references

RESPONSE FORMAT:
- When generating code, wrap in \`\`\`html, \`\`\`css, or \`\`\`javascript blocks
- For complete web apps, generate a single self-contained HTML file
- Keep responses focused and actionable
- Use modern best practices (CSS Grid, Flexbox, ES6+)
- Always produce working, runnable code
- If the user shares code, reference it by line when suggesting changes

PERSONALITY:
- Concise and professional
- Show don't tell — prefer code over explanation
- Think step by step before complex solutions`;

const PROVIDERS = {
  nvidia: {
    hostname: 'integrate.api.nvidia.com',
    path: '/v1/chat/completions',
    getKey: (env) => env.NVIDIA_API_KEY,
    extraHeaders: {},
    supportsStreaming: true
  },
  openrouter: {
    hostname: 'openrouter.ai',
    path: '/api/v1/chat/completions',
    getKey: (env) => env.OPENROUTER_API_KEY,
    extraHeaders: {
      'HTTP-Referer': 'https://www.lxobsidianportal.co.za',
      'X-Title': 'LX Obsidian Labs'
    },
    supportsStreaming: true
  }
};

const DEFAULT_MODELS = {
  nvidia: 'nvidia/nemotron-3.5-lightning-30b-a3b',
  openrouter: 'google/gemma-4-31b-it:free'
};

function corsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type'
  };
}

export async function onRequest(context) {
  const { request, env } = context;

  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: corsHeaders() });
  }

  if (request.method !== 'POST') {
    return new Response(JSON.stringify({ error: 'Method not allowed' }), {
      status: 405,
      headers: { 'Content-Type': 'application/json', ...corsHeaders() }
    });
  }

  try {
    const body = await request.json();
    const { message, provider = 'nvidia', model, images, stream = true, history = [] } = body;

    if (!message || typeof message !== 'string') {
      return new Response(JSON.stringify({ error: 'Message required' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json', ...corsHeaders() }
      });
    }

    const config = PROVIDERS[provider];
    if (!config) {
      return new Response(JSON.stringify({ error: 'Unknown provider' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json', ...corsHeaders() }
      });
    }

    const apiKey = config.getKey(env);
    if (!apiKey) {
      return new Response(JSON.stringify({ error: `${provider.toUpperCase()} API key not configured` }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', ...corsHeaders() }
      });
    }

    const resolvedModel = model || DEFAULT_MODELS[provider] || DEFAULT_MODELS.nvidia;

    // Build messages array with history
    const messages = [{ role: 'system', content: SYSTEM_PROMPT }];
    for (const h of history.slice(-30)) {
      if (h.role === 'user' || h.role === 'assistant') {
        messages.push({ role: h.role, content: h.content });
      }
    }
    messages.push({ role: 'user', content: message });

    const headers = {
      'Authorization': 'Bearer ' + apiKey,
      'Content-Type': 'application/json'
    };
    if (config.extraHeaders) Object.assign(headers, config.extraHeaders);

    // Stream directly from the AI API
    const requestBody = {
      model: resolvedModel,
      messages,
      max_tokens: 4096,
      temperature: 0.7,
      top_p: 0.9,
      stream: true
    };

    const apiResponse = await fetch('https://' + config.hostname + config.path, {
      method: 'POST',
      headers,
      body: JSON.stringify(requestBody)
    });

    if (!apiResponse.ok) {
      const errText = await apiResponse.text();
      let detail = '';
      try {
        const errJson = JSON.parse(errText);
        detail = errJson.error?.message || errText.substring(0, 300);
      } catch {
        detail = errText.substring(0, 300);
      }
      return new Response(JSON.stringify({
        error: `Provider error ${apiResponse.status}`,
        detail
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', ...corsHeaders() }
      });
    }

    // Real-time passthrough streaming
    const { readable, writable } = new TransformStream();
    const writer = writable.getWriter();
    const encoder = new TextEncoder();

    (async () => {
      const reader = apiResponse.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop();

          for (const line of lines) {
            if (!line.startsWith('data: ')) continue;
            const data = line.slice(6).trim();
            if (data === '[DONE]') {
              await writer.write(encoder.encode('data: [DONE]\n\n'));
              continue;
            }
            try {
              const parsed = JSON.parse(data);
              const delta = parsed.choices?.[0]?.delta;
              if (delta?.content) {
                await writer.write(encoder.encode(`data: ${JSON.stringify({ content: delta.content })}\n\n`));
              }
            } catch {}
          }
        }
      } catch (e) {
        await writer.write(encoder.encode(`data: ${JSON.stringify({ error: 'Stream interrupted' })}\n\n`));
      } finally {
        await writer.close();
      }
    })();

    return new Response(readable, {
      status: 200,
      headers: {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        ...corsHeaders()
      }
    });

  } catch (error) {
    return new Response(JSON.stringify({ error: 'Connection failed: ' + error.message }), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders() }
    });
  }
}
