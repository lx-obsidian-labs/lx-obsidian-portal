const BASE='https://clockintoday.com/api/v1/';
const headers={'Content-Type':'application/json; charset=utf-8','Cache-Control':'public, max-age=300','X-Content-Type-Options':'nosniff'};
function reply(body,status=200){return new Response(JSON.stringify(body),{status,headers})}
function clean(value){return typeof value==='string'?value.trim().slice(0,200):''}
function first(...args){return args.find(v=>typeof v==='string'&&v.trim())||''}
function normalize(item){if(!item||typeof item!=='object')return null;const name=clean(first(item.name,item.title,item.platform));if(!name)return null;return {name,description:clean(first(item.description,item.summary,item.tagline)),category:clean(first(item.category,item.category_name)),location:clean(first(item.location,item.region,item.country)),payment:clean(first(item.payment,item.pay_type)),url:clean(first(item.website_url,item.website,item.url,item.application_url))}}
export async function onRequestGet({request}) {
 const url=new URL(request.url),mode=url.searchParams.get('mode'),q=clean(url.searchParams.get('q')||''),category=clean(url.searchParams.get('category')||'');
 if(q.length>100||category.length>100)return reply({error:'Invalid search'},400);
 const endpoint=new URL(mode==='categories'?'categories':'platforms',BASE);
 if(mode!=='categories'){if(q)endpoint.searchParams.set('q',q);if(category)endpoint.searchParams.set('category',category)}
 try{
   const response=await fetch(endpoint.toString(),{headers:{Accept:'application/json'},signal:AbortSignal.timeout(7000),cf:{cacheTtl:300,cacheEverything:true}});
   if(!response.ok)return reply({error:'Provider unavailable'},502);
   const payload=await response.json();
   if(mode==='categories'){const arr=Array.isArray(payload)?payload:Array.isArray(payload.categories)?payload.categories:Array.isArray(payload.data)?payload.data:[];return reply({categories:arr.slice(0,100).map(x=>typeof x==='string'?x:clean(x?.name)).filter(Boolean)})}
   const arr=Array.isArray(payload)?payload:Array.isArray(payload.platforms)?payload.platforms:Array.isArray(payload.data)?payload.data:Array.isArray(payload.results)?payload.results: Array.isArray(payload.data?.platforms)?payload.data.platforms:[];
   const items=arr.slice(0,100).map(normalize).filter(Boolean);
   const filtered=items.filter(p=>(!q||[p.name,p.description,p.category].join(' ').toLowerCase().includes(q.toLowerCase()))&&(!category||p.category.toLowerCase().includes(category.toLowerCase())));
   return reply({items:filtered,source:'ClockInToday',note:'Work platforms, not individual verified vacancies'});
 }catch{return reply({error:'Platform data temporarily unavailable'},502)}
}