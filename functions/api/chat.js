// ── System prompt (adapted from bolt.diy architecture) ──────────
// Uses <lxArtifact> + <lxAction> structured output format
// so the LLM output is cleanly parseable instead of raw HTML dumps.

const SYSTEM_PROMPT = `You are LX AI, an expert web developer. You create production-ready, visually stunning single-file HTML applications.

<system_constraints>
  You generate COMPLETE single-file HTML pages. Each file must be self-contained with:
  - All CSS in a <style> tag
  - All JS in a <script> tag
  - All HTML structure
  - External resources only via CDN (Google Fonts, CDN libraries)

  NEVER output partial code. NEVER use placeholders like "// rest of code...".
  NEVER output thinking or reasoning text outside the artifact tags.
  NEVER output syntax highlighting tokens like <span class="kw"> or class="str">.
</system_constraints>

<code_formatting_info>
  Use 2 spaces for code indentation.
  Output COMPLETE files only — no diffs, no partial updates.
</code_formatting_info>

<artifact_info>
  You create artifacts using the following format. ALWAYS wrap your response in these tags:

  <lxArtifact id="unique-id" title="Project Title">
    <lxAction type="file" filePath="index.html">
      COMPLETE file content here
    </lxAction>
  </lxArtifact>

  Rules:
  1. Think HOLISTICALLY before creating an artifact. Consider the full design.
  2. ALWAYS use the <lxArtifact> and <lxAction> tags. This is non-negotiable.
  3. Each <lxAction type="file"> must contain the COMPLETE file content.
  4. The filePath should be "index.html" for the main page.
  5. You can include multiple <lxAction> tags for multiple files (e.g., style.css, app.js).
  6. The id attribute must be descriptive and unique (e.g., "portfolio-site", "todo-app").
  7. NEVER truncate or abbreviate file contents.
  8. NEVER output anything outside the <lxArtifact> tags except a brief 1-2 sentence intro.

  Example:
  <lxArtifact id="snake-game" title="Snake Game">
    <lxAction type="file" filePath="index.html">
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Snake Game</title>
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body { background: #0a0a0f; color: #e4e4e7; font-family: 'Inter', sans-serif; }
          /* ... complete styles ... */
        </style>
      </head>
      <body>
        <!-- ... complete HTML ... -->
        <script>
          // ... complete JavaScript ...
        </script>
      </body>
      </html>
    </lxAction>
  </lxArtifact>
</artifact_info>

<design_instructions>
  Create visually stunning, premium designs:
  - Use modern CSS (Grid, Flexbox, custom properties, gradients, glassmorphism)
  - Smooth animations and transitions (hover states, scroll effects, micro-interactions)
  - Responsive design (mobile-first, breakpoints at 768px and 1024px)
  - Dark theme by default unless requested otherwise
  - Professional typography with clear hierarchy
  - Generous whitespace and balanced layouts
  - High-quality visual polish — avoid generic templates
  - Use CSS variables for theming
  - Include loading states and empty states where appropriate
</design_instructions>

<quality_requirements>
  - ALL interactive elements must have hover/focus states
  - Forms must have validation
  - Navigation must work (smooth scroll, mobile menu toggle)
  - Images use alt text
  - Semantic HTML (header, nav, main, section, footer)
  - ARIA labels on interactive elements
  - Minimum 600 lines of well-structured code for complex pages
</quality_requirements>

NEVER say "This artifact sets up..." — just create it.
NEVER explain your code unless the user asks. Just output the artifact.
ULTRA IMPORTANT: Do NOT be verbose. Output the artifact immediately.`;

const REFINE_PROMPT = `You are a code editor. The user will give you existing code and a change request.

Output the COMPLETE updated code inside an <lxArtifact> tag:

<lxArtifact id="refined-code" title="Refined Code">
  <lxAction type="file" filePath="index.html">
    COMPLETE updated file content
  </lxAction>
</lxArtifact>

Rules:
- Keep all working code — only change what was requested
- Output the COMPLETE file, never partial
- Do NOT explain unless asked
- Do NOT output anything outside the artifact tags`;

const EXPLAIN_PROMPT = `You are a code teacher. Explain the provided code clearly and concisely.
Focus on what it does, how it works, and key patterns used.
Use plain language. Structure your response with headings if helpful.
Do NOT output code fences or artifacts — just explain.`;

const CHAT_PROMPT = `You are LX AI, a helpful assistant for web developers.
You can help with:
- Planning web app architecture
- Explaining code concepts
- Debugging issues
- Suggesting improvements
- Answering web dev questions

Be concise and helpful. Show reasoning before code when applicable.
If the user asks you to build something, use the <lxArtifact> format.`;

// ── Provider config ─────────────────────────────────────────────
const PROVIDERS = {
  nvidia: {
    hostname: 'integrate.api.nvidia.com',
    path: '/v1/chat/completions',
    getKey: (env) => env.NVIDIA_API_KEY,
    extraHeaders: {},
    models: [
      'nvidia/nemotron-3.5-lightning-30b-a3b',
      'nvidia/nemotron-3-super-120b-a12b',
      'nvidia/nemotron-3-ultra-550b-a55b'
    ]
  },
  openrouter: {
    hostname: 'openrouter.ai',
    path: '/api/v1/chat/completions',
    getKey: (env) => env.OPENROUTER_API_KEY,
    extraHeaders: {
      'HTTP-Referer': 'https://www.lxobsidianportal.co.za',
      'X-Title': 'LX Obsidian Labs'
    },
    models: [
      'nvidia/nemotron-3.5-lightning:free',
      'nvidia/nemotron-3-ultra-550b-a55b:free',
      'poolside/laguna-s-2.1:free'
    ]
  }
};

// ── Cleanup (strips any stray syntax tokens the model might leak) ─
function clean(text) {
  return text
    .replace(/<span\s+[^>]*>/gi, '')
    .replace(/<\/span>/gi, '')
    .replace(/<\/?code[^>]*>/gi, '')
    .replace(/<\/?pre[^>]*>/gi, '')
    .replace(/class=class="[^"]*">/g, '')
    .replace(/class="kw">/g, '')
    .replace(/class="str">/g, '')
    .replace(/class="num">/g, '')
    .replace(/class="cm">/g, '')
    .replace(/class="tag">/g, '')
    .replace(/class="fn">/g, '')
    .replace(/class="op">/g, '')
    .replace(/<\/?span[^>]*>/gi, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"');
}

// ── Repair truncated artifact (auto-close unclosed tags) ────────
function repairTruncatedArtifact(text) {
  let fixed = text;

  // If there's an <lxAction> without a matching </lxAction>, close it
  const openActions = (fixed.match(/<lxAction\b/gi) || []).length;
  const closeActions = (fixed.match(/<\/lxAction>/gi) || []).length;
  if (openActions > closeActions) {
    // Count how many unclosed tags there are
    for (let i = 0; i < openActions - closeActions; i++) {
      fixed += '\n</lxAction>';
    }
  }

  // If there's an <lxArtifact> without a matching </lxArtifact>, close it
  const openArtifacts = (fixed.match(/<lxArtifact\b/gi) || []).length;
  const closeArtifacts = (fixed.match(/<\/lxArtifact>/gi) || []).length;
  if (openArtifacts > closeArtifacts) {
    for (let i = 0; i < openArtifacts - closeArtifacts; i++) {
      fixed += '\n</lxArtifact>';
    }
  }

  return fixed;
}

// ── Parse <lxArtifact> response from LLM ────────────────────────
function parseArtifact(raw) {
  let cleaned = clean(raw);

  // Auto-close truncated tags before parsing
  cleaned = repairTruncatedArtifact(cleaned);

  // Extract artifact metadata
  const artifactMatch = cleaned.match(/<lxArtifact\s+([^>]*)>/i);
  if (!artifactMatch) return null;

  const attrs = artifactMatch[1];
  const id = (attrs.match(/id="([^"]*)"/i) || [, ''])[1];
  const title = (attrs.match(/title="([^"]*)"/i) || [, ''])[1];

  // Extract all actions (greedy — captures everything up to closing tag)
  const actions = [];
  const actionRegex = /<lxAction\s+([^>]*)>([\s\S]*?)<\/lxAction>/gi;
  let actionMatch;
  while ((actionMatch = actionRegex.exec(cleaned)) !== null) {
    const actionAttrs = actionMatch[1];
    const content = actionMatch[2].trim();
    const type = (actionAttrs.match(/type="([^"]*)"/i) || [, 'file'])[1];
    const filePath = (actionAttrs.match(/filePath="([^"]*)"/i) || [, 'index.html'])[1];
    actions.push({ type, filePath, content });
  }

  // Fallback: if no </lxAction> found, try greedy match to end of string
  if (actions.length === 0) {
    const greedyMatch = cleaned.match(/<lxAction\s+([^>]*)>([\s\S]*)/i);
    if (greedyMatch) {
      const actionAttrs = greedyMatch[1];
      const content = greedyMatch[2]
        .replace(/<\/lxAction>/gi, '')
        .replace(/<\/lxArtifact>/gi, '')
        .trim();
      const type = (actionAttrs.match(/type="([^"]*)"/i) || [, 'file'])[1];
      const filePath = (actionAttrs.match(/filePath="([^"]*)"/i) || [, 'index.html'])[1];
      if (content.length > 50) actions.push({ type, filePath, content });
    }
  }

  // Extract any text before/after the artifact (for the assistant's message)
  const beforeArtifact = cleaned.substring(0, cleaned.indexOf('<lxArtifact')).trim();
  const introText = beforeArtifact || '';

  return { id, title, actions, introText };
}

// ── Template fallback ───────────────────────────────────────────
function generateFromTemplate(spec) {
  const s = typeof spec === 'string' ? JSON.parse(spec) : spec;
  const c = s.colors || { primary: '#7c5cff', secondary: '#34d399', accent: '#f59e0b', bg: '#0a0a0f', text: '#e4e4e7' };

  let sectionsHtml = '';
  for (const sec of (s.sections || [])) {
    switch (sec.type) {
      case 'hero':
        sectionsHtml += `
    <section class="hero">
      <div class="container"><h1>${sec.title || ''}</h1><p>${sec.subtitle || ''}</p>
      ${sec.cta ? `<a href="#" class="btn">${sec.cta}</a>` : ''}</div></section>`;
        break;
      case 'grid':
        sectionsHtml += `
    <section class="section" id="${sec.id}"><div class="container">
      <h2>${sec.title || ''}</h2><div class="grid">${(sec.items || []).map(i => `<div class="card"><h3>${i}</h3></div>`).join('')}</div></div></section>`;
        break;
      case 'cards':
        sectionsHtml += `
    <section class="section" id="${sec.id}"><div class="container">
      <h2>${sec.title || ''}</h2><div class="grid">${(sec.items || []).map(i => `<div class="card"><h3>${i.name || ''}</h3><p class="price">${i.price || ''}</p><p>${i.desc || ''}</p></div>`).join('')}</div></div></section>`;
        break;
      case 'text':
        sectionsHtml += `
    <section class="section alt" id="${sec.id}"><div class="container">
      <h2>${sec.title || ''}</h2><p>${sec.content || ''}</p></div></section>`;
        break;
      case 'form':
        sectionsHtml += `
    <section class="section" id="${sec.id}"><div class="container">
      <h2>${sec.title || ''}</h2><form class="contact-form" onsubmit="event.preventDefault();alert('Sent!')">
      ${(sec.fields || []).map(f => f === 'message' ? `<textarea placeholder="${f}" required></textarea>` : `<input type="${f === 'email' ? 'email' : 'text'}" placeholder="${f}" required>`).join('')}
      <button type="submit" class="btn">Send</button></form></div></section>`;
        break;
      case 'footer':
        sectionsHtml += `
    <footer class="footer"><div class="container">
      <p>&copy; 2025 ${s.title || 'App'}</p><div class="links">${(sec.links || []).map(l => `<a href="#">${l}</a>`).join('')}</div></div></footer>`;
        break;
    }
  }

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>${s.title || 'App'}</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--primary:${c.primary};--secondary:${c.secondary};--accent:${c.accent};--bg:${c.bg};--text:${c.text}}
body{font-family:'Inter',sans-serif;background:var(--bg);color:var(--text);line-height:1.6}
.container{max-width:1200px;margin:0 auto;padding:0 1.5rem}
.hero{min-height:80vh;display:flex;align-items:center;text-align:center}
.hero h1{font-size:clamp(2rem,5vw,4rem);margin-bottom:1rem}
.hero p{font-size:1.2rem;opacity:0.7;margin-bottom:2rem}
.section{padding:5rem 0}
.section.alt{background:rgba(255,255,255,0.03)}
.section h2{font-size:2rem;margin-bottom:2rem;text-align:center}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:1.5rem}
.card{background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.08);border-radius:12px;padding:1.5rem;transition:transform .2s}
.card:hover{transform:translateY(-4px)}
.card h3{margin-bottom:0.5rem}
.price{color:var(--accent);font-weight:700;font-size:1.1rem;margin-bottom:0.5rem}
.btn{display:inline-block;padding:0.8rem 2rem;background:var(--primary);color:#fff;border:none;border-radius:8px;font-size:1rem;cursor:pointer;text-decoration:none;transition:opacity .2s}
.btn:hover{opacity:0.85}
.contact-form{display:flex;flex-direction:column;gap:1rem;max-width:500px;margin:0 auto}
.contact-form input,.contact-form textarea{padding:0.8rem;border:1px solid rgba(255,255,255,0.1);border-radius:8px;background:rgba(255,255,255,0.05);color:var(--text);font-size:1rem}
.contact-form textarea{min-height:120px;resize:vertical}
.footer{padding:2rem 0;text-align:center;opacity:0.5;font-size:0.9rem}
.footer .links{margin-top:0.5rem;display:flex;gap:1rem;justify-content:center}
.footer a{color:var(--text);opacity:0.7}
@media(max-width:768px){.hero{min-height:60vh}.grid{grid-template-columns:1fr}}
</style>
</head>
<body>
${sectionsHtml}
</body>
</html>`;

  // Wrap in artifact format for consistent parsing
  return `<lxArtifact id="template-output" title="${s.title || 'App'}">
<lxAction type="file" filePath="index.html">
${html}
</lxAction>
</lxArtifact>`;
}

// ── API call with fallback ──────────────────────────────────────
async function callAI(messages, env, preferredProvider, preferredModel, maxTokens = 4096) {
  const providerOrder = preferredProvider === 'openrouter'
    ? ['openrouter', 'nvidia']
    : ['nvidia', 'openrouter'];

  for (const provName of providerOrder) {
    const prov = PROVIDERS[provName];
    const apiKey = prov.getKey(env);
    if (!apiKey) continue;

    const modelList = preferredModel && provName === preferredProvider
      ? [preferredModel, ...prov.models]
      : prov.models;

    for (const modelId of modelList) {
      try {
        const resp = await fetch('https://' + prov.hostname + prov.path, {
          method: 'POST',
          headers: {
            'Authorization': 'Bearer ' + apiKey,
            'Content-Type': 'application/json',
            ...prov.extraHeaders
          },
          body: JSON.stringify({
            model: modelId,
            messages,
            max_tokens: maxTokens,
            temperature: 0.4,
            top_p: 0.85,
            stream: false
          })
        });

        if (resp.ok) {
          const ct = resp.headers.get('content-type') || '';
          if (ct.includes('application/json')) {
            const data = await resp.json();
            const content = data.choices?.[0]?.message?.content || '';
            if (content.trim()) return { content: clean(content), provider: provName, model: modelId };
          }
        }
      } catch {}
    }
  }

  return null;
}

// ── CORS ────────────────────────────────────────────────────────
const ALLOWED_ORIGINS = [
  'https://www.lxobsidianportal.co.za',
  'https://lxobsidianportal.co.za',
  'http://localhost:8788',
  'http://localhost:8789'
];

function cors(request) {
  const origin = request?.headers?.get('Origin') || '';
  const allowed = ALLOWED_ORIGINS.includes(origin) ? origin : ALLOWED_ORIGINS[0];
  return {
    'Access-Control-Allow-Origin': allowed,
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization'
  };
}

// ── Handler ─────────────────────────────────────────────────────
export async function onRequest(context) {
  const { request, env } = context;
  if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors(request) });
  if (request.method !== 'POST') return new Response(JSON.stringify({ error: 'POST only' }), { status: 405, headers: { 'Content-Type': 'application/json', ...cors(request) } });

  try {
    const body = await request.json();
    const { message, mode = 'chat', provider = 'nvidia', model, code, history = [] } = body;

    if (!message && !code) return new Response(JSON.stringify({ error: 'Message required' }), { status: 400, headers: { 'Content-Type': 'application/json', ...cors(request) } });

    // ── Build messages based on mode ──
    let messages;
    let maxTokens;

    switch (mode) {
      case 'generate': {
        // Single-phase generation: plan + code in one shot
        messages = [
          { role: 'system', content: SYSTEM_PROMPT },
          ...history.slice(-8).map(m => ({ role: m.role, content: m.content.substring(0, 2000) })),
          { role: 'user', content: `Build this web application:\n${message}` }
        ];
        maxTokens = 8192;
        break;
      }

      case 'refine': {
        messages = [
          { role: 'system', content: REFINE_PROMPT },
          { role: 'user', content: `Current code:\n${code || ''}\n\nChange requested: ${message}` }
        ];
        maxTokens = 8192;
        break;
      }

      case 'explain': {
        messages = [
          { role: 'system', content: EXPLAIN_PROMPT },
          { role: 'user', content: code || message }
        ];
        maxTokens = 2048;
        break;
      }

      default: {
        // General chat — may include code generation if user asks
        messages = [
          { role: 'system', content: CHAT_PROMPT },
          ...history.slice(-16).map(m => ({ role: m.role, content: m.content.substring(0, 3000) })),
          { role: 'user', content: message }
        ];
        maxTokens = 4096;
        break;
      }
    }

    // ── Try AI providers ──
    const result = await callAI(messages, env, provider, model, maxTokens);

    if (result) {
      // Parse the artifact from the response
      const artifact = parseArtifact(result.content);

      return new Response(JSON.stringify({
        reply: result.content,
        artifact: artifact,   // Parsed structured data
        provider: result.provider,
        model: result.model
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', ...cors(request) }
      });
    }

    // ── Template fallback for generate mode ──
    if (mode === 'generate') {
      try {
        // Try to parse the user message as a JSON spec
        const spec = JSON.parse(message);
        const html = generateFromTemplate(spec);
        const artifact = parseArtifact(html);
        return new Response(JSON.stringify({ reply: html, artifact, provider: 'template', model: 'fallback' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json', ...cors(request) }
        });
      } catch {
        // Not JSON — generate from description
        const spec = {
          title: message.substring(0, 50),
          sections: [
            { id: 'hero', type: 'hero', title: message.substring(0, 60), subtitle: 'Built with LX AI', cta: 'Get Started' },
            { id: 'about', type: 'text', title: 'About', content: message },
            { id: 'footer', type: 'footer', links: ['About', 'Contact'] }
          ]
        };
        const html = generateFromTemplate(spec);
        const artifact = parseArtifact(html);
        return new Response(JSON.stringify({ reply: html, artifact, provider: 'template', model: 'fallback' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json', ...cors(request) }
        });
      }
    }

    return new Response(JSON.stringify({ error: 'All AI providers failed. Please try again.' }), {
      status: 502,
      headers: { 'Content-Type': 'application/json', ...cors(request) }
    });

  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json', ...cors(request) }
    });
  }
}
