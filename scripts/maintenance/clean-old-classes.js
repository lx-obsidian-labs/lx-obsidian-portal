const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const PAGES = fs.readdirSync(ROOT).filter(f => f.endsWith('.html'));

let updated = 0;
for (const page of PAGES) {
  const filePath = path.join(ROOT, page);
  let html = fs.readFileSync(filePath, 'utf8');
  const original = html;

  // Remove old theme-refresh class from html tag
  html = html.replace(/<html([^>]*) class="theme-refresh"/g, '<html$1');

  // Remove old page-* body classes that conflict with SaaS design
  html = html.replace(/<body class="page-[^"]*">/g, '<body>');

  // Remove old loading bar elements
  html = html.replace(/<div class="loading-bar"[^>]*>[\s\S]*?<\/div>\s*/g, '');

  // Remove old announcement bar elements
  html = html.replace(/<div class="announcement"[^>]*>[\s\S]*?<\/div>\s*/g, '');

  // Remove old skip-link elements
  html = html.replace(/<a[^>]*class="skip-link"[^>]*>[^<]*<\/a>\s*/g, '');

  // Remove old search modal if present
  html = html.replace(/<div class="search-modal"[^>]*>[\s\S]*?<\/div>\s*<\/div>\s*/g, '');

  if (html !== original) {
    fs.writeFileSync(filePath, html);
    console.log('CLEANED: ' + page);
    updated++;
  }
}
console.log('\nCleaned: ' + updated + ' pages');
