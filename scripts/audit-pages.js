// Structural uniformity audit across all HTML pages
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const pages = fs.readdirSync(root).filter(f => f.endsWith('.html'));

function grab(src, re) { const m = src.match(re); return m ? m[1] : null; }

const rows = pages.map(file => {
  const src = fs.readFileSync(path.join(root, file), 'utf8');
  const scripts = [...src.matchAll(/<script[^>]*src="([^"]+)"/g)].map(m => m[1]);
  return {
    file,
    lines: src.split('\n').length,
    title: grab(src, /<title>([^<]*)<\/title>/),
    desc: grab(src, /<meta name="description" content="([^"]*)"/),
    canonical: grab(src, /<link rel="canonical" href="([^"]*)"/),
    ogTitle: /property="og:title"/.test(src),
    ogImage: /property="og:image"/.test(src),
    twitter: /name="twitter:card"/.test(src),
    jsonld: (src.match(/application\/ld\+json/g) || []).length,
    themeColor: grab(src, /<meta name="theme-color" content="([^"]*)"/),
    colorScheme: /name="color-scheme"/.test(src),
    manifest: /rel="manifest"/.test(src),
    favicon: /rel="icon"/.test(src),
    skip: /skip-link/.test(src),
    nav: /velora-nav/.test(src),
    burger: /velora-burger/.test(src),
    footer: /glass-footer/.test(src),
    newsForm: /glassNews/.test(src),
    main: /<main id="main">/.test(src),
    h1: (src.match(/<h1[\s>]/g) || []).length,
    css: [...src.matchAll(/<link rel="stylesheet" href="([^"]+)"/g)].map(m => m[1]).join(','),
    fonts: grab(src, /family=([^&"]+)/g) ? [...src.matchAll(/family=([^\&"]+)/g)].map(m => m[1]).join(';') : '',
    scripts,
    chat: scripts.some(s => /chat-widget/.test(s)),
    dotgrid: scripts.some(s => /dot-grid/.test(s)),
    reveal: scripts.some(s => /scroll-reveal/.test(s)),
    glassFooterJs: scripts.some(s => /glass-footer/.test(s)),
    ticker: scripts.some(s => /ticker/.test(s)),
    mobilenav: scripts.some(s => /mobile-nav/.test(s)),
    sw: /serviceWorker/.test(src),
    inlineStyleCount: (src.match(/ style="/g) || []).length,
  };
});

// Print comparison table
const ref = rows.find(r => r.file === 'index.html');
const keys = ['title','desc','canonical','ogTitle','ogImage','twitter','jsonld','themeColor','colorScheme','manifest','favicon','skip','nav','burger','footer','newsForm','main','h1','css','fonts','chat','dotgrid','reveal','glassFooterJs','ticker','mobilenav','sw'];

console.log('=== PER-PAGE FLAGS (differs from index.html marked with *) ===');
for (const r of rows) {
  const diffs = [];
  for (const k of keys) {
    const rv = ref ? ref[k] : null;
    if (JSON.stringify(r[k]) !== JSON.stringify(rv)) diffs.push(`${k}=${JSON.stringify(r[k])}`);
  }
  console.log(`\n${r.file} (${r.lines} lines) inlineStyles=${r.inlineStyleCount}`);
  console.log('   ' + (diffs.length ? diffs.join('\n   ') : 'OK identical flags'));
}

console.log('\n=== SCRIPT INVENTORY (index.html) ===');
if (ref) ref.scripts.forEach(s => console.log('  ' + s));

console.log('\n=== PER-PAGE SCRIPTS ===');
for (const r of rows) console.log(`${r.file}: ${r.scripts.join(' | ') || '(none)'}`);

// Link integrity
console.log('\n=== BROKEN INTERNAL LINKS ===');
const files = new Set(pages.map(p => p.toLowerCase()));
const assets = new Set();
function walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name.startsWith('.') || e.name === 'node_modules') continue;
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else assets.add(path.relative(root, p).replace(/\\/g, '/').toLowerCase());
  }
}
walk(root);
for (const r of rows) {
  const src = fs.readFileSync(path.join(root, r.file), 'utf8');
  const hrefs = [...src.matchAll(/(?:href|src)="([^"#][^"]*)"/g)].map(m => m[1]);
  const broken = [];
  for (const h of hrefs) {
    if (/^(https?:|mailto:|tel:|data:|javascript:|#|\/\/)/.test(h)) continue;
    const clean = h.split('?')[0].split('#')[0];
    if (!clean) continue;
    const target = clean.replace(/^\//, '').toLowerCase();
    if (!assets.has(target)) broken.push(h);
  }
  if (broken.length) console.log(`${r.file}: ${[...new Set(broken)].join(', ')}`);
}
