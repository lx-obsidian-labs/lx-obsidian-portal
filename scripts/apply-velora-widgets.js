/**
 * Inserts the Velora ticker widgets and wires their scripts.
 *
 *   - Blurred Text Ticker band  -> index.html, services.html
 *   - Interactive Ticker Link   -> index.html, portfolio.html
 *
 * Idempotent: bands carry VELORA marker comments and are skipped if present.
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

const SCRIPTS = [
  '  <script src="js/ui/ticker-blur.js" defer></script>\n',
  '  <script src="js/ui/ticker-links.js" defer></script>\n',
];

const BLUR_BAND = `<!-- VELORA:TICKER-BLUR -->
<section class="velora-band" aria-labelledby="veloraStatement">
  <div class="saas-wrap velora-band__inner">
    <span class="saas-eyebrow">From idea to impact</span>
    <h2 class="velora-band__title" id="veloraStatement" style="position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap">We help teams automate, integrate, scale and ship</h2>
    <div class="ticker-blur" data-texts="Automate|Integrate|Scale|Ship|Grow" aria-hidden="true"></div>
    <p class="velora-band__sub">South African AI and software engineering &mdash; from first prototype to production systems.</p>
  </div>
</section>
`;

const linkCard = (href, title, desc, img) =>
  '        <a class="ticker-link" href="' + href + '"' +
  (img ? ' data-image="' + img + '"' : '') + '>' +
  '<span class="ticker-link__media">' +
  (img
    ? '<img src="' + img + '" alt="" loading="lazy">'
    : '<span style="font:700 .9rem/1 \'Space Grotesk\',sans-serif;color:var(--saas-dim);letter-spacing:.06em">' + title.replace(/[^A-Za-z ]/g, '').slice(0, 2).toUpperCase() + '</span>') +
  '</span>' +
  '<span class="ticker-link__title">' + title + '</span>' +
  '<span class="ticker-link__desc">' + desc + '</span>' +
  '<span class="ticker-link__cta">Explore &rarr;</span>' +
  '</a>';

const LINKS_BAND = `<!-- VELORA:TICKER-LINKS -->
<section class="velora-band velora-band--flush" aria-labelledby="veloraShowcase">
  <div class="saas-wrap">
    <span class="saas-eyebrow">Built by LX Obsidian</span>
    <h2 class="velora-band__title" id="veloraShowcase" style="margin:10px 0 0;font:500 clamp(1.6rem,2.6vw,2.2rem)/1.1 'Space Grotesk',sans-serif;letter-spacing:-.04em;color:#fff">Products &amp; platforms</h2>
  </div>
  <div class="ticker-links" data-speed="42">
    <div class="ticker-links__track">
${linkCard('ai.html', 'AI Platform', 'Unified inference across NVIDIA and OpenRouter models.', 'assets/hero-image.png')}
${linkCard('synapse.html', 'Synapse AI', 'Conversational intelligence for your workflows.', 'assets/synapse-screenshot-2.png')}
${linkCard('vista.html', 'Vista Cinema', 'Free movie & TV companion for Android.', 'assets/vista-marketing-1.png')}
${linkCard('marketplace.html', 'Marketplace', 'Ready-made tools, templates and integrations.')}
${linkCard('mobile-apps.html', 'Mobile Apps', 'Ship to the App Store and Play Store.')}
${linkCard('local-visibility.html', 'Local SEO', 'Get found by nearby customers.')}
${linkCard('integrations.html', 'Integrations', 'Connect the stack you already run.')}
    </div>
  </div>
</section>
`;

// page -> { script: bool, bands: [ {marker, html, before} ] }
const PLAN = {
  'index.html': [
    { marker: 'VELORA:TICKER-BLUR', html: BLUR_BAND, before: '<!-- ------- COMPANIES WE\'VE HELPED ------- -->' },
    { marker: 'VELORA:TICKER-LINKS', html: LINKS_BAND, before: '<!-- ------- FEATURED PRODUCTS ------- -->' },
  ],
  'services.html': [
    { marker: 'VELORA:TICKER-BLUR', html: BLUR_BAND, before: '<section class="saas-section" style="border-top:none;padding:80px 0 60px">' },
  ],
  'portfolio.html': [
    { marker: 'VELORA:TICKER-LINKS', html: LINKS_BAND, before: '<section class="saas-section" style="border-top:none;padding:80px 0 60px">' },
  ],
};

const ALL_PAGES = [
  'index.html', 'about.html', 'services.html', 'contact.html', 'portfolio.html',
  'blog.html', 'faq.html', 'synapse.html', 'vista.html', 'marketplace.html',
  'mobile-apps.html', 'local-visibility.html', 'integrations.html', 'help-us-grow.html',
];

let updated = 0;

// 1. Wire the ticker scripts on every page.
for (const page of ALL_PAGES) {
  const filePath = path.join(ROOT, page);
  if (!fs.existsSync(filePath)) continue;
  let html = fs.readFileSync(filePath, 'utf8');
  const original = html;
  for (const tag of SCRIPTS) {
    const src = tag.match(/src="([^"]+)"/)[1];
    if (!html.includes(src)) {
      html = html.replace(/<\/body>/i, tag + '</body>');
    }
  }
  if (html !== original) {
    fs.writeFileSync(filePath, html);
    console.log('SCRIPTS: ' + page);
    updated++;
  }
}

// 2. Insert the bands.
for (const [page, bands] of Object.entries(PLAN)) {
  const filePath = path.join(ROOT, page);
  if (!fs.existsSync(filePath)) {
    console.log('SKIP: ' + page + ' (not found)');
    continue;
  }
  let html = fs.readFileSync(filePath, 'utf8');
  const original = html;
  const notes = [];

  for (const band of bands) {
    if (html.includes('<!-- ' + band.marker + ' -->')) {
      notes.push(band.marker + ':present');
      continue;
    }
    if (!html.includes(band.before)) {
      notes.push(band.marker + ':ANCHOR-MISSING');
      continue;
    }
    html = html.replace(band.before, band.html + '\n' + band.before);
    notes.push(band.marker + ':inserted');
  }

  if (html !== original) {
    fs.writeFileSync(filePath, html);
    console.log('BANDS: ' + page + ' [' + notes.join(', ') + ']');
    updated++;
  } else {
    console.log('BANDS: ' + page + ' (no change) [' + notes.join(', ') + ']');
  }
}

console.log('\nDone: ' + updated + ' file writes');
