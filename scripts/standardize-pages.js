/**
 * One-off standardization pass across all site pages.
 * Makes <head>, skip-links, fonts, theme-color and JSON-LD uniform.
 * Idempotent: safe to re-run (skips anything already in place).
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

const PAGES = [
  'index.html', '404.html', 'about.html', 'blog.html', 'contact.html',
  'faq.html', 'help-us-grow.html', 'integrations.html', 'local-visibility.html',
  'marketplace.html', 'mobile-apps.html', 'portfolio.html', 'services.html',
  'synapse.html', 'vista.html',
];

const BASE = 'https://www.lxobsidianportal.co.za';

const ORG_JSONLD = `<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "Organization",
  "name": "LX Obsidian Labs",
  "url": "https://www.lxobsidianportal.co.za",
  "logo": "https://www.lxobsidianportal.co.za/assets/og-default.svg",
  "description": "South African AI and software engineering company building intelligent platforms, automation systems and digital infrastructure.",
  "sameAs": ["https://github.com/lx-obsidian-labs"],
  "contactPoint": {
    "@type": "ContactPoint",
    "email": "hello@lxobsidian.com",
    "contactType": "customer service"
  }
}
</script>
`;

let changed = 0;

for (const page of PAGES) {
  const filePath = path.join(ROOT, page);
  if (!fs.existsSync(filePath)) { console.log('MISSING: ' + page); continue; }
  let html = fs.readFileSync(filePath, 'utf8');
  const orig = html;
  const notes = [];

  // 1. theme-color → true site black (Velora theme)
  if (html.includes('<meta name="theme-color" content="#030305">')) {
    html = html.replace(/<meta name="theme-color" content="#030305">/g,
      '<meta name="theme-color" content="#000000">');
    notes.push('theme-color');
  }

  // 2. color-scheme after theme-color (matches index.html)
  if (!/<meta name="color-scheme" content="dark">/.test(html)) {
    html = html.replace(/(<meta name="theme-color" content="#000000">)/,
      '$1\n<meta name="color-scheme" content="dark">');
    notes.push('color-scheme');
  }

  // 3. manifest link after color-scheme
  if (!/<link rel="manifest" href="manifest\.json">/.test(html)) {
    html = html.replace(/(<meta name="color-scheme" content="dark">)/,
      '$1\n<link rel="manifest" href="manifest.json">');
    notes.push('manifest');
  }

  // 4. Uniform font weights: Inter 400–800 + Space Grotesk 500–700
  const fontRe = /family=Inter:wght@400;500;600;700&family=Space\+Grotesk:wght@500;600;700/;
  if (fontRe.test(html)) {
    html = html.replace(fontRe,
      'family=Inter:wght@400;500;600;700;800&family=Space+Grotesk:wght@500;600;700');
    notes.push('fonts+800');
  }

  // 5. Title separator: " - " → em dash (uniform " — LX Obsidian Labs")
  if (/[^-] - LX Obsidian Labs<\/title>/.test(html)) {
    html = html.replace(/ - LX Obsidian Labs<\/title>/g, ' — LX Obsidian Labs</title>');
    notes.push('title-dash');
  }

  // 6. Standardize skip link (class + lx.css styles, no inline JS)
  //    a) classed variant with inline style/onfocus/onblur (index + inner pages)
  html = html.replace(
    /<a href="#main" class="skip-link"[^>]*onfocus="[^"]*"onblur="[^"]*"[^>]*>Skip to content<\/a>/,
    '<a href="#main" class="skip-link">Skip to content</a>');
  html = html.replace(
    /<a href="#main" class="skip-link" style="[^"]*"[^>]*>Skip to content<\/a>/,
    '<a href="#main" class="skip-link">Skip to content</a>');
  //    b) marketplace classless variant
  html = html.replace(
    /<a href="#main-content" style="[^"]*"onfocus="[^"]*"onblur="[^"]*"[^>]*>Skip to main content<\/a>/,
    '<a href="#main" class="skip-link">Skip to content</a>');
  if (html.includes('class="skip-link">Skip to content</a>')) notes.push('skip-link');

  // 7. Add Organization JSON-LD when the page has none
  const hasJsonLd = /application\/ld\+json/.test(html);
  if (!hasJsonLd && page !== '404.html') {
    html = html.replace(/<\/head>/i, ORG_JSONLD + '</head>');
    notes.push('jsonld');
  }

  if (html !== orig) {
    fs.writeFileSync(filePath, html);
    changed++;
    console.log('UPDATED: ' + page + ' [' + notes.join(', ') + ']');
  } else {
    console.log('ok:      ' + page);
  }
}

console.log('\n' + changed + ' pages updated');
