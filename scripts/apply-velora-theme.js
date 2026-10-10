/**
 * Applies the ported Framer components site-wide:
 *   - Velora navigation bar         (replaces .saas-nav)
 *   - Liquid Glass footer           (replaces .saas-footer)
 *   - Dot Grid BG + Scroll Reveal Text + glass footer scripts
 *
 * Safe to re-run: already-migrated pages are skipped.
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

const EXTRA_SCRIPTS =
  '  <script src="js/ui/dot-grid.js" defer></script>\n' +
  '  <script src="js/ui/scroll-reveal.js" defer></script>\n' +
  '  <script src="js/ui/glass-footer.js" defer></script>\n';

const ico = (d) =>
  '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" ' +
  'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + d + '</svg>';

const solid = (d) =>
  '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">' + d + '</svg>';

const ICONS = {
  cpu: ico('<rect x="4" y="4" width="16" height="16" rx="2"/><rect x="9" y="9" width="6" height="6"/><path d="M9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 14h3M1 9h3M1 14h3"/>'),
  node: ico('<circle cx="12" cy="12" r="3.5"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4M5 5l2.5 2.5M16.5 16.5L19 19M19 5l-2.5 2.5M7.5 16.5L5 19"/>'),
  film: ico('<rect x="2" y="2" width="20" height="20" rx="2"/><path d="M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 17h5M17 7h5"/>'),
  store: ico('<path d="M3 9l1.5-5h15L21 9"/><path d="M4 9v10a1 1 0 001 1h14a1 1 0 001-1V9"/><path d="M9 20v-5h6v5"/>'),
  briefcase: ico('<rect x="2" y="7" width="20" height="14" rx="2"/><path d="M8 7V5a2 2 0 012-2h4a2 2 0 012 2v2"/><path d="M2 13h20"/>'),
  plug: ico('<path d="M9 2v6M15 2v6"/><path d="M6 8h12v3a6 6 0 01-12 0V8z"/><path d="M12 17v5"/>'),
  map: ico('<path d="M12 22s7-6.5 7-12a7 7 0 10-14 0c0 5.5 7 12 7 12z"/><circle cx="12" cy="10" r="2.5"/>'),
  phone: ico('<rect x="6" y="2" width="12" height="20" rx="2"/><path d="M11 18h2"/>'),
  grid: ico('<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>'),
  help: ico('<circle cx="12" cy="12" r="10"/><path d="M9.1 9a3 3 0 015.8 1c0 2-3 3-3 3"/><path d="M12 17h.01"/>'),
  users: ico('<path d="M17 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2"/><circle cx="9.5" cy="7" r="4"/><path d="M22 21v-2a4 4 0 00-3-3.9"/><path d="M16 3.1a4 4 0 010 7.8"/>'),
  file: ico('<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><path d="M14 2v6h6"/><path d="M8 13h8M8 17h5"/>'),
  heart: ico('<path d="M20.8 4.6a5.5 5.5 0 00-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 00-7.8 7.8l1 1.1L12 21.2l7.8-7.7 1-1.1a5.5 5.5 0 000-7.8z"/>'),
  mail: ico('<rect x="2" y="4" width="20" height="16" rx="2"/><path d="M2 6l10 7 10-7"/>'),
  external: ico('<path d="M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6"/><path d="M15 3h6v6"/><path d="M10 14L21 3"/>'),
  chevron: '<svg class="velora-chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 9l6 6 6-6"/></svg>',
  github: solid('<path d="M12 .5C5.7.5.5 5.7.5 12c0 5.1 3.3 9.4 7.9 10.9.6.1.8-.3.8-.6v-2c-3.2.7-3.9-1.5-3.9-1.5-.5-1.3-1.3-1.7-1.3-1.7-1-.7.1-.7.1-.7 1.2.1 1.8 1.2 1.8 1.2 1 1.8 2.7 1.3 3.4 1 .1-.8.4-1.3.7-1.6-2.6-.3-5.3-1.3-5.3-5.8 0-1.3.5-2.3 1.2-3.1-.1-.3-.5-1.5.1-3.1 0 0 1-.3 3.3 1.2a11.5 11.5 0 016 0C17.7 4.6 18.7 5 18.7 5c.6 1.6.2 2.8.1 3.1.8.8 1.2 1.8 1.2 3.1 0 4.5-2.7 5.5-5.3 5.8.4.4.8 1.1.8 2.2v3.3c0 .3.2.7.8.6 4.6-1.5 7.9-5.8 7.9-10.9C23.5 5.7 18.3.5 12 .5z"/>'),
  linkedin: solid('<path d="M20.45 20.45h-3.56v-5.57c0-1.33-.02-3.04-1.85-3.04-1.85 0-2.13 1.45-2.13 2.94v5.67H9.35V9h3.42v1.56h.05c.48-.9 1.64-1.85 3.37-1.85 3.6 0 4.27 2.37 4.27 5.46v6.28zM5.34 7.43a2.06 2.06 0 110-4.13 2.06 2.06 0 010 4.13zM7.12 20.45H3.55V9h3.57v11.45zM22.22 0H1.77C.79 0 0 .77 0 1.72v20.56C0 23.23.79 24 1.77 24h20.45c.98 0 1.78-.77 1.78-1.72V1.72C24 .77 23.2 0 22.22 0z"/>'),
  x: solid('<path d="M18.24 2.25h3.31l-7.23 8.26 8.5 11.24h-6.65l-5.21-6.82-5.96 6.82H1.69l7.73-8.84L1.25 2.25h6.82l4.71 6.23 5.46-6.23zm-1.16 17.52h1.83L7.01 4.13H5.04l12.04 15.64z"/>'),
  youtube: solid('<path d="M23.5 6.2a3.02 3.02 0 00-2.12-2.14C19.5 3.55 12 3.55 12 3.55s-7.5 0-9.38.51A3.02 3.02 0 00.5 6.2 31.6 31.6 0 000 12a31.6 31.6 0 00.5 5.8 3.02 3.02 0 002.12 2.14c1.88.51 9.38.51 9.38.51s7.5 0 9.38-.51a3.02 3.02 0 002.12-2.14A31.6 31.6 0 0024 12a31.6 31.6 0 00-.5-5.8zM9.6 15.6V8.4l6.24 3.6-6.24 3.6z"/>'),
};

const card = (icon, title, desc, href, external) =>
  '        <a class="velora-card" href="' + href + '"' +
  (external ? ' target="_blank" rel="noopener"' : '') + '>' +
  '<span class="velora-card__icon">' + ICONS[icon] + '</span>' +
  '<span class="velora-card__body">' +
  '<span class="velora-card__title">' + title + '</span>' +
  '<span class="velora-card__desc">' + desc + '</span>' +
  '</span></a>';

const col = (head, cards) =>
  '      <div class="velora-menu__col">\n' +
  '        <p class="velora-menu__head">' + head + '</p>\n' +
  cards.join('\n') + '\n' +
  '      </div>';

const dropdown = (label, columns) => {
  const id = 'velora-menu-' + label.toLowerCase();
  return (
    '    <div class="velora-item">\n' +
    '      <button type="button" class="velora-link has-chevron" aria-expanded="false" ' +
    'aria-controls="' + id + '" data-velora-menu>' + label + ICONS.chevron + '</button>\n' +
    '      <div class="velora-menu" id="' + id + '" role="group" aria-label="' + label + '">\n' +
    columns.join('\n') + '\n' +
    '      </div>\n' +
    '    </div>'
  );
};

const VELORA_NAV =
`<header class="velora-nav" id="veloraNav">
  <div class="velora-nav__inner">
    <a class="velora-brand" href="index.html">
      <span class="velora-brand__mark">LX</span>
      <span class="velora-brand__label">Obsidian Labs</span>
    </a>

    <nav class="velora-links" id="veloraLinks" aria-label="Primary">
${dropdown('Product', [
  col('Platform', [
    card('cpu', 'AI Platform', 'Autonomous AI systems', 'ai.html'),
    card('node', 'Synapse AI', 'Conversational intelligence', 'synapse.html'),
  ]),
  col('Apps', [
    card('film', 'Vista Cinema', 'Free movie &amp; TV app', 'vista.html'),
    card('store', 'Marketplace', 'Tools &amp; templates', 'marketplace.html'),
  ]),
  col('Start', [
    card('heart', 'Help Us Grow', 'Back the mission', 'help-us-grow.html'),
    card('mail', 'Contact', 'Talk to our team', 'contact.html'),
  ]),
])}
${dropdown('Solutions', [
  col('Services', [
    card('briefcase', 'Solutions', 'What we build', 'services.html'),
    card('plug', 'Integrations', 'Connect your stack', 'integrations.html'),
  ]),
  col('Growth', [
    card('map', 'Local SEO', 'Get found locally', 'local-visibility.html'),
    card('phone', 'Mobile Apps', 'Ship to app stores', 'mobile-apps.html'),
  ]),
  col('Proof', [
    card('grid', 'Work', 'Selected projects', 'portfolio.html'),
    card('help', 'FAQ', 'Quick answers', 'faq.html'),
  ]),
])}
${dropdown('Company', [
  col('Company', [
    card('users', 'About', 'Our story and mission', 'about.html'),
    card('file', 'Insights', 'Ideas and updates', 'blog.html'),
  ]),
  col('Support', [
    card('heart', 'Help Us Grow', 'Back the mission', 'help-us-grow.html'),
    card('help', 'FAQ', 'Quick answers', 'faq.html'),
  ]),
  col('Connect', [
    card('store', 'Marketplace', 'Tools &amp; templates', 'marketplace.html'),
    card('external', 'GitHub', 'Open source', 'https://github.com/lx-obsidian-labs', true),
  ]),
])}
      <a class="velora-link" href="blog.html">Insights</a>
    </nav>

    <div class="velora-actions">
      <a class="velora-link" href="contact.html">Login</a>
      <a class="velora-cta" href="contact.html"><span class="velora-cta__pill">Get Started</span></a>
      <button type="button" class="velora-burger" id="veloraBurger" aria-label="Open menu" aria-expanded="false" aria-controls="veloraLinks"><span></span><span></span></button>
    </div>
  </div>
</header>`;

const footCol = (title, links) =>
  '      <div class="glass-col">\n' +
  '        <p class="glass-col__title">' + title + '</p>\n' +
  links.map((l) =>
    '        <a class="glass-link" href="' + l[0] + '">' + l[1] + '</a>'
  ).join('\n') + '\n' +
  '      </div>';

const social = (label, href, icon) =>
  '        <a href="' + href + '" target="_blank" rel="noopener" aria-label="' + label + '">' +
  ICONS[icon] + '</a>';

const GLASS_FOOTER =
`<footer class="glass-footer">
  <div class="glass-footer__inner">
    <div class="glass-footer__top">
      <div class="glass-footer__brand">
        <a class="glass-brand" href="index.html">
          <span class="glass-brand__mark">LX</span>
          <span class="glass-brand__name">Obsidian Labs</span>
        </a>
        <p class="glass-footer__desc">South African AI and software engineering company building intelligent platforms, automation systems and digital infrastructure.</p>
      </div>
      <div class="glass-footer__news">
        <form class="glass-news" id="glassNews" data-endpoint="/api/subscribe" novalidate>
          <span class="glass-news__field">
            <input class="glass-news__input" type="email" name="email" placeholder="Your email" aria-label="Email address" required>
          </span>
          <button class="glass-news__btn" type="submit">Subscribe</button>
        </form>
        <p class="glass-news__note" id="glassNewsNote" hidden></p>
      </div>
    </div>
    <div class="glass-footer__cols">
${footCol('Platform', [
  ['ai.html', 'AI Platform'],
  ['synapse.html', 'Synapse AI'],
  ['vista.html', 'Vista Cinema'],
  ['marketplace.html', 'Marketplace'],
])}
${footCol('Company', [
  ['about.html', 'About'],
  ['portfolio.html', 'Work'],
  ['blog.html', 'Insights'],
  ['help-us-grow.html', 'Help Us Grow'],
])}
${footCol('Resources', [
  ['services.html', 'Solutions'],
  ['mobile-apps.html', 'Mobile Apps'],
  ['local-visibility.html', 'Local SEO'],
  ['integrations.html', 'Integrations'],
  ['faq.html', 'FAQ'],
])}
    </div>
    <div class="glass-footer__divider"></div>
    <div class="glass-footer__bottom">
      <p class="glass-footer__copy">&copy; 2026 LX Obsidian Labs. All rights reserved.</p>
      <div class="glass-social">
${social('GitHub', 'https://github.com/lx-obsidian-labs', 'github')}
${social('LinkedIn', 'https://www.linkedin.com', 'linkedin')}
${social('X', 'https://x.com', 'x')}
${social('YouTube', 'https://www.youtube.com', 'youtube')}
      </div>
    </div>
  </div>
</footer>`;

const PAGES = [
  'index.html', 'about.html', 'services.html', 'contact.html', 'portfolio.html',
  'blog.html', 'faq.html', 'synapse.html', 'vista.html', 'marketplace.html',
  'mobile-apps.html', 'local-visibility.html', 'integrations.html', 'help-us-grow.html',
];

const FORCE = process.argv.includes('--force');

let updated = 0;
let skipped = 0;

for (const page of PAGES) {
  const filePath = path.join(ROOT, page);
  if (!fs.existsSync(filePath)) {
    console.log('SKIP: ' + page + ' (not found)');
    skipped++;
    continue;
  }

  let html = fs.readFileSync(filePath, 'utf8');
  const original = html;
  const notes = [];

  if (FORCE && html.includes('velora-nav')) {
    html = html.replace(/<header class="velora-nav" id="veloraNav">[\s\S]*?<\/header>/, VELORA_NAV);
    notes.push('nav(re-gen)');
  } else if (!html.includes('velora-nav')) {
    if (/<nav class="saas-nav">[\s\S]*?<\/nav>/.test(html)) {
      html = html.replace(/<nav class="saas-nav">[\s\S]*?<\/nav>/, VELORA_NAV);
      notes.push('nav');
    } else {
      notes.push('NO-NAV');
    }
  }

  if (FORCE && html.includes('glass-footer')) {
    html = html.replace(/<footer class="glass-footer">[\s\S]*?<\/footer>/, GLASS_FOOTER);
    notes.push('footer(re-gen)');
  } else if (!html.includes('glass-footer')) {
    if (/<footer class="saas-footer">[\s\S]*?<\/footer>/.test(html)) {
      html = html.replace(/<footer class="saas-footer">[\s\S]*?<\/footer>/, GLASS_FOOTER);
      notes.push('footer');
    } else {
      notes.push('NO-FOOTER');
    }
  }

  if (!html.includes('js/ui/dot-grid.js')) {
    html = html.replace(/<\/body>/i, EXTRA_SCRIPTS + '</body>');
    notes.push('scripts');
  }

  if (html !== original) {
    fs.writeFileSync(filePath, html);
    console.log('UPDATED: ' + page + ' [' + notes.join(', ') + ']');
    updated++;
  } else {
    console.log('SKIP: ' + page + ' (no changes needed)');
    skipped++;
  }
}

console.log('\nDone: ' + updated + ' updated, ' + skipped + ' skipped');
