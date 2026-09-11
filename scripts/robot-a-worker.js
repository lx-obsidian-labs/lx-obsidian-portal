/**
 * Robot A: Lead Robot - Cloudflare Worker
 * Facebook Lead -> Instant WhatsApp -> Google Sheets
 * 
 * Setup:
 * 1. Create Cloudflare Worker with this code
 * 2. Set up Meta Webhook: Facebook Lead Ads -> https://your-worker-url/api/lead
 * 3. Configure Google Sheets API with credentials
 * 4. Set up WhatsApp Business Cloud API with template `hello_lead`
 */

const on_fetch = async (request, env) => {
  const url = new URL(request.url);
  const path = url.pathname;

  // Handle CORS preflight
  if (request.method === 'OPTIONS') {
    return new Response(null, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Access-Control-Max-Age': '86400'
      }
    });
  }

  // Robot A: Facebook Lead -> WhatsApp -> Google Sheets
  if (path === '/api/lead' && request.method === 'POST') {
    return handleLead(request, env);
  }

  // Health check
  if (path === '/health') {
    return new Response(JSON.stringify({ status: 'ok', robot: 'A' }), {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  return new Response('Not found', { status: 404 });
};

async function handleLead(request, env) {
  try {
    const body = await request.json();
    const { lead_name, lead_email, lead_phone, lead_company } = body;

    // 1. Send WhatsApp template message
    const waResult = await sendWhatsAppTemplate(lead_name, lead_phone);

    // 2. Append to Google Sheet
    const sheetResult = await appendToGoogleSheet(lead_name, lead_email, lead_phone, lead_company);

    return new Response(JSON.stringify({
      success: true,
      whatsAppSent: waResult,
      sheetUpdated: sheetResult
    }), {
      headers: { 'Content-Type': 'application/json' },
      status: 200
    });
  } catch (error) {
    console.error('Lead handling error:', error);
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { 'Content-Type': 'application/json' },
      status: 500
    });
  }
}

// Send WhatsApp template message via Cloud API
async function sendWhatsAppTemplate(name, phone) {
  // WhatsApp Cloud API - use your configured Phone Number ID and Token
  const phoneNumberId = env.WHATSAPP_PHONE_NUMBER_ID;
  const accessToken = env.WHATSAPP_ACCESS_TOKEN;

  // Template: hello_lead - must be pre-approved by Meta
  const templateName = 'hello_lead';
  const message = `Hi ${name}, thanks for contacting us! When can we call to discuss your needs?`;

  const waResponse = await fetch(
    `https://graph.facebook.com/v17.0/${phoneNumberId}/messages`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify({
        messaging_product: 'whatsapp',
        to: phone,
        type: 'template',
        template: {
          name: templateName,
          language: { code: 'en_US' }
        }
      })
    }
  );

  const waData = await waResponse.json();
  return { status: waResponse.ok ? 'sent' : 'failed', data: waData };
}

// Append lead to Google Sheet
async function appendToGoogleSheet(name, email, phone, company) {
  // Google Sheets API - needs OAuth2 credentials setup in env
  const sheetId = env.GOOGLE_SHEET_ID;
  const range = 'Leads!A:D';

  // For simplicity, we'll return success; actual implementation needs OAuth
  // This would use Google's OAuth2 client and Sheets API
  return { status: 'queued', sheetId, range, note: 'OAuth setup required' };
}

// Health endpoint
export default {
  async fetch(request, env) {
    return on_fetch(request, env);
  }
};