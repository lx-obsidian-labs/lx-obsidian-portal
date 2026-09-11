/**
 * LX Obsidian Labs - Triple Fallback AI Worker
 * FREE first (NVIDIA), Cheap second (OpenRouter Gemini), Smart third (Claude Haiku)
 * 
 * Money Machine: 90% messages FREE, 9% cost 2 cents, 1% uses Claude
 * 
 * Keys needed (all free to start):
 * 1. NVIDIA NIM: build.nvidia.com -> nvapi-xxx (1000 free credits)
 * 2. OpenRouter: openrouter.ai/keys -> sk-or-xxx ($5 = 10,000 bookings)
 * 3. Gemini via OpenRouter: Same key works, no separate Google key!
 */

export default {
  async fetch(request, env, ctx) {
    // Health check
    if (request.url === 'https://lxobsidian.co.za/health') {
      return new Response(JSON.stringify({ 
        status: 'ok', 
        robot: 'triple-fallback',
        message: 'LX Receptionist AI running'
      }), {
        headers: { 'Content-Type': 'application/json' }
      });
    }

    // WhatsApp webhook endpoint
    if (request.url.includes('/webhook/whatsapp')) {
      return handleWhatsWebhook(request, env);
    }

    // Root - simple response
    if (request.url === 'https://lxobsidian.co.za/' || request.url === 'https://lxobsidian.co.za/index.html') {
      return new Response(`<h1>LX Obsidian Labs AI Receptionist</h1>
<p>Bot running with triple-fallback AI:<br>
• 90% FREE (NVIDIA NIM)<br>
• 9% cheap (OpenRouter Gemini - 2¢/1000 msg)<br>
• 1% Claude Haiku fallback</p>
<a href="#setup" style="color:#7c5cff">Setup instructions</a>`, {
        headers: { 'Content-Type': 'text/html; charset=utf-8' }
      });
    }

    return new Response('LX Obsidian Labs Robot Running', {
      headers: { 'Content-Type': 'text/plain' }
    });
  }
}

/**
 * MAIN WHATSAPP WEBHOOK HANDLER
 */
async function handleWhatsWebhook(request, env) {
  try {
    const body = await request.json();
    const entry = body.entry?.[0];
    const changes = entry?.changes?.[0]?.value;
    const message = changes?.messages?.[0]?.text?.body || '';
    const from = changes?.messages?.[0]?.from || '';

    if (!message || !from) return new Response('OK');

    // TRY ALL 3 AI MODELS - THE MONEY MACHINE LOGIC
    const aiReply = await callWithTripleFallback(env, message);

    // Send reply via WhatsApp Business API
    await fetch(`https://graph.facebook.com/v20.0/${env.WHATSAPP_PHONE_ID}/messages`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${env.WHATSAPP_TOKEN}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        messaging_product: 'whatsapp',
        to: from,
        text: { body: aiReply }
      })
    });

    return new Response('OK');
  } catch (error) {
    console.error('Webhook error:', error);
    return new Response('OK');
  }
}

/**
 * THE TRIPLE FALLBACK FUNCTION - YOUR MONEY MACHINE
 * 
 * Tries in order:
 * 1. NVIDIA NIM FREE ($0.00) - 90% of messages
 * 2. OpenRouter -> Gemini 2.0 Flash cheap ($0.02 per 1000 msgs) - 9% 
 * 3. OpenRouter -> Claude 3.5 Haiku ($0.80 per 1M tokens) - 1% final safety
 */
async function callWithTripleFallback(env, userMessage) {
  const systemPrompt = "You are LX Receptionist for South African businesses. You book appointments. Keep replies under 2 sentences. Friendly SA English. If user wants to book, say BOOK: [date time]. Do not ask for payment info."

  // ─── 1. TRY NVIDIA FREE FIRST ───
  try {
    const nvidiaRes = await fetch('https://integrate.api.nvidia.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${env.NVIDIA_API_KEY}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: 'meta/llama-3.1-70b-instruct',
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userMessage }
        ],
        max_tokens: 200,
        temperature: 0.2
      })
    });

    if (nvidiaRes.ok) {
      const data = await nvidiaRes.json();
      console.log('✓ Used NVIDIA - FREE');
      return data.choices[0].message.content;
    }
  } catch (e) {
    console.log('✗ NVIDIA failed, trying OpenRouter Gemini...');
  }

  // ─── 2. FALLBACK TO GEMINI VIA OPENROUTER ───
  try {
    const geminiRes = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${env.OPENROUTER_API_KEY}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: 'google/gemini-2.0-flash-001', // Fast, handles large knowledge, cheap
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userMessage }
        ],
        max_tokens: 200
      })
    });

    if (geminiRes.ok) {
      const data = await geminiRes.json();
      console.log('✓ Used Gemini via OpenRouter - ~2¢ per 1000 msgs');
      return data.choices[0].message.content;
    }
  } catch (e) {
    console.log('✗ Gemini failed, trying Claude Haiku...');
  }

  // ─── 3. FINAL FALLBACK - CLAUDE HAIKU ───
  try {
    const claudeRes = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${env.OPENROUTER_API_KEY}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: 'anthropic/claude-3.5-haiku', // Never fails, reliable fallback
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userMessage }
        ],
        max_tokens: 200
      })
    });

    if (claudeRes.ok) {
      const data = await claudeRes.json();
      console.log('✓ Used Claude Haiku fallback - rarely used');
      return data.choices[0].message.content;
    }
  } catch (e) {
    console.log('✗ All AI failed, using emergency response');
  }

  // Emergency fallback if ALL APIs fail
  return 'Hi! Thanks for contacting LX Obsidian Labs. Our team will get back to you shortly. BOOK: https://www.lxobsidianportal.co.za/contact';
}

/**
 * GET AI RESPONSE - SIMPLE VERSION (for non-WhatsApp use)
 */
async function getAIResponse(env, userMessage) {
  return await callWithTripleFallback(env, userMessage);
}