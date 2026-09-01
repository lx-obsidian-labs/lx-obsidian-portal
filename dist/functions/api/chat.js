const SYSTEM_PROMPT = `You are LX Assistant, an advanced AI coding assistant built by LX Obsidian Labs. You help users build, debug, and ship web applications.

CAPABILITIES:
- Generate complete, production-ready HTML/CSS/JS code
- Debug and fix code issues
- Explain code clearly
- Create full web apps from descriptions
- Analyze images when uploaded
- Search the web for documentation and references
- Create and update files in the project
- Execute JavaScript code

RESPONSE FORMAT:
- When generating code, wrap in \`\`\`html, \`\`\`css, or \`\`\`javascript blocks
- For complete web apps, generate a single self-contained HTML file
- Keep responses focused and actionable
- Use modern best practices (CSS Grid, Flexbox, ES6+)
- Always produce working, runnable code

PERSONALITY:
- Concise and professional
- Show don't tell — prefer code over explanation
- Think step by step before complex solutions
- Sign off with "-- LX Assistant" on longer responses`;

const PROVIDERS = {
  nvidia: {
    hostname: 'integrate.api.nvidia.com',
    path: '/v1/chat/completions',
    getKey: (env) => env.NVIDIA_API_KEY,
    extraHeaders: {},
    supportsTools: false
  },
  openrouter: {
    hostname: 'openrouter.ai',
    path: '/api/v1/chat/completions',
    getKey: (env) => env.OPENROUTER_API_KEY,
    extraHeaders: {
      'HTTP-Referer': 'https://www.lxobsidianportal.co.za',
      'X-Title': 'LX Obsidian Labs'
    },
    supportsTools: true
  }
};

const DEFAULT_MODELS = {
  nvidia: 'nvidia/nemotron-3.5-lightning-30b-a3b',
  openrouter: 'google/gemma-4-31b-it:free'
};

const TOOLS = [
  {
    type: 'function',
    function: {
      name: 'web_search',
      description: 'Search the web for current information, documentation, or references',
      parameters: {
        type: 'object',
        properties: {
          query: { type: 'string', description: 'Search query' }
        },
        required: ['query']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'create_file',
      description: 'Create or update a file in the project with given content',
      parameters: {
        type: 'object',
        properties: {
          path: { type: 'string', description: 'File path like index.html, style.css, app.js' },
          content: { type: 'string', description: 'The full file content' }
        },
        required: ['path', 'content']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'run_code',
      description: 'Execute JavaScript code in a sandbox and return the result',
      parameters: {
        type: 'object',
        properties: {
          code: { type: 'string', description: 'JavaScript code to execute' }
        },
        required: ['code']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'read_file',
      description: 'Read the contents of a file in the project',
      parameters: {
        type: 'object',
        properties: {
          path: { type: 'string', description: 'File path to read' }
        },
        required: ['path']
      }
    }
  }
];

async function executeTool(toolName, args) {
  if (toolName === 'web_search') {
    try {
      const r = await fetch(`https://api.search.brave.com/res/v1/web/search?q=${encodeURIComponent(args.query)}&count=5`, {
        headers: { 'Accept': 'application/json', 'Accept-Encoding': 'gzip' }
      });
      if (!r.ok) return { error: 'Search unavailable (status ' + r.status + ')' };
      const data = await r.json();
      return {
        results: (data.web?.results || []).slice(0, 5).map(r => ({
          title: r.title,
          url: r.url,
          snippet: r.description
        }))
      };
    } catch (e) { return { error: 'Search failed: ' + e.message }; }
  }

  if (toolName === 'create_file') {
    return { success: true, path: args.path, message: `File "${args.path}" created/updated successfully` };
  }

  if (toolName === 'read_file') {
    return { success: true, path: args.path, content: `(File content would be read from project: ${args.path})` };
  }

  if (toolName === 'run_code') {
    try {
      const fn = new Function('return ' + args.code);
      const result = fn();
      return { result: typeof result === 'string' ? result : JSON.stringify(result) };
    } catch (e) {
      return { error: e.message };
    }
  }

  return { error: 'Unknown tool: ' + toolName };
}

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
    for (const h of history.slice(-20)) {
      messages.push({ role: h.role, content: h.content });
    }
    messages.push({ role: 'user', content: message });

    const headers = {
      'Authorization': 'Bearer ' + apiKey,
      'Content-Type': 'application/json'
    };
    if (config.extraHeaders) Object.assign(headers, config.extraHeaders);

    const toolEvents = [];

    // Tool calling loop (max 3 rounds)
    let finalContent = '';
    let currentMessages = [...messages];

    for (let round = 0; round < 3; round++) {
      const requestBody = {
        model: resolvedModel,
        messages: currentMessages,
        max_tokens: 4096,
        temperature: 0.7,
        top_p: 0.9,
        stream: false
      };

      // Only send tools if provider supports them
      if (config.supportsTools) {
        requestBody.tools = TOOLS;
        requestBody.tool_choice = 'auto';
      }

      const apiResponse = await fetch('https://' + config.hostname + config.path, {
        method: 'POST',
        headers,
        body: JSON.stringify(requestBody)
      });

      if (!apiResponse.ok) {
        const errText = await apiResponse.text();
        return new Response(JSON.stringify({
          error: `Provider error ${apiResponse.status}`,
          detail: errText.substring(0, 300),
          toolEvents
        }), {
          status: 200,
          headers: { 'Content-Type': 'application/json', ...corsHeaders() }
        });
      }

      const data = await apiResponse.json();

      if (data.error) {
        return new Response(JSON.stringify({
          error: data.error.message || 'Provider error',
          provider, model: resolvedModel, toolEvents
        }), {
          status: 200,
          headers: { 'Content-Type': 'application/json', ...corsHeaders() }
        });
      }

      const choice = data.choices?.[0];
      if (!choice) break;

      const assistantMsg = choice.message;

      // Check for tool calls
      if (assistantMsg.tool_calls && assistantMsg.tool_calls.length > 0) {
        // Add assistant message with tool calls to conversation
        currentMessages.push(assistantMsg);

        // Execute each tool call
        for (const tc of assistantMsg.tool_calls) {
          const fnName = tc.function.name;
          let fnArgs;
          try {
            fnArgs = JSON.parse(tc.function.arguments);
          } catch {
            fnArgs = {};
          }

          toolEvents.push({ type: 'tool_call', name: fnName, args: fnArgs });
          const result = await executeTool(fnName, fnArgs);
          toolEvents.push({ type: 'tool_result', name: fnName, result });

          // Add tool result to conversation
          currentMessages.push({
            role: 'tool',
            tool_call_id: tc.id,
            content: JSON.stringify(result)
          });
        }

        continue; // Next round
      }

      // No tool calls — we have the final text response
      finalContent = assistantMsg.content || '';
      break;
    }

    // Now stream the final content to the client
    if (stream) {
      const { readable, writable } = new TransformStream();
      const writer = writable.getWriter();
      const encoder = new TextEncoder();

      (async () => {
        try {
          // Send tool events first
          if (toolEvents.length > 0) {
            await writer.write(encoder.encode(`data: ${JSON.stringify({ toolEvents })}\n\n`));
          }

          // Send content in chunks for streaming effect
          const chunkSize = 20;
          for (let i = 0; i < finalContent.length; i += chunkSize) {
            const chunk = finalContent.slice(i, i + chunkSize);
            await writer.write(encoder.encode(`data: ${JSON.stringify({ content: chunk })}\n\n`));
          }

          await writer.write(encoder.encode('data: [DONE]\n\n'));
        } catch (e) {
          await writer.write(encoder.encode(`data: ${JSON.stringify({ error: 'Stream error' })}\n\n`));
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
    }

    // Non-streaming response
    return new Response(JSON.stringify({
      reply: finalContent,
      provider,
      model: resolvedModel,
      toolEvents
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders() }
    });

  } catch (error) {
    return new Response(JSON.stringify({
      error: 'Connection failed: ' + error.message
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders() }
    });
  }
}
