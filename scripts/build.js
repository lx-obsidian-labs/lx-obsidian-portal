const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const DIST = path.join(ROOT, 'dist');

const ROOT_HTML = fs.readdirSync(ROOT).filter(name => name.endsWith('.html') && fs.statSync(path.join(ROOT, name)).isFile());

const INCLUDE = [
  ...ROOT_HTML,
  'js', 'assets', 'functions',
  '_headers', '_redirects',
  'sw.js', 'manifest.json', 'robots.txt', 'sitemap.xml', 'llms.txt',
  'vista-version.json',
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
  // Match quoted HTML attributes, leaving external, absolute and fragment URLs unchanged.
  html = html.replace(/\b(href|src|poster)=(["'])([^"']+)\2/gi, (match, attr, quote, target) => {
    if (/^(?:\/|#|[a-z][a-z0-9+.-]*:|\/\/)/i.test(target)) return match;
    return attr + '=' + quote + '/' + target + quote;
  });
  // Avoid misleading Login labels that actually lead to Contact.
  html = html.replace(/(<a\b[^>]*href=["']\/?contact(?:\.html)?["'][^>]*>)\s*Login\s*(<\/a>)/gi, '$1Contact$2');
  html = html.replace(/(<span class=["']velora-cta__pill["']>)Get Started(<\/span>)/g, '$1Start a Project$2');
  fs.writeFileSync(output, html);
}

// A missing entry point must fail the build rather than silently ship a 404.
for (const required of ['index.html', '404.html', 'pdf.html', '_redirects', 'css/lx.css']) {
  if (!fs.existsSync(path.join(DIST, required))) throw new Error('Missing required published asset: ' + required);
}
console.log('Verified ' + ROOT_HTML.length + ' HTML pages and required published assets');

const total = countFiles(DIST);
console.log(`Build complete: dist/ (${total} files, ${getSize(DIST)} KB)`);

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
  return Math.round(total / 1024);
}
