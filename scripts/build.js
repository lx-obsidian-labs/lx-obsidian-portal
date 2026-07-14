const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const DIST = path.join(ROOT, 'dist');

const INCLUDE = [
  'index.html', '404.html', 'about.html', 'blog.html', 'contact.html',
  'faq.html', 'marketplace.html', 'portfolio.html', 'services.html',
  'synapse.html',
  'js', 'assets', 'dashboard',
  'functions',
  '_headers', '_redirects',
  'sw.js', 'manifest.json', 'robots.txt', 'sitemap.xml', 'ads.txt',
  'package.json'
];

const CSS_ORDER = [
  'variables.css',
  'reset.css',
  'layout.css',
  'components.css',
  'animations.css',
  'responsive.css',
  'features.css',
  'bright.css'
];

if (fs.existsSync(DIST)) {
  fs.rmSync(DIST, { recursive: true });
}

for (const name of INCLUDE) {
  const srcPath = path.join(ROOT, name);
  if (!fs.existsSync(srcPath)) continue;

  const destPath = path.join(DIST, name);
  const stat = fs.statSync(srcPath);

  if (stat.isDirectory()) {
    fs.cpSync(srcPath, destPath, { recursive: true });
  } else {
    fs.mkdirSync(path.dirname(destPath), { recursive: true });
    fs.copyFileSync(srcPath, destPath);
  }
}

// Concatenate CSS into a single file to avoid @import waterfall
const cssDir = path.join(ROOT, 'css');
const distCssDir = path.join(DIST, 'css');
fs.mkdirSync(distCssDir, { recursive: true });

let combinedCss = '';
for (const file of CSS_ORDER) {
  const filePath = path.join(cssDir, file);
  if (fs.existsSync(filePath)) {
    combinedCss += fs.readFileSync(filePath, 'utf8') + '\n\n';
  }
}
fs.writeFileSync(path.join(distCssDir, 'style.css'), combinedCss);
console.log(`CSS concatenated: ${CSS_ORDER.length} files -> dist/css/style.css (${Math.round(combinedCss.length / 1024)} KB)`);

// Copy remaining CSS files that pages might reference individually
for (const file of fs.readdirSync(cssDir)) {
  if (!CSS_ORDER.includes(file) && file !== 'style.css') {
    fs.copyFileSync(path.join(cssDir, file), path.join(distCssDir, file));
  }
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
