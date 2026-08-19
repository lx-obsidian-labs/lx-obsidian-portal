const fs = require('fs');
const pages = ['about.html', 'advertise.html', 'blog.html', 'contact.html', 'faq.html', 'industries.html', 'marketplace.html', 'portfolio.html', 'services.html', 'synapse.html', 'vista.html'];
const required = ['color-scheme', '--violet', '--violet-2', '--blue', 'starfield', 'cursorGlow', 'loading-bar__fill'];
let allMatch = true;
for (const p of pages) {
  const h = fs.readFileSync('dist/' + p, 'utf8');
  const missing = required.filter(c => {
    if (c === 'loading-bar__fill') return !h.includes('loading-bar__fill');
    if (c === 'starfield') return !h.includes('id="starfield"');
    if (c === 'cursorGlow') return !h.includes('id="cursorGlow"');
    return !h.includes(c);
  });
  if (missing.length > 0) {
    console.log(p + ': MISSING ' + missing.join(', '));
    allMatch = false;
  }
}
if (allMatch) console.log('ALL PAGES HAVE ALL REQUIRED ELEMENTS');