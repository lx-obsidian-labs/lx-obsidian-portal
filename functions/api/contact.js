// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
// Preserve existing third-party attribution and applicable notices.
// Project enquiry API — accepts same-origin form submissions only.
// D1 is the durable source of truth; optional LEADS_WEBHOOK_URL enables notification.
const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status,
  headers: {'Content-Type':'application/json; charset=utf-8', 'Cache-Control':'no-store',
    'X-Content-Type-Options':'nosniff'}
});
const clean = (value, max = 250) => typeof value === 'string' ? value.trim().slice(0, max) : '';
const emailOK = value => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) && value.length <= 254;
export async function onRequest(context) {
  const { request, env } = context;
  if (request.method !== 'POST') return json({error:'Method not allowed'},405);
  const origin = request.headers.get('Origin');
  const url = new URL(request.url);
  const allowed = new Set(['https://www.lxobsidianportal.co.za','https://lxobsidianportal.co.za','http://localhost:8788','http://localhost:8789']);
  // Browser-origin enforcement prevents arbitrary third-party website submissions.
  if (!origin || !(origin === url.origin || allowed.has(origin) && origin.endsWith('lxobsidianportal.co.za'))) {
    return json({error:'Untrusted origin'},403);
  }
  if (!(request.headers.get('Content-Type') || '').toLowerCase().includes('application/json')) {
    return json({error:'JSON request required'},415);
  }
  if (Number(request.headers.get('Content-Length') || 0) > 12000) return json({error:'Request too large'},413);
  if (!env.DB) return json({error:'Service temporarily unavailable'},503);
  // Enforce the actual streamed byte count, not just the optional Content-Length.
  let input;
  try {
    const reader = request.body?.getReader();
    if (!reader) return json({error:'Invalid JSON'},400);
    const decoder = new TextDecoder('utf-8', {fatal:true});
    let bytes = 0, raw = '';
    while (true) {
      const {done,value} = await reader.read();
      if (done) break;
      bytes += value.byteLength;
      if (bytes > 12000) {
        await reader.cancel().catch(()=>{});
        return json({error:'Request too large'},413);
      }
      raw += decoder.decode(value,{stream:true});
    }
    raw += decoder.decode();
    input = JSON.parse(raw);
  } catch { return json({error:'Invalid JSON'},400); }
  if (!input || typeof input !== 'object' || Array.isArray(input)) return json({error:'Invalid request'},400);
  // Honeypot: bots should not get a stored enquiry.
  if (clean(input.company_website,500)) return json({ok:true, reference:'RECEIVED'});
  const name=clean(input.name,120), email=clean(input.email,254).toLowerCase();
  const subject=clean(input.subject,180), message=clean(input.message,4000);
  const projectType=clean(input.projecttype,120), industry=clean(input.industry,120);
  const business=clean(input.business_name,160), phone=clean(input.phone,60);
  const budget=clean(input.budget,80), timeline=clean(input.launch,80);
  const nda=input.nda === true ? 1 : 0;
  if (name.length < 2 || !emailOK(email) || subject.length < 4 || message.length < 15 || !projectType || !industry) {
    return json({error:'Please complete all required fields with a valid email and a meaningful project description.'},400);
  }
  const id = 'LX-' + crypto.randomUUID().slice(0,12).toUpperCase();
  try {
    await env.DB.prepare(
      'INSERT INTO project_leads (id,name,business_name,email,phone,industry,project_type,subject,message,budget,timeline,nda_requested) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)'
    ).bind(id,name,business,email,phone,industry,projectType,subject,message,budget,timeline,nda).run();
  } catch (err) {
    console.error('lead_storage_failed', String(err?.message || err));
    return json({error:'We could not save your enquiry. Please try again or contact us directly.'},503);
  }
  // Webhook is optional; never leak its URL/token or the lead payload to logs.
  // Use a private authenticated notification endpoint, not a public chat URL.
  if (env.LEADS_WEBHOOK_URL && /^https:\/\//.test(env.LEADS_WEBHOOK_URL)) {
    const notify = async () => {
      try {
        const headers={'Content-Type':'application/json'};
        if(env.LEADS_WEBHOOK_TOKEN) headers.Authorization='Bearer '+env.LEADS_WEBHOOK_TOKEN;
        const response=await fetch(env.LEADS_WEBHOOK_URL,{method:'POST',headers,body:JSON.stringify({
          reference:id,name,business_name:business,email,phone,industry,projecttype:projectType,subject,message,budget,timeline,nda_requested:!!nda
        })});
        await env.DB.prepare('UPDATE project_leads SET notification_status=? WHERE id=?').bind(response.ok?'sent':'failed',id).run();
      } catch(e) {
        console.error('lead_notification_failed',id);
        await env.DB.prepare('UPDATE project_leads SET notification_status=? WHERE id=?').bind('failed',id).run().catch(()=>{});
      }
    };
    context.waitUntil(notify());
  }
  return json({ok:true,reference:id,saved:true},201);
}
