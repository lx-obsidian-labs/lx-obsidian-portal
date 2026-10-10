// Gives 404.html the same site chrome as every other page:
// Velora nav header + Liquid Glass footer (copied verbatim from index.html).
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const index = fs.readFileSync(path.join(ROOT, 'index.html'), 'utf8');
const p = path.join(ROOT, '404.html');
let html = fs.readFileSync(p, 'utf8');

const navM = index.match(/<header class="velora-nav"[\s\S]*?<\/header>/);
const footM = index.match(/<footer class="glass-footer">[\s\S]*?<\/footer>/);
if (!navM || !footM) { console.error('Could not extract nav/footer from index.html'); process.exit(1); }
if (html.includes('velora-nav')) { console.log('404.html already has nav — skipping'); process.exit(0); }

// 1. Nav after the skip link (so skip link stays first in DOM)
html = html.replace(/(<a href="#main" class="skip-link">Skip to content<\/a>)/,
  '$1\n\n  <!-- ------- NAV ------- -->\n  ' + navM[0]);

// 2. Footer before the scripts; shrink the full-viewport main so the footer is reachable
html = html.replace(/(\n  <script src="js\/components\/chat-widget\.js\?v=2">)/,
  '\n  <!-- ------- FOOTER ------- -->\n  ' + footM[0] + '\n' + '$1');

html = html.replace('style="min-height:100vh;display:grid;place-items:center;padding:24px;text-align:center"',
  'style="min-height:clamp(420px,68vh,720px);display:grid;place-items:center;padding:140px 24px 64px;text-align:center"');

// 3. Scripts the nav + footer need
html = html.replace(/(<\/body>)/i,
  '  <script src="js/ui/mobile-nav.js"></script>\n' +
  '  <script src="js/ui/glass-footer.js" defer></script>\n$1');

fs.writeFileSync(p, html);
console.log('404.html: nav + footer + scripts injected');
