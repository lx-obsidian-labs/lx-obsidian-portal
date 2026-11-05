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

\n// Marketing and enquiry regression checks.
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
