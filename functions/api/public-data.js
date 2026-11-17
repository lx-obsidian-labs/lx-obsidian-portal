// LX Obsidian Labs — fixed-provider, read-only public data endpoints.
const currencies=new Set(["ZAR","USD","EUR","GBP","JPY","CAD","AUD","CHF","CNY","INR","BWP","NAD","KES","NGN"]);
const countries=new Set(["ZAF","SWZ","BWA","NAM","KEN","NGA","USA","GBR"]);
const holidayCountries=new Set(['ZA','GB','US','DE','BW']);
const indicators=new Set(['SP.POP.TOTL','NY.GDP.MKTP.CD','SL.UEM.TOTL.ZS']);
const json=(data,status=200)=>Response.json(data,{status,headers:{'Cache-Control':status===200?'public, max-age=300':'no-store','X-Content-Type-Options':'nosniff'}});
export async function onRequestGet({request}){
 const q=new URL(request.url).searchParams;const type=q.get('type');let url;
 if(type==='currency'){const from=q.get('from'),to=q.get('to');if(!currencies.has(from)||!currencies.has(to)||from===to)return json({error:'Choose two different supported currencies.'},400);url='https://api.frankfurter.dev/v2/rates?base='+from+'&quotes='+to;}
 else if(type==='country'){const c=q.get('country'),i=q.get('indicator');if(!countries.has(c)||!indicators.has(i))return json({error:'Invalid country or indicator.'},400);url='https://api.worldbank.org/v2/country/'+c+'/indicator/'+i+'?format=json&mrnev=1';}
 else if(type==='holidays'){const c=q.get('country'),y=q.get('year');if(!holidayCountries.has(c)||!/^20(2[5-9]|30)$/.test(y||''))return json({error:'Choose a supported country and year.'},400);url='https://nagerholidays.com/api/v4/Holidays/'+c+'/'+y;}
 else return json({error:'Unknown data tool.'},400);
 try{const res=await fetch(url,{headers:{Accept:'application/json'},signal:AbortSignal.timeout(8000),redirect:'error',cf:{cacheEverything:true,cacheTtl:3600}});if(!res.ok)throw Error('Upstream unavailable');const d=await res.json();
 if(type==='currency'){const row=Array.isArray(d)&&d.find(x=>x.base===q.get('from')&&x.quote===q.get('to'));if(!row||!Number.isFinite(row.rate)||row.rate<=0||typeof row.date!=='string')throw Error('Missing rate');return json({rate:row.rate,date:row.date});}
 if(type==='country'){const row=Array.isArray(d?.[1])&&d[1].find(x=>typeof x.value==='number');if(!row)throw Error('No data');return json({value:row.value,year:row.date,indicator:row.indicator?.value});}
 if(!Array.isArray(d))throw Error('Invalid holiday data');const rows=d.filter(x=>typeof x.date==='string').slice(0,80).map(x=>({date:x.date,name:typeof x.name==='string'?x.name:typeof x.localName==='string'?x.localName:Array.isArray(x.name)?x.name.find(n=>n.languageCode==='EN')?.text||x.name[0]?.text||'Holiday':Array.isArray(x.names)?x.names.find(n=>n.languageCode==='EN')?.text||x.names[0]?.text||'Holiday':'Holiday'}));return json({holidays:rows});
 }catch{return json({error:'Data provider is unavailable or has no matching result. Please try again later.'},502);}
}
