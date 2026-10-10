// Find which CSS classes used in HTML are defined in lx.css vs saas.css vs neither
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');

function classesIn(file) {
  const src = fs.readFileSync(path.join(root, file), 'utf8');
  const set = new Set();
  // crude: capture ".foo" selector chunks (incl chained .a.b and media queries)
  const re = /\.([A-Za-z][A-Za-z0-9_-]*)/g;
  let m;
  while ((m = re.exec(src))) set.add(m[1]);
  return set;
}

const lx = classesIn('css/lx.css');
const saas = classesIn('css/saas.css');

// classes used in deployed HTML
const pages = ['index.html','404.html','about.html','ai.html','blog.html','contact.html',
  'faq.html','marketplace.html','portfolio.html','services.html','synapse.html','vista.html',
  'local-visibility.html','mobile-apps.html','integrations.html','help-us-grow.html'];

const used = new Map(); // class -> pages
for (const p of pages) {
  const src = fs.readFileSync(path.join(root, p), 'utf8');
  const re = /class="([^"]+)"/g;
  let m;
  while ((m = re.exec(src))) {
    for (const c of m[1].split(/\s+/)) {
      if (!c) continue;
      if (!used.has(c)) used.set(c, new Set());
      used.get(c).add(p);
    }
  }
  // also classList-style usage in inline scripts
}

const onlySaas = [], neither = [];
for (const [c, ps] of used) {
  if (lx.has(c)) continue;
  if (saas.has(c)) { onlySaas.push(`${c}  [${[...ps].join(',')}]`); continue; }
  neither.push(`${c}  [${[...ps].join(',')}]`);
}

console.log('=== Classes used in HTML but ONLY defined in saas.css (break if saas.css dropped) ===');
console.log(onlySaas.join('\n') || '(none)');
console.log('\n=== Classes used in HTML but defined in NEITHER lx.css nor saas.css (inline-styled or missing) ===');
console.log(neither.join('\n') || '(none)');

// Also check which :root tokens differ
const lxRoot = fs.readFileSync(path.join(root,'css/lx.css'),'utf8').match(/:root\s*{[\s\S]*?}/)[0];
const saasRoot = fs.readFileSync(path.join(root,'css/saas.css'),'utf8').match(/:root\s*{[\s\S]*?}/)[0];
function tokens(s){const o={};const re=/(--[a-z0-9-]+)\s*:\s*([^;]+);/g;let m;while((m=re.exec(s)))o[m[1]]=m[2].trim();return o;}
const a = tokens(lxRoot), b = tokens(saasRoot);
console.log('\n=== :root token conflicts (lx.css value -> saas.css override in dist) ===');
for (const k of Object.keys(b)) if (k in a && a[k] !== b[k]) console.log(`${k}: ${a[k]}  ==>  ${b[k]}`);
