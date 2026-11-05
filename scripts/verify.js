const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const dist = path.resolve(__dirname, '../dist');
const required = ['index.html', 'pdf/index.html', 'synapse/web/index.html', '404.html', '_redirects', 'css/lx.css'];
for (const name of required) {
  if (!fs.existsSync(path.join(dist, name))) throw Error('Missing required asset: ' + name);
}
const home = fs.readFileSync(path.join(dist, 'index.html'), 'utf8');
const routes = fs.readFileSync(path.join(dist, '_redirects'), 'utf8');
if (!home.includes('href="/pdf"')) throw Error('Homepage PDF Studio link missing');
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
if(!home.includes('We engineer custom software')) throw Error('Homepage positioning reverted');
console.log('Marketing verification passed: secure lead capture, honest CTAs, case study snapshots, updated positioning');

// Site-wide design, navigation and link-integrity guardrails.
const htmlFiles = fs.readdirSync(dist).filter(name => name.endsWith('.html'));
const canonicalNavigation = home.match(/<header class="velora-nav"[^>]*>[\s\S]*?<\/header>/)?.[0];
if(!canonicalNavigation || !canonicalNavigation.includes('href="/#solutions"') || !canonicalNavigation.includes('href="/#products"')) {
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
const expectedDownloads = [
 'lx-obsidian-capabilities-2026.pdf',
 'project-discovery-workbook.pdf',
 'vista-cinema-install-guide.pdf',
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
if (!downloads.includes('VistaCinema-v1.0.9.apk') || !downloads.includes('VistaCinema-TV-v1.0.9.apk')) throw Error('Latest official Android builds missing from downloads');
if (!downloads.includes('GitHub sign-in') || !downloads.includes('artifact may expire')) throw Error('Synapse build caveat missing');
if (downloads.includes('href="#"')) throw Error('Placeholder link on download hub');
console.log('Downloads checks passed: PDF headers, files, discoverability, release links');
