export async function onRequest(context) {
  const { request } = context;

  if (request.method !== 'POST') {
    return new Response(JSON.stringify({ error: 'Method not allowed' }), {
      status: 405,
      headers: { 'Content-Type': 'application/json', 'Allow': 'POST' }
    });
  }

  try {
    const { message } = await request.json();

    if (!message || typeof message !== 'string') {
      return new Response(JSON.stringify({ error: 'Message is required' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    const systemPrompt = {
      role: 'system',
      content: 'You are LX Assistant, the official AI assistant for LX Obsidian Labs. ' +
        'You help users with questions about software development, AI, cloud computing, ' +
        'mobile apps, enterprise systems, and digital transformation. ' +
        'Be concise, professional, and helpful. ' +
        'If asked about pricing, direct users to the marketplace for app pricing or to the contact page for custom project quotes. ' +
        'If asked about contact, direct users to the contact page. ' +
        'Keep responses under 150 words. Do not mention that you are an AI model. ' +
        'Sign off with "— LX Assistant" at the end of longer responses.'
    };

    const NVIDIA_API_KEY = context.env.NVIDIA_API_KEY;

    if (!NVIDIA_API_KEY) {
      return new Response(JSON.stringify({ reply: 'AI service is not configured. Please contact support.' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    const response = await fetch('https://integrate.api.nvidia.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + NVIDIA_API_KEY,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: 'meta/llama3-70b-instruct',
        messages: [systemPrompt, { role: 'user', content: message }],
        max_tokens: 512,
        temperature: 0.7,
        top_p: 0.9
      })
    });

    const data = await response.json();

    let reply = 'Thanks for your message! Our team will follow up shortly.';
    if (data.choices && data.choices[0] && data.choices[0].message) {
      reply = data.choices[0].message.content.trim();
    }

    return new Response(JSON.stringify({ reply: reply }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });

  } catch (error) {
    return new Response(JSON.stringify({ reply: 'I apologize, but I\'m having trouble connecting right now. Please try again.' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  }
}
