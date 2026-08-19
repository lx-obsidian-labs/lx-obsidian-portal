const fs = require('fs');
const path = require('path');

const PARTS = [
  'hp_head.html',
  'hp_body.html',
  'hp_foot.html',
];

const ROOT = path.resolve(__dirname, '..');
let combined = '';
for (const part of PARTS) {
  const p = path.join(ROOT, 'temp', part);
  if (fs.existsSync(p)) {
    combined += fs.readFileSync(p, 'utf8');
  }
}

// Apply fixes
combined = combined.replace(/href="\/deploy\/"/g, 'href="/vista"');
combined = combined.replace(
  /<a href="#capabilities">Capabilities<\/a>\s*<a href="#products">Products<\/a>/,
  '<a href="#capabilities">Capabilities</a>\n        <a href="/marketplace">Marketplace</a>\n        <a href="#products">Products</a>'
);

const outPath = path.join(ROOT, 'index.html');
fs.writeFileSync(outPath, combined, 'utf8');
console.log('Homepage written:', outPath, combined.length, 'chars');
