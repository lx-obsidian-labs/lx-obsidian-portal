const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

const SAAS_NAV = `  <nav class="saas-nav"><div class="saas-nav__inner">
    <a class="saas-nav__brand" href="index.html"><span class="saas-nav__mark">LX</span><span class="saas-nav__label">Obsidian Labs</span></a>
    <div class="saas-nav__links" id="navLinks">
      <a href="ai.html">AI Platform</a>
      <a href="services.html">Solutions</a>
      <a href="marketplace.html">Marketplace</a>
      <a href="portfolio.html">Work</a>
      <a href="about.html">Company</a>
    </div>
    <div class="saas-nav__actions">
      <a class="saas-btn saas-btn--ghost" href="contact.html">Login</a>
      <a class="saas-btn saas-btn--primary" href="contact.html">Start Free</a>
      <button class="saas-nav__menu" id="saasMenu" aria-label="Open menu" aria-expanded="false">☰</button>
    </div>
  </div></nav>`;

const SAAS_FOOTER = `  <footer class="saas-footer"><div class="saas-footer__grid">
    <div><a class="saas-nav__brand" href="index.html"><span class="saas-nav__mark">LX</span><span class="saas-nav__label">OBSIDIAN LABS</span></a><p>South African AI and software engineering company building intelligent platforms, automation systems and digital infrastructure.</p></div>
    <div><h4>Platform</h4><a href="ai.html">AI Platform</a><a href="services.html">Solutions</a><a href="marketplace.html">Marketplace</a></div>
    <div><h4>Products</h4><a href="synapse.html">Synapse AI</a><a href="vista.html">Vista Cinema</a><a href="portfolio.html">Work</a></div>
    <div><h4>Company</h4><a href="about.html">About</a><a href="blog.html">Insights</a><a href="contact.html">Contact</a></div>
    <div><h4>Developers</h4><a href="ai.html">AI Platform</a><a href="https://github.com/lx-obsidian-labs" target="_blank" rel="noopener">GitHub</a></div>
  </div><div class="saas-footer__bottom"><span>&copy; 2026 LX Obsidian Labs. All rights reserved.</span><span>AI · Automation · Intelligence</span></div></footer>`;

const SAAS_SCRIPTS = `  <script src="js/chat-widget.js"></script>
  <script>
    document.getElementById('saasMenu')?.addEventListener('click', function () {
      var links = document.getElementById('navLinks');
      var open = links.classList.toggle('open');
      this.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  </script>
  <script src="js/navigation.js" defer></script>
  <script src="js/app.js" defer></script>
  <script src="js/seo.js" defer></script>
  <script>if('serviceWorker' in navigator){window.addEventListener('load',()=>navigator.serviceWorker.register('/sw.js').catch(()=>{}));}</script>`;

// Pages to update (exclude index.html and marketplace.html which are already done)
const PAGES = [
  'about.html',
  'services.html',
  'contact.html',
  'portfolio.html',
  'blog.html',
  'faq.html',
  'synapse.html',
  'vista.html',
  '404.html'
];

// Nav patterns to match (various old nav structures)
const NAV_PATTERNS = [
  // Pattern 1: nav with nav--solid and complex dropdowns
  /<nav class="nav nav--solid"[^>]*>[\s\S]*?<\/nav>\s*(?:<\/div>\s*)?/,
  // Pattern 2: nav with just class="nav"
  /<nav class="nav"[^>]*>[\s\S]*?<\/nav>\s*(?:<\/div>\s*)?/,
  // Pattern 3: saas-nav (already updated)
  /<nav class="saas-nav">[\s\S]*?<\/nav>/
];

// Footer patterns
const FOOTER_PATTERNS = [
  /<footer class="footer">[\s\S]*?<\/footer>/,
  /<footer class="saas-footer">[\s\S]*?<\/footer>/
];

// Script patterns to replace
const SCRIPT_PATTERNS = [
  /<script src="js\/navigation\.js"[^>]*><\/script>\s*(?:<script src="js\/app\.js"[^>]*><\/script>\s*)?(?:<script src="js\/seo\.js"[^>]*><\/script>\s*)?(?:<script>[\s\S]*?<\/script>\s*)?/,
  /<script src="js\/chat-widget\.js"><\/script>\s*<script>[\s\S]*?<\/script>\s*<script src="js\/navigation\.js"[^>]*><\/script>\s*(?:<script src="js\/app\.js"[^>]*><\/script>\s*)?(?:<script src="js\/seo\.js"[^>]*><\/script>\s*)?(?:<script>[\s\S]*?<\/script>\s*)?/
];

let updated = 0;
let skipped = 0;

for (const page of PAGES) {
  const filePath = path.join(ROOT, page);
  if (!fs.existsSync(filePath)) {
    console.log(`SKIP: ${page} (not found)`);
    skipped++;
    continue;
  }

  let html = fs.readFileSync(filePath, 'utf8');
  const original = html;

  // Replace nav
  let navReplaced = false;
  for (const pattern of NAV_PATTERNS) {
    if (pattern.test(html)) {
      html = html.replace(pattern, SAAS_NAV + '\n');
      navReplaced = true;
      break;
    }
  }

  // Replace footer
  for (const pattern of FOOTER_PATTERNS) {
    if (pattern.test(html)) {
      html = html.replace(pattern, SAAS_FOOTER);
      break;
    }
  }

  // Replace scripts before </body>
  // Remove old script blocks and add new ones
  html = html.replace(/<script src="js\/navigation\.js"[^>]*><\/script>/g, '');
  html = html.replace(/<script src="js\/app\.js"[^>]*><\/script>/g, '');
  html = html.replace(/<script src="js\/seo\.js"[^>]*><\/script>/g, '');
  html = html.replace(/<script src="js\/chat-widget\.js"><\/script>/g, '');
  html = html.replace(/<script>if\('serviceWorker'[\s\S]*?<\/script>/g, '');
  
  // Remove old inline nav toggle scripts
  html = html.replace(/<script>\s*document\.getElementById\('navToggle'\)[\s\S]*?<\/script>/g, '');
  html = html.replace(/<script>\s*document\.getElementById\('announcementClose'\)[\s\S]*?<\/script>/g, '');

  // Add new scripts before </body>
  if (!html.includes('chat-widget.js')) {
    html = html.replace(/<\/body>/i, SAAS_SCRIPTS + '\n</body>');
  }

  // Add SaaS theme body style if missing
  if (!html.includes('saas-nav')) {
    // Add base styles for SaaS theme
    html = html.replace(/<style>/, '<style>\n    *{box-sizing:border-box}html{background:#030305;scroll-behavior:smooth}body{margin:0;background:#030305!important;color:#f4f4f7!important;font-family:Inter,ui-sans-serif,system-ui,sans-serif;overflow-x:hidden}\n  </style>\n  <style>');
  }

  // Add chat widget if not present
  if (!html.includes('chat-widget')) {
    html = html.replace(/<\/body>/i, '  <script src="js/chat-widget.js"></script>\n</body>');
  }

  if (html !== original) {
    fs.writeFileSync(filePath, html);
    console.log(`UPDATED: ${page}`);
    updated++;
  } else {
    console.log(`SKIP: ${page} (no changes needed)`);
    skipped++;
  }
}

console.log(`\nDone: ${updated} updated, ${skipped} skipped`);
