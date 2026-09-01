const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const PAGES = fs.readdirSync(ROOT).filter(f => f.endsWith('.html'));

let updated = 0;
for (const page of PAGES) {
  const filePath = path.join(ROOT, page);
  let html = fs.readFileSync(filePath, 'utf8');
  const original = html;

  // Replace style.css with lx.css
  html = html.replace(/href="css\/style\.css"/g, 'href="css/lx.css"');

  // Remove any inline <style> blocks that duplicate lx.css (the base reset + saas styles)
  // Remove the old body/html reset inlines since lx.css handles it
  html = html.replace(/<style>\s*\*\{box-sizing:border-box\}html\{background:#030305;scroll-behavior:smooth\}body\{margin:0;background:#030305!important;color:#f4f4f7!important;font-family:Inter,ui-sans-serif,system-ui,sans-serif;overflow-x:hidden\}\s*<\/style>/g, '');
  html = html.replace(/<style>\s*\*\{box-sizing:border-box\}html\{background:#000;scroll-behavior:smooth\}body\{margin:0;background:#000;color:#e4e4e7;font-family:'Inter',system-ui,sans-serif;overflow-x:hidden;-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale\}[\s\S]*?<\/style>/g, '');

  if (html !== original) {
    fs.writeFileSync(filePath, html);
    console.log('UPDATED: ' + page);
    updated++;
  } else {
    console.log('SKIP: ' + page + ' (no style.css ref)');
  }
}
console.log('\nDone: ' + updated + ' updated');
