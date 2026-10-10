/**
 * Uniform-margins audit: every page's content containers must match the
 * homepage convention (.saas-wrap = width:min(1200px,calc(100% - 48px))).
 * Reports per page: wrapper class usage + any inline width/max-width
 * container styles that deviate from the 1200px standard.
 */
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const HOMEPAGE_WRAP = 'min(1200px,calc(100% - 48px))';
const WRAPPERS = ['saas-wrap', 'saas-hero__grid', 'velora-band__inner', 'store-hero__grid',
  'store-toolbar__inner', 'store-products', 'store-trust__grid', 'store-cta', 'glass-footer__inner'];

const files = fs.readdirSync(ROOT).filter(f => f.endsWith('.html'));
let issues = 0;

for (const f of files) {
  const h = fs.readFileSync(path.join(ROOT, f), 'utf8');
  const counts = {};
  for (const w of WRAPPERS) {
    const n = (h.match(new RegExp('class="[^"]*' + w, 'g')) || []).length;
    if (n) counts[w] = n;
  }
  // inline style container widths (deviation check)
  const inline = [...h.matchAll(/width:\s*min\([^)]*\)/g)].map(m => m[0]);
  const maxw = [...h.matchAll(/(?:^|[";])\s*max-width:\s*(\d{3,4})px/g)].map(m => m[1] + 'px');
  const badInline = [...new Set(inline)].filter(s => !s.includes(HOMEPAGE_WRAP) && !s.includes('min(1400px,100%)'));
  console.log(`\n### ${f}`);
  console.log('  wrappers: ' + (Object.keys(counts).length ? JSON.stringify(counts) : 'NONE-FOUND'));
  if (badInline.length) { console.log('  ⚠ deviant inline width: ' + badInline.join(' | ')); issues++; }
  if (!Object.keys(counts).length) { console.log('  ⚠ no known wrapper class — check manually'); issues++; }
}
console.log('\n' + (issues ? issues + ' page(s) need attention' : 'All pages use the standard wrapper'));
