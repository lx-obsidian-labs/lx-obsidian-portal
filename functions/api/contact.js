export async function onRequest(context) {
  var headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Content-Type': 'application/json'
  };

  if (context.request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers });
  }

  if (context.request.method !== 'POST') {
    return new Response(JSON.stringify({ error: 'Method not allowed' }), { status: 405, headers });
  }

  try {
    var body = await context.request.json();
    var name = (body.name || '').trim();
    var email = (body.email || '').trim();
    var subject = (body.subject || '').trim();
    var message = (body.message || '').trim();

    if (!name || !email || !subject || !message) {
      return new Response(JSON.stringify({ error: 'All fields are required' }), { status: 400, headers });
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return new Response(JSON.stringify({ error: 'Invalid email' }), { status: 400, headers });
    }

    if (typeof process !== 'undefined' && process.env && process.env.CONTACT_EMAIL) {
      var to = process.env.CONTACT_EMAIL;
      console.log('Contact form submission to:', to);
    }

    console.log('Contact submission:', { name, email, subject, messageLength: message.length });

    return new Response(JSON.stringify({ ok: true }), { status: 200, headers });
  } catch (err) {
    return new Response(JSON.stringify({ error: 'Internal error' }), { status: 500, headers });
  }
}
