// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
// Preserve existing third-party attribution and applicable notices.
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const DIST = path.join(ROOT, 'dist');

const ROOT_HTML = fs.readdirSync(ROOT).filter(name => name.endsWith('.html') && fs.statSync(path.join(ROOT, name)).isFile());
const vistaRelease = JSON.parse(fs.readFileSync(path.join(ROOT, 'vista-version.json'), 'utf8'));

const INCLUDE = [
  ...ROOT_HTML,
  'js', 'assets', 'functions',
  '_headers', '_redirects',
  'sw.js', 'manifest.json', 'robots.txt', 'sitemap.xml', 'llms.txt',
  'vista-version.json',
  'brand.json',
  'package.json'
];

// lx.css is the complete unified theme (Velora monochrome). saas.css is legacy
// purple-theme code kept for reference only — concatenating it after lx.css
// overrode every :root token and reverted production to the old theme.
const CSS_ORDER = ['lx.css'];

if (fs.existsSync(DIST)) fs.rmSync(DIST, { recursive: true });

for (const name of INCLUDE) {
  const srcPath = path.join(ROOT, name);
  if (!fs.existsSync(srcPath)) continue;
  const destPath = path.join(DIST, name);
  const stat = fs.statSync(srcPath);
  if (stat.isDirectory()) fs.cpSync(srcPath, destPath, { recursive: true });
  else {
    fs.mkdirSync(path.dirname(destPath), { recursive: true });
    fs.copyFileSync(srcPath, destPath);
  }
}

// Generate verified downloadable PDF and CSV resources into the deploy output.
require('./generate-downloadables.js');

// Publish the Synapse web workspace at /synapse/web without requiring SPA fallback.
const synapseWebDir = path.join(DIST, 'synapse', 'web');
fs.mkdirSync(synapseWebDir, { recursive: true });
fs.copyFileSync(path.join(ROOT, 'synapse-web.html'), path.join(synapseWebDir, 'index.html'));

// Publish native PDF Studio directory and avoid conflicting rewrite rules.
const pdfDir = path.join(DIST, 'pdf');
fs.mkdirSync(pdfDir, { recursive: true });
fs.copyFileSync(path.join(ROOT, 'pdf.html'), path.join(pdfDir, 'index.html'));
const redirectFile = path.join(DIST, '_redirects');
const rules = fs.readFileSync(redirectFile, 'utf8').split('\n');
fs.writeFileSync(redirectFile, rules.filter(line => !line.trim().startsWith('/pdf ' ) && !line.trim().startsWith('/pdf/ ')).join('\n'));

const cssDir = path.join(ROOT, 'css');
const distCssDir = path.join(DIST, 'css');
fs.mkdirSync(distCssDir, { recursive: true });
let combinedCss = '';
for (const file of CSS_ORDER) {
  const filePath = path.join(cssDir, file);
  if (fs.existsSync(filePath)) combinedCss += `/* ${file} */\n` + fs.readFileSync(filePath, 'utf8') + '\n\n';
}
fs.writeFileSync(path.join(distCssDir, 'lx.css'), combinedCss);
console.log(`CSS concatenated: ${CSS_ORDER.length} files -> dist/css/lx.css (${Math.round(combinedCss.length / 1024)} KB)`);

for (const file of fs.readdirSync(cssDir)) {
  if (!CSS_ORDER.includes(file) && file !== 'style.css' && file !== 'lx.css') {
    copyRecursive(path.join(cssDir, file), path.join(distCssDir, file));
  }
}

function copyRecursive(src, dest) {
  const stat = fs.statSync(src);
  if (stat.isDirectory()) fs.cpSync(src, dest, { recursive: true });
  else {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.copyFileSync(src, dest);
  }
}

const homePath = path.join(DIST, 'index.html');
if (fs.existsSync(homePath)) {
  let home = fs.readFileSync(homePath, 'utf8');
  const rasterMap = {
    'assets/generated/lx-cube-swarm.webp': 'assets/generated/lx-hero-cluster-v3.webp',
    'assets/generated/lx-hollow-cube.webp': 'assets/generated/lx-step-discover-v3.webp',
    'assets/generated/lx-geodesic-sphere.webp': 'assets/generated/lx-step-engineer-v3.webp',
    'assets/generated/lx-geometric-core.webp': 'assets/generated/lx-step-launch-v3.webp',
    'assets/generated/lx-system-monitor.webp': 'assets/generated/lx-system-dashboard-v3.webp',
    'assets/generated/lx-code-dashboard.webp': 'assets/generated/lx-developer-dashboard-v3.webp'
  };
  for (const [oldPath, newPath] of Object.entries(rasterMap)) {
    home = home.split(oldPath).join(newPath);
  }
  fs.writeFileSync(homePath, home);
  console.log('Homepage wired to genuine LX raster artwork');
}

// Use the homepage navigation as the canonical shared header for all pages
// that already implement the Velora navigation component.
const homeHtml = fs.readFileSync(path.join(DIST, 'index.html'), 'utf8');
const navPattern = /<header class="velora-nav"[^>]*>[\s\S]*?<\/header>/;
const canonicalNav = homeHtml.match(navPattern)?.[0];
// The shared nav is used on inner pages too; section anchors must resolve to the homepage.
if (canonicalNav && (/href=\"#solutions\"/.test(canonicalNav) || /href=\"#products\"/.test(canonicalNav))) {
  throw new Error('Shared navigation has a page-relative section anchor; use /#solutions and /#products');
}
if (!canonicalNav) throw new Error('Canonical homepage navigation missing');
let synchronizedNavs = 0;
for (const file of ROOT_HTML) {
  if (file === 'index.html') continue;
  const output = path.join(DIST, file);
  const html = fs.readFileSync(output, 'utf8');
  if (!navPattern.test(html)) continue;
  fs.writeFileSync(output, html.replace(navPattern, canonicalNav));
  synchronizedNavs++;
}
console.log('Synchronized Velora navigation on ' + synchronizedNavs + ' additional pages');

const footerPattern = /<footer class="glass-footer"[^>]*>[\s\S]*?<\/footer>/;
const canonicalFooter = homeHtml.match(footerPattern)?.[0];
let synchronizedFooters = 0;
if (canonicalFooter) {
  for (const file of ROOT_HTML) {
    if (file === 'index.html') continue;
    const output = path.join(DIST, file);
    const html = fs.readFileSync(output, 'utf8');
    if (!footerPattern.test(html)) continue;
    fs.writeFileSync(output, html.replace(footerPattern, canonicalFooter));
    synchronizedFooters++;
  }
}
console.log('Synchronized glass footer on ' + synchronizedFooters + ' additional pages');


// Normalize root-relative assets and internal HTML links across every published page.
// This prevents nested/trailing-slash routes from resolving css/, js/ or assets/ incorrectly.
for (const file of ROOT_HTML) {
  const output = path.join(DIST, file);
  let html = fs.readFileSync(output, 'utf8');
  if (file === 'vista.html') {
    html = html.replaceAll('1.0.11', vistaRelease.versionName)
      .replace('brings clearer catalog and playback recovery, refined TV navigation and a new way to tell us how Vista really feels.',
        'brings improved playback and catalog recovery, TV playback options, official support, clearer login errors and expanded avatars.');
  }
  // Match quoted HTML attributes, leaving external, absolute and fragment URLs unchanged.
  html = html.replace(/\b(href|src|poster)=(["'])([^"']+)\2/gi, (match, attr, quote, target) => {
    if (/^(?:\/|#|[a-z][a-z0-9+.-]*:|\/\/)/i.test(target)) return match;
    return attr + '=' + quote + '/' + target + quote;
  });
  // Direct internal page links to canonical public routes, preserving fragments and queries.
  html = html.replace(/\bhref=(["'])([^"']+)\1/gi, (match, quote, href) => {
    const origin = 'https://www.lxobsidianportal.co.za';
    const prefix = href.startsWith(origin+'/') ? origin : '';
    const local = prefix ? href.slice(prefix.length) : href;
    if (!local.startsWith('/') || local.startsWith('//')) return match;
    const [route, suffix = ''] = local.split(/(?=[?#])/s, 2);
    const tail = local.slice(route.length);
    let dest = route;
    if (route === '/pdf' || route === '/pdf.html' || route === '/pdf/index.html') dest = '/pdf/';
    else if (route === '/index.html') dest = '/';
    else if (route === '/synapse-web.html') dest = '/synapse/web';
    else if (route.endsWith('.html') && ROOT_HTML.includes(route.slice(1)) && route !== '/404.html') dest = route.slice(0,-5);
    return 'href='+quote+prefix+dest+tail+quote;
  });
  // Avoid misleading Login labels that actually lead to Contact.
  html = html.replace(/(<a\b[^>]*href=["']\/?contact(?:\.html)?["'][^>]*>)\s*Login\s*(<\/a>)/gi, '$1Contact$2');
  html = html.replace(/(<span class=["']velora-cta__pill["']>)Get Started(<\/span>)/g, '$1Start a Project$2');
  // Page styles load first so shared responsive refinements can target each layout.
  html = html.replace(/<\/head>/i, '<link rel="stylesheet" href="/css/site-layout.css?v=20261119-3">\n</head>');
  fs.writeFileSync(output, html);
}

// Refresh nested entrypoints from normalized source so navigation and assets match.
fs.copyFileSync(path.join(DIST, 'pdf.html'), path.join(pdfDir, 'index.html'));
fs.copyFileSync(path.join(DIST, 'synapse-web.html'), path.join(synapseWebDir, 'index.html'));

// Remove the legacy PDF file only after HTML normalization has completed.
fs.rmSync(path.join(DIST, 'pdf.html'), { force: true });

// A missing entry point must fail the build rather than silently ship a 404.
for (const required of ['index.html', '404.html', 'pdf/index.html', 'synapse/web/index.html', '_redirects', 'css/lx.css']) {
  if (!fs.existsSync(path.join(DIST, required))) throw new Error('Missing required published asset: ' + required);
}
console.log('Verified ' + ROOT_HTML.length + ' HTML pages and required published assets');

// Apply the current name to all published templates and dynamic product labels, not archives/URLs.
const { brandPublicContent } = require('./brand.js');
function publishBrand(dir) {
  for (const item of fs.readdirSync(dir, {withFileTypes:true})) {
    const file = path.join(dir, item.name);
    if (item.isDirectory()) publishBrand(file);
    else if (/\.(html|js|json|svg|txt|xml)$/i.test(item.name)) {
      const text = fs.readFileSync(file, 'utf8');
      const updated = brandPublicContent(text);
      if (updated !== text) fs.writeFileSync(file, updated);
    }
  }
}
publishBrand(DIST);

// Retired raster marketing composites contain the previous name baked into their pixels.
for (const name of ['vista-marketing-1.png', 'vista-marketing-2.png']) {
  for (const folder of ['assets', 'assets/images']) fs.rmSync(path.join(DIST, folder, name), {force:true});
}

// APKs are GitHub assets, never Cloudflare Pages uploads. Fail closed if packaging regresses.
function verifyNoInstallers(dir) {
  for (const item of fs.readdirSync(dir, { withFileTypes: true })) {
    const file = path.join(dir, item.name);
    if (item.isDirectory()) verifyNoInstallers(file);
    else if (/\.(apk|aab|ipa|p12|mobileprovision|jks|keystore)$/i.test(item.name)) throw new Error('Installer/signing file leaked into Pages output: ' + file);
  }
}
verifyNoInstallers(DIST);

const total = countFiles(DIST);
console.log(`Build complete: dist/ (${total} files, ${Math.round(getSize(DIST) / 1024)} KB)`);

function countFiles(dir) {
  let count = 0;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) count += countFiles(p);
    else if (!entry.name.startsWith('.') && entry.name !== 'package-lock.json') count++;
  }
  return count;
}

function getSize(dir) {
  let total = 0;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) total += getSize(p);
    else total += fs.statSync(p).size;
  }
  return total;
}
