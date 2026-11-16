/* LX newsletter: store explicit opt-in; never log subscriber addresses. */
import { run } from './_db.js';
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
function json(payload,status=200) {
  return new Response(JSON.stringify(payload),{status,headers:{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
}
export async function onRequest(context) {
  const {request,env}=context;
  if(request.method!=='POST')return json({error:'Method not allowed'},405);
  const origin=request.headers.get('Origin');
  const allowed=new Set(['https://www.lxobsidianportal.co.za','https://lxobsidianportal.co.za','http://localhost:8788','http://localhost:8789']);
  if(!origin||origin!==new URL(request.url).origin||!allowed.has(origin))return json({error:'Invalid request origin'},403);
  if(!(request.headers.get('Content-Type')||'').toLowerCase().includes('application/json'))return json({error:'JSON required'},415);
  if(Number(request.headers.get('Content-Length')||0)>4096)return json({error:'Request too large'},413);
  let body;
  try {
    const reader=request.body?.getReader();
    if(!reader)return json({error:'Invalid request'},400);
    const decoder=new TextDecoder('utf-8',{fatal:true});let raw='',size=0;
    while(true){const {done,value}=await reader.read();if(done)break;size+=value.byteLength;if(size>4096){await reader.cancel().catch(()=>{});return json({error:'Request too large'},413);}raw+=decoder.decode(value,{stream:true});}
    body=JSON.parse(raw+decoder.decode());
  }catch{return json({error:'Invalid request'},400);}
  if(!body||typeof body!=='object'||Array.isArray(body)||body.consent!==true)return json({error:'Please confirm newsletter consent.'},400);
  const email=typeof body.email==='string'?body.email.trim().toLowerCase():'';
  if(email.length>254||!EMAIL_RE.test(email))return json({error:'Please enter a valid email address.'},400);
  if(!env.DB)return json({error:'Newsletter signup is temporarily unavailable. Please try again later.'},503);
  const source=body.source==='homepage'?'newsletter-opt-in:v1:homepage':'newsletter-opt-in:v1:footer';
  try {
    await run(context,'INSERT INTO subscribers (email,source,created_at) VALUES (?,?,?) ON CONFLICT(email) DO UPDATE SET source=excluded.source,created_at=excluded.created_at WHERE subscribers.source NOT LIKE \'newsletter-opt-in:v1:%\'',email,source,new Date().toISOString());
    return json({ok:true,stored:true});
  }catch{
    return json({error:'Your signup could not be saved. Please try again later.'},503);
  }
}