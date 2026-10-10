/**
 * Rebuilds ai.html from the Lx-Obsidian-Pro artifact:
 *   - proper LX site <head> (meta, OG, canonical, JSON-LD, fonts, lx.css)
 *   - slim liquid-glass site header (h1 = AI Platform, back-to-site + CTA)
 *   - monochrome re-skin override layer (purple accents → white/black)
 *   - app viewport sized below the fixed header
 *   - single h1 (artifact's welcome heading demoted to h2)
 * Idempotent: regenerates ai.html from the source artifact each run.
 */
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

// The source artifact (Lx-Obsidian-Pro.html) was removed from the working tree
// after ai.html was generated from it. ai.html itself is now the canonical,
// maintained file — edit it directly. This script only regenerates ai.html if
// the artifact is restored (e.g. via: git checkout HEAD -- Lx-Obsidian-Pro.html).
const ARTIFACT = path.join(ROOT, 'Lx-Obsidian-Pro.html');
if (!fs.existsSync(ARTIFACT)) {
  console.error(
    'Lx-Obsidian-Pro.html not found.\n' +
    'ai.html is already generated and canonical — no rebuild is needed.\n' +
    'To regenerate from the artifact, restore it first:\n' +
    '  git checkout HEAD -- Lx-Obsidian-Pro.html'
  );
  process.exit(1);
}
const src = fs.readFileSync(ARTIFACT, 'utf8');

// ── Extract the artifact pieces ──────────────────────────────────────
const styleM = src.match(/<style>[\s\S]*?<\/style>/);
if (!styleM) { console.error('artifact <style> not found'); process.exit(1); }
const artifactStyle = styleM[0];

const modStart = src.indexOf('<script type="module">') + '<script type="module">'.length;
const modEnd = src.indexOf('</script>', modStart);
if (modStart < '<script type="module">'.length || modEnd === -1) {
  console.error('module script not found'); process.exit(1);
}
let moduleJs = src.slice(modStart, modEnd);

const sanStart = src.indexOf('<script>(function(){function m(a)');
const sanEnd = src.indexOf('</script>', sanStart) + '</script>'.length;
const sanitizer = sanStart === -1 ? '' : src.slice(sanStart, sanEnd);

// ── Edits inside the app bundle ──────────────────────────────────────
const edits = [
  // Selection tint on the root would be caught by the re-skin attribute
  // selectors — lx.css already provides ::selection styling.
  [' selection:bg-[#7c5cff]/30', ''],
  // Single document h1 lives in the site header above the app.
  ['P("h1",{className:"text-[24px]', 'P("h2",{className:"text-[24px]'],
];
for (const [from, to] of edits) {
  if (!moduleJs.includes(from)) { console.error('edit target missing: ' + from); process.exit(1); }
  moduleJs = moduleJs.split(from).join(to);
}

// ── Re-skin + layout overrides ───────────────────────────────────────
const RESKIN = `<style>
    /* ==== Velora monochrome re-skin: purple accents → white / black ==== */
    /* Primary surfaces */
    [class*="bg-[#7c5cff]"]{background:#FDFDFD!important;background-image:none!important}
    [class*="bg-[#7c5cff]"].text-white{color:#0A0A0C!important}
    [class*="bg-[#7c5cff]"] .text-white,[class*="bg-[#7c5cff]"] .fill-white{color:#0A0A0C!important;fill:#0A0A0C!important}
    [class*="hover:bg-[#6b4eff]"]:hover{background:#FFFFFF!important}

    /* Gradient CTAs (Build / Build & Deploy) */
    [class*="from-[#7c5cff]"][class*="to-[#6a4ae6]"]{background-image:linear-gradient(180deg,#FFFFFF,#D9D9D9)!important}
    [class*="from-[#7c5cff]"][class*="to-[#6a4ae6]"]:hover{background-image:linear-gradient(180deg,#FFFFFF,#FFFFFF)!important}
    [class*="from-[#7c5cff]"][class*="to-[#6a4ae6]"].text-white{color:#0A0A0C!important}
    [class*="from-[#7c5cff]"][class*="to-[#6a4ae6]"] .fill-white{fill:#0A0A0C!important}
    /* Credit meter */
    [class*="from-[#7c5cff]"][class*="to-[#9b8eff]"]{background-image:linear-gradient(90deg,#FFFFFF,#9A9A9A)!important}
    /* Brand mark + decorative tiles → Velora dark mark */
    [class*="from-[#7c5cff]"][class*="to-[#c4b5ff]"],
    [class*="from-[#7c5cff]"][class*="to-[#b5a3ff]"],
    [class*="from-violet-500"]{background-image:linear-gradient(150deg,#171717,#3A3A3A)!important}

    /* Accent text / icons */
    [class*="text-[#9b8eff]"],[class*="text-[#7c5cff]"]{color:#FDFDFD!important}
    [class*="bg-white"][class*="text-[#7c5cff]"]{color:#0A0A0C!important}

    /* Borders */
    [class*="border-[#7c5cff]"]{border-color:rgba(255,255,255,.4)!important}
    [class*="border-[#2a2a35]"]{border-color:rgba(255,255,255,.14)!important}

    /* Purple glows → soft white glow */
    [class*="rgba(124,92,255"]{box-shadow:0 0 16px rgba(255,255,255,.14)!important}
    [class*="shadow-[0_0_8px_#7c5cff]"]{box-shadow:0 0 10px rgba(255,255,255,.35)!important}

    /* ==== Slim liquid-glass site header ==== */
    .app-bar{position:fixed;top:0;left:0;right:0;z-index:95;height:64px;background:rgba(8,8,8,.78);-webkit-backdrop-filter:blur(24px) saturate(150%);backdrop-filter:blur(24px) saturate(150%);border-bottom:1px solid rgba(255,255,255,.08)}
    .app-bar__inner{display:flex;align-items:center;justify-content:space-between;gap:16px;height:100%;width:min(1400px,100%);margin:0 auto;padding:0 18px}
    .app-bar__brand{display:flex;align-items:center;gap:10px;min-width:0;text-decoration:none;color:#FDFDFD}
    .app-bar__label{margin:0;font:600 .95rem/1 'Space Grotesk','Inter',sans-serif;letter-spacing:-.02em;color:#FDFDFD;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
    .app-bar__actions{display:flex;align-items:center;gap:6px;flex:none}
    .app-bar__link{display:inline-flex;align-items:center;height:34px;padding:0 15px;border-radius:999px;color:#9A9A9A;font-size:.85rem;text-decoration:none;transition:color .2s,background .2s}
    .app-bar__link:hover,.app-bar__link:focus-visible{color:#fff;background:rgba(255,255,255,.07)}
    .app-bar__link:focus-visible{outline:2px solid #FDFDFD;outline-offset:2px}
    .app-bar__brand:focus-visible{outline:2px solid #FDFDFD;outline-offset:3px;border-radius:8px}
    .app-bar .velora-cta__pill{background:var(--saas-accent);color:#0A0A0C;box-shadow:none}
    @media(max-width:560px){.app-bar__link{display:none}.app-bar__inner{padding:0 14px}}

    /* ==== App viewport fills the screen below the header ==== */
    #root{padding-top:64px;box-sizing:border-box}
    #root>div{height:calc(100dvh - 64px)!important;min-height:0}
  </style>`;

// ── Assemble the page ────────────────────────────────────────────────
const html = `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="theme-color" content="#000000">
  <link rel="icon" type="image/svg+xml" href="/assets/favicon.svg">
  <meta name="color-scheme" content="dark">
  <link rel="manifest" href="manifest.json">
  <meta name="description" content="LX AI Platform — generate, debug and preview web apps in your browser. Describe what you want to build and ship it with free NVIDIA and OpenRouter models.">
  <meta property="og:title" content="AI Platform — LX Obsidian Labs">
  <meta property="og:description" content="Generate, debug and preview web apps in your browser. Describe an idea and build it instantly with the LX AI Platform.">
  <meta property="og:type" content="website">
  <meta property="og:url" content="https://www.lxobsidianportal.co.za/ai">
  <meta property="og:image" content="https://www.lxobsidianportal.co.za/assets/og-default.svg">
  <meta name="twitter:card" content="summary_large_image">
  <link rel="canonical" href="https://www.lxobsidianportal.co.za/ai">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Space+Grotesk:wght@500;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="css/lx.css">
  <title>AI Platform — LX Obsidian Labs</title>
  <script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "name": "LX AI Platform",
    "description": "Generate, debug and preview web apps with free NVIDIA and OpenRouter AI models. Build web apps in your browser.",
    "url": "https://www.lxobsidianportal.co.za/ai",
    "applicationCategory": "DeveloperApplication",
    "operatingSystem": "Web",
    "offers": {
      "@type": "Offer",
      "price": "0",
      "priceCurrency": "ZAR"
    },
    "author": {
      "@type": "Organization",
      "name": "LX Obsidian Labs",
      "url": "https://www.lxobsidianportal.co.za"
    }
  }
  </script>
  ${artifactStyle}
  ${RESKIN}
</head>
<body>
  <a href="#main" class="skip-link">Skip to content</a>

  <!-- ------- SITE HEADER ------- -->
  <header class="app-bar">
    <div class="app-bar__inner">
      <a class="app-bar__brand" href="index.html">
        <span class="velora-brand__mark" aria-hidden="true">LX</span>
        <h1 class="app-bar__label">AI Platform</h1>
      </a>
      <nav class="app-bar__actions" aria-label="Site">
        <a class="app-bar__link" href="index.html">Back to site</a>
        <a class="app-bar__link" href="contact.html">Contact</a>
        <a class="velora-cta" href="contact.html"><span class="velora-cta__pill">Get Started</span></a>
      </nav>
    </div>
  </header>

  <main id="main">
    <div id="root"></div>
  </main>

  <script type="module">${moduleJs}</script>
  ${sanitizer}
  <script>if('serviceWorker' in navigator){window.addEventListener('load',()=>navigator.serviceWorker.register('/sw.js').catch(()=>{}));}</script>
</body>
</html>
`;

fs.writeFileSync(path.join(ROOT, 'ai.html'), html);
console.log('ai.html regenerated (' + Math.round(html.length / 1024) + ' KB)');
