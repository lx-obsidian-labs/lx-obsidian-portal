'use strict';
/* Read-only release audit. Run after npm run build: node scripts/audit-release.js */
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const dist = path.join(root, 'dist');
const errors = [], warnings = [];
const exists = p => fs.existsSync(path.join(dist, p));
const read = p => fs.readFileSync(path.join(dist, p), 'utf8');
const required = ['index.html','404.html','pdf/index.html','synapse/web/index.html','css/lx.css','_headers','_redirects','robots.txt','sitemap.xml'];
for (const p of required) if (!exists(p)) errors.push('Missing release file: '+p);
if (exists('index.html')) {
  const html = read('index.html');
  if (!/<html\b[^>]*lang=/i.test(html)) errors.push('Homepage missing language attribute');
  if (!/<meta\s+name=["']description["']/i.test(html)) errors.push('Homepage missing description');
  if (!/<link\s+rel=["']canonical["']/i.test(html)) errors.push('Homepage missing canonical URL');
  if (!/<h1\b/i.test(html)) errors.push('Homepage missing H1');
  if (!/<meta\s+name=["']viewport["']/i.test(html)) errors.push('Homepage missing viewport');
}
if (exists('_headers')) {
  const h = read('_headers');
  for (const key of ['Content-Security-Policy','X-Content-Type-Options','Referrer-Policy','Strict-Transport-Security']) {
    if (!h.includes(key)) errors.push('Missing security header: '+key);
  }
  if (/script-src[^\n]*'unsafe-eval'/.test(h)) warnings.push('CSP permits unsafe-eval: inventory dependencies and remove if possible');
  if (/script-src[^\n]*'unsafe-inline'/.test(h)) warnings.push('CSP permits inline scripts: plan nonce/hash-based policy');
}
if (exists('_redirects')) {
  const redirects = read('_redirects').split(/\r?\n/);
  const duplicate = new Set(), seen = new Set();
  for (const line of redirects) {
    const s = line.trim();
    if (!s || s.startsWith('#')) continue;
    const from = s.split(/\s+/)[0];
    if (seen.has(from)) duplicate.add(from);
    seen.add(from);
  }
  if (duplicate.size) warnings.push('Duplicate redirect sources: '+[...duplicate].join(', '));
}
function walk(dir) {
  if (!fs.existsSync(dir)) return;
  for (const entry of fs.readdirSync(dir,{withFileTypes:true})) {
    const full = path.join(dir,entry.name);
    if (entry.isDirectory()) walk(full);
    else if (entry.name.endsWith('.html')) {
      const html = fs.readFileSync(full,'utf8');
      const rel = path.relative(dist,full);
      if (!/<title\b[^>]*>[^<]+<\/title>/i.test(html)) warnings.push(rel+': missing title');
      if (!/<html\b[^>]*lang=/i.test(html)) warnings.push(rel+': missing lang');
      if (/<img\b(?![^>]*\balt=)[^>]*>/i.test(html)) warnings.push(rel+': image without alt attribute (review decorative images)');
    }
  }
}
walk(dist);
console.log('LX release audit: '+errors.length+' errors, '+warnings.length+' warnings');
for (const w of warnings) console.warn('WARN '+w);
for (const e of errors) console.error('FAIL '+e);
if (errors.length) process.exitCode = 1;
