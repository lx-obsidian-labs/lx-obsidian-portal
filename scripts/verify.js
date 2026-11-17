// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
// Preserve existing third-party attribution and applicable notices.
const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const dist = path.resolve(__dirname, '../dist');
if (fs.existsSync(path.join(dist,'docs'))) throw Error('Internal engineering documentation leaked into static output');
const required = ['index.html', 'pdf/index.html', 'synapse/web/index.html', '404.html', '_redirects', 'css/lx.css'];
for (const name of required) {
  if (!fs.existsSync(path.join(dist, name))) throw Error('Missing required asset: ' + name);
}
const home = fs.readFileSync(path.join(dist, 'index.html'), 'utf8');
const routes = fs.readFileSync(path.join(dist, '_redirects'), 'utf8');
if (!home.includes('href="/pdf/"')) throw Error('Homepage PDF Studio link missing');
if (!fs.existsSync(path.join(dist, 'pdf/index.html'))) throw Error('PDF Studio directory route missing');
if (fs.existsSync(path.join(dist, 'pdf.html'))) throw Error('Duplicate PDF entrypoint causes Pages clean URL conflict');
function check(dir) {
  for (const item of fs.readdirSync(dir, {withFileTypes:true})) {
    const file = path.join(dir, item.name);
    if (item.isDirectory()) check(file);
    else if (file.endsWith('.js') && !file.endsWith('.min.js')) {
      const result = cp.spawnSync(process.execPath, ['--check', file], {encoding:'utf8'});
      if (result.status !== 0) throw Error('JavaScript syntax error in ' + file + ': ' + result.stderr);
    }
  }
}
check(dist);
console.log('Release verification passed: required assets, PDF route, homepage link and JS syntax');

const synapseWeb = fs.readFileSync(path.join(dist, 'synapse/web/index.html'), 'utf8');
if (!synapseWeb.includes("fetch('/api/chat'")) throw Error('Synapse web chat endpoint missing');
const synapseLanding = fs.readFileSync(path.join(dist, 'synapse.html'), 'utf8');
if (!synapseLanding.includes('/synapse/web') || !synapseLanding.includes('release-notes')) throw Error('Synapse release notes or web link missing');


// Marketing and enquiry regression checks.
const contact = fs.readFileSync(path.join(dist,'contact.html'),'utf8');
const contactApi = fs.readFileSync(path.join(dist,'functions/api/contact.js'),'utf8');
const portfolio = fs.readFileSync(path.join(dist,'portfolio.html'),'utf8');
const integrations = fs.readFileSync(path.join(dist,'integrations.html'),'utf8');
const ai = fs.readFileSync(path.join(dist,'ai.html'),'utf8');
if(!contact.includes("fetch('/api/contact'") || !contact.includes('result.saved') || !contact.includes('result.reference')) throw Error('Contact flow lacks server-confirmed durable submission');
if(contact.includes("window.open('https://wa.me/27694829711?text='")) throw Error('Legacy false-positive WhatsApp contact handler remains');
if(!contactApi.includes('INSERT INTO project_leads') || !contactApi.includes('env.DB.prepare')) throw Error('Lead storage endpoint is missing');
if(!contactApi.includes('Untrusted origin') || !contactApi.includes('emailOK')) throw Error('Server-side validation guard missing');
if(!portfolio.includes('id="project-snapshots"')) throw Error('Engineering case-study snapshots missing');
if(integrations.includes('href="#" class="integration-card"')) throw Error('Dead integration CTAs present');
if(ai.includes('index.html#pricing')) throw Error('Invalid AI pricing link present');
if(!/<h1\b[^>]*id="home-title"[^>]*>Websites, apps and AI<br>\s*<span>for your business\.<\/span><\/h1>/.test(home)) throw Error('Homepage positioning reverted');
console.log('Marketing verification passed: secure lead capture, honest CTAs, case study snapshots, updated positioning');

// Site-wide design, navigation and link-integrity guardrails.
const htmlFiles = fs.readdirSync(dist).filter(name => name.endsWith('.html'));
const canonicalNavigation = home.match(/<header class="velora-nav"[^>]*>[\s\S]*?<\/header>/)?.[0];
if(!canonicalNavigation || !canonicalNavigation.includes('href="/#start-here"') || !canonicalNavigation.includes('href="/#study-tools"')) {
 throw Error('Shared navigation must use cross-page-safe homepage anchor URLs');
}
if (home.includes('https://www.linkedin.com') || home.includes('https://x.com') || home.includes('https://www.youtube.com')) throw Error('Generic social media domains are not verified company accounts');
for(const file of htmlFiles) {
 const content=fs.readFileSync(path.join(dist,file),'utf8');
 if(!/<h1[\s>]/i.test(content)) throw Error('Missing page H1: '+file);
 const nav=content.match(/<header class="velora-nav"[^>]*>[\s\S]*?<\/header>/)?.[0];
 if(nav && nav!==canonicalNavigation) throw Error('Navigation drift: '+file);
 for(const match of content.matchAll(/\b(?:src|href)=(["'])([^"']+)\1/g)) {
  const href=match[2];
  if(!href || /^(https?:|mailto:|tel:|data:|blob:|javascript:|\/\/|#)/i.test(href)) continue;
  let route=href.split(/[?#]/)[0].replace(/^\//,'');
  if(!route || route.startsWith('api/')) continue;
  const candidates=[route,route+'.html',route+'/index.html'];
  if(!candidates.some(candidate=>fs.existsSync(path.join(dist,candidate)))) throw Error('Broken local link '+href+' in '+file);
 }
}
if (fs.readFileSync(path.join(dist,'services.html'),'utf8').includes('Website Launch Pricing')) throw Error('Old price-led enterprise services section remains');
if (fs.readFileSync(path.join(dist,'about.html'),'utf8').includes("We don't use templates")) throw Error('Absolute no-templates claim remains');
console.log('Site-wide audit passed for '+htmlFiles.length+' pages: nav consistency, local links, core content claims');

// Download hub regression tests: verified generated files and honest links.
const downloads = fs.readFileSync(path.join(dist,'downloads.html'),'utf8');
const vistaRelease = JSON.parse(fs.readFileSync(path.join(dist,'vista-version.json'),'utf8'));
const expectedDownloads = [
 'lx-obsidian-capabilities-2026.pdf',
 'project-discovery-workbook.pdf',
 'vista-watch-install-guide.pdf',
 'project-requirements-template.csv'
];
for (const file of expectedDownloads) {
 const output=path.join(dist,'downloads',file);
 if (!fs.existsSync(output)) throw Error('Missing downloadable resource: '+file);
 if (!downloads.includes('/downloads/'+file)) throw Error('Downloads page does not link to: '+file);
 const bytes=fs.readFileSync(output);
 if (bytes.length<150) throw Error('Resource unexpectedly small: '+file);
 if (file.endsWith('.pdf') && (!bytes.subarray(0,8).toString('latin1').startsWith('%PDF-1.') || !bytes.toString('latin1').trimEnd().endsWith('%%EOF'))) throw Error('Invalid PDF wrapper: '+file);
}
if (!home.includes('href="/downloads"') || !home.includes('Downloads &amp; Resources')) throw Error('Downloads not discoverable in primary navigation/footer');
if (!downloads.includes('Version '+vistaRelease.versionName) || !downloads.includes('/api/vista-download?platform=android') || !downloads.includes('/api/vista-download?platform=tv')) throw Error('Latest official Android builds missing from downloads');
if (!downloads.includes('GitHub sign-in') || !downloads.includes('artifact may expire')) throw Error('Synapse build caveat missing');
if (downloads.includes('href="#"')) throw Error('Placeholder link on download hub');
console.log('Downloads checks passed: PDF headers, files, discoverability, release links');

// Resource-centre UX regression checks.
if (!downloads.includes('id="dlSearch"') || !downloads.includes('id="dlResults"') || !downloads.includes('id="dlEmpty"')) throw Error('Download centre search/empty states missing');
if (!downloads.includes('id="download-help"') || !downloads.includes('data-copy="'+vistaRelease.sha256+'"')) throw Error('Download safety guidance or published checksum missing');
if (!fs.readFileSync(path.join(dist,'services.html'),'utf8').includes('/downloads/project-discovery-workbook.pdf')) throw Error('Services resource CTA missing');
if (!contact.includes('/downloads/project-discovery-workbook.pdf')) throw Error('Contact brief workbook link missing');
if (!fs.readFileSync(path.join(dist,'vista.html'),'utf8').includes('Open Downloads &amp; Resources')) throw Error('Vista download centre link missing');
console.log('Resource centre UX checks passed');

for (const file of htmlFiles) {
 const html = fs.readFileSync(path.join(dist,file),'utf8');
 if (html.includes('Vista Cinema') || html.includes('VISTA CINEMA')) throw Error('Retired product name published in '+file);
 if (html.includes('vista-marketing-1.png') || html.includes('vista-marketing-2.png')) throw Error('Retired brand artwork referenced in '+file);
}
if (!downloads.includes('Vista Watch') || !fs.existsSync(path.join(dist,'assets/vista-watch-mark.svg'))) throw Error('Vista Watch identity is missing');
console.log('Vista Watch branding passed across published pages, identity assets and download guides');

for (const id of ['start-here','study-tools']) if (!home.includes('id="'+id+'"')) throw Error('Missing visitor journey: '+id);
for (const href of ['/pdf/','/synapse','/downloads','/contact']) if (!home.includes('href="'+href+'"')) throw Error('Missing task destination: '+href);

// Company history and transparent pricing regression guards.
const servicePage=fs.readFileSync(path.join(dist,'services.html'),'utf8');
const aboutPage=fs.readFileSync(path.join(dist,'about.html'),'utf8');
const aiSummary=fs.readFileSync(path.join(dist,'llms.txt'),'utf8');
for(const [name,content] of [['homepage',home],['about',aboutPage],['llms.txt',aiSummary]]) {
 if(!content.includes('Swaziland (now Eswatini)') || !content.includes('2020') || /Founded in South Africa in 2016/i.test(content)) throw Error('Company origin story mismatch: '+name);
}
const pricingKeys=['website','professional','ecommerce','mobile','automation','system','social-starter','social-growth','social-plus','care','seo','managed','domain-coza','domain-com'];
for(const key of pricingKeys){
 const card=servicePage.match(new RegExp('<article class="lx-rate" data-package="'+key+'">[\\s\\S]*?<\\/article>'))?.[0];
 if(!card || !card.includes('What you get') || !card.includes('What costs extra:') || !card.includes('lx-rate-price')) throw Error('Incomplete pricing disclosure: '+key);
 if(!contact.includes('value="'+key+'"')) throw Error('Quote option missing: '+key);
}
for(const domainKey of ['domain-coza','domain-com']) if(!servicePage.includes('href="/contact?offer='+domainKey+'"')) throw Error('Domain quote routing missing: '+domainKey);
if(!contact.includes('id="lxQuoteFeatures"') || !contact.includes('id="lxQuoteExclusions"') || !contact.includes("p.term==='annual'")) throw Error('Quote scope/annual pricing missing');
const currentFooter=home.match(/<footer class="glass-footer"[^>]*>[\s\S]*?<\/footer>/)?.[0]||'';
for(const local of ['nathan','sales','info','support','accounts','admin','partnerships','careers','hello']) {
 const email=local+'@lxobsidianportal.co.za';
 if(!currentFooter.includes('href="mailto:'+email+'"') || !currentFooter.includes('>'+email+'</a>')) throw Error('Public business email missing: '+email);
}
console.log('Company history, 14 pricing packages, quote options and public contact links verified');

// Public ecosystem: discovery, honest availability and working contact routes.
for (const slug of ['automation','careers','partners']) {
 const page=fs.readFileSync(path.join(dist,slug+'.html'),'utf8');
 if (!home.includes('href="/'+slug+'"') || !page.includes('https://www.lxobsidianportal.co.za/'+slug) || !page.includes('mailto:sales@lxobsidianportal.co.za?subject=')) throw Error('Ecosystem route or enquiry missing: '+slug);
 if (!fs.readFileSync(path.join(dist,'sitemap.xml'),'utf8').includes('https://www.lxobsidianportal.co.za/'+slug+'</loc>')) throw Error('Missing sitemap route: '+slug);
}
if (!fs.readFileSync(path.join(dist,'automation.html'),'utf8').includes('Public MCP releases are not yet available')) throw Error('MCP availability disclosure missing');
if (!fs.readFileSync(path.join(dist,'careers.html'),'utf8').includes('No open vacancies are currently advertised')) throw Error('Career availability disclosure missing');
if (/^\/partners\s+\/contact/m.test(routes)) throw Error('Partner page shadowed by legacy redirect');
console.log('Public ecosystem checks passed');

const publicTools=fs.readFileSync(path.join(dist,'tools.html'),'utf8');if(!home.includes('href="/tools"')||!publicTools.includes('data-tool="currency"')||!publicTools.includes('data-tool="password"')||!fs.existsSync(path.join(dist,'functions/api/public-data.js')))throw Error('Public tools release incomplete');console.log('Public utilities checks passed');

// Pricing consistency: every advertised base price matches the quote calculator.
const rateCard={"website":6000,"professional":12000,"ecommerce":22000,"mobile":35000,"automation":18000,"system":45000,"social-starter":950,"social-growth":1800,"social-plus":3500,"care":850,"seo":1800,"managed":3000,"domain-coza":249,"domain-com":499};
const planMatch=contact.match(/const plans=(\{[^\n]+\});/);if(!planMatch)throw Error('Quote rate data missing');const quoteRates=JSON.parse(planMatch[1]);
for(const [key,value] of Object.entries(rateCard)){const card=servicePage.match(new RegExp('<article class="lx-rate" data-package="'+key+'">[\\s\\S]*?<\\/article>'))?.[0];if(!card||!card.includes('R'+value.toLocaleString('en-US'))||quoteRates[key]?.price!==value)throw Error('Price mismatch: '+key);const option=contact.match(new RegExp('<option value="'+key+'">[^<]+<\\/option>'))?.[0];if(!option||!option.includes('R'+value.toLocaleString('en-US')))throw Error('Quote option price mismatch: '+key);}
if(!home.includes('R6,000')||!aiSummary.includes('R6,000')||!contact.includes('x*650+y')||!contact.includes('value="850">Website care'))throw Error('Pricing rollout incomplete');
console.log('Pricing consistency verified');

// Canonical consistency and high-priority SEO discovery.
const seoSitemap=fs.readFileSync(path.join(dist,'sitemap.xml'),'utf8');
const seoOrigin='https://www.lxobsidianportal.co.za';
const seoUrls=[...seoSitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map(m=>m[1]);
if(new Set(seoUrls).size!==seoUrls.length)throw Error('Duplicate sitemap entries');
for(const [route,file] of [['/pdf/','pdf/index.html'],['/privacy-policy','privacy-policy.html'],['/vista-terms','vista-terms.html'],['/synapse/web','synapse/web/index.html']]){const content=fs.readFileSync(path.join(dist,file),'utf8');if(!seoUrls.includes(seoOrigin+route)||!content.includes('rel="canonical" href="'+seoOrigin+route+'"'))throw Error('Canonical mismatch: '+route);}
for(const route of ['/software-development-guide-south-africa','/ai-automation-guide','/obsidian-labs','/engineering-case-studies'])if(!home.includes('href="'+route+'"'))throw Error('SEO discovery link missing: '+route);
if(!home.includes('R6,000')||!servicePage.includes('Website development, software and AI services in South Africa'))throw Error('Commercial SEO positioning missing');
console.log('SEO canonical and discovery checks passed');
