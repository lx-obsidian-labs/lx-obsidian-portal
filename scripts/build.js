const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const DIST = path.join(ROOT, 'dist');

const INCLUDE = [
  'index.html', '404.html', 'about.html', 'ai.html', 'blog.html', 'contact.html',
  'faq.html', 'marketplace.html', 'portfolio.html', 'services.html',
  'synapse.html', 'vista.html',
  'js', 'assets', 'functions',
  '_headers', '_redirects',
  'sw.js', 'manifest.json', 'robots.txt', 'sitemap.xml',
  'package.json'
];

const CSS_ORDER = ['lx.css', 'saas.css'];

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
    fs.copyFileSync(path.join(cssDir, file), path.join(distCssDir, file));
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
