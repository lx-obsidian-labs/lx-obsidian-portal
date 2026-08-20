const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const DIST = path.join(ROOT, 'dist');

const INCLUDE = [
  'index.html', '404.html', 'about.html', 'blog.html', 'contact.html',
  'faq.html', 'marketplace.html', 'portfolio.html', 'services.html',
  'synapse.html', 'industries.html', 'partners.html', 'advertise.html',
  'vista.html',
  'js', 'assets', 'dashboard', 'deploy',
  'functions',
  '_headers', '_redirects',
  'sw.js', 'manifest.json', 'robots.txt', 'sitemap.xml', 'ads.txt',
  'package.json'
];

const CSS_ORDER = [
  'variables.css','reset.css','layout.css','components.css','animations.css',
  'responsive.css','features.css','investor-ui.css','polish.css','refinement.css',
  'advanced-theme.css'
];

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
fs.writeFileSync(path.join(distCssDir, 'style.css'), combinedCss);
console.log(`CSS concatenated: ${CSS_ORDER.length} files -> dist/css/style.css (${Math.round(combinedCss.length / 1024)} KB)`);

for (const file of fs.readdirSync(cssDir)) {
  if (!CSS_ORDER.includes(file) && file !== 'style.css') fs.copyFileSync(path.join(cssDir, file), path.join(distCssDir, file));
}

function svgData(svg) {
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svg.replace(/\n\s*/g, ' '));
}

const lxVectors = {
  'assets/generated/lx-cube-swarm.webp': svgData(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 900"><defs><radialGradient id="g" cx="50%" cy="52%" r="50%"><stop stop-color="#35126f"/><stop offset=".5" stop-color="#090b18"/><stop offset="1" stop-color="#020204"/></radialGradient><linearGradient id="e" x1="0" x2="1"><stop stop-color="#b642ff"/><stop offset=".5" stop-color="#6d4dff"/><stop offset="1" stop-color="#31c9ff"/></linearGradient><filter id="gl"><feGaussianBlur stdDeviation="10" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><rect width="900" height="900" fill="url(#g)"/><g opacity=".18" stroke="#7b5cff" fill="none"><circle cx="450" cy="450" r="290"/><circle cx="450" cy="450" r="220"/><circle cx="450" cy="450" r="150"/></g><ellipse cx="450" cy="720" rx="235" ry="42" fill="#7d2dff" opacity=".14"/><g transform="translate(450 430)" filter="url(#gl)" stroke="url(#e)" stroke-width="3"><g fill="#0b0d14"><rect x="-95" y="-205" width="190" height="190" rx="30" transform="rotate(7)"/><rect x="-235" y="-70" width="190" height="190" rx="30" transform="rotate(-18)"/><rect x="55" y="-65" width="190" height="190" rx="30" transform="rotate(18)"/><rect x="-105" y="60" width="190" height="190" rx="30" transform="rotate(-8)"/></g></g><g fill="#7f4dff"><circle cx="173" cy="327" r="3"/><circle cx="701" cy="244" r="3"/><circle cx="747" cy="462" r="2"/><circle cx="235" cy="601" r="2"/></g></svg>`),
  'assets/generated/lx-hollow-cube.webp': svgData(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 700 500"><defs><linearGradient id="a" x1="0" x2="1"><stop stop-color="#151722"/><stop offset=".55" stop-color="#090a10"/><stop offset="1" stop-color="#111321"/></linearGradient><linearGradient id="e" x1="0" x2="1"><stop stop-color="#9b42ff"/><stop offset="1" stop-color="#2fc8ff"/></linearGradient><filter id="g"><feGaussianBlur stdDeviation="7" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><rect width="700" height="500" fill="#050609"/><g transform="translate(350 235) rotate(18)" fill="url(#a)" stroke="url(#e)" stroke-width="2.5" filter="url(#g)"><rect x="-145" y="-145" width="290" height="290" rx="72"/><rect x="-80" y="-80" width="160" height="160" rx="46" fill="#050609"/><ellipse cx="-150" cy="0" rx="42" ry="74" fill="#050609"/><ellipse cx="150" cy="0" rx="42" ry="74" fill="#050609"/><ellipse cx="0" cy="-150" rx="74" ry="42" fill="#050609"/><ellipse cx="0" cy="150" rx="74" ry="42" fill="#050609"/></g><ellipse cx="350" cy="430" rx="130" ry="24" fill="#7f34ff" opacity=".13"/></svg>`),
  'assets/generated/lx-geodesic-sphere.webp': svgData(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 700 500"><defs><radialGradient id="s"><stop stop-color="#15182a"/><stop offset="1" stop-color="#050609"/></radialGradient><linearGradient id="e" x1="0" x2="1"><stop stop-color="#a743ff"/><stop offset="1" stop-color="#29caff"/></linearGradient></defs><rect width="700" height="500" fill="#050609"/><circle cx="350" cy="235" r="150" fill="url(#s)" stroke="url(#e)" stroke-width="2"/><g stroke="url(#e)" stroke-width="2" fill="none" opacity=".85"><path d="M350 85 220 160l-20 150 150 75 150-75-20-150z"/><path d="M220 160 350 235 480 160M200 310l150-75 150 75M350 85v300"/><path d="M245 120 280 350M455 120 420 350M200 235h300"/></g><g fill="#0b0d14" stroke="#a26cff"><circle cx="350" cy="85" r="8"/><circle cx="220" cy="160" r="8"/><circle cx="480" cy="160" r="8"/><circle cx="200" cy="310" r="8"/><circle cx="500" cy="310" r="8"/><circle cx="350" cy="385" r="8"/><circle cx="350" cy="235" r="10"/></g><ellipse cx="350" cy="430" rx="130" ry="22" fill="#7530ff" opacity=".13"/></svg>`),
  'assets/generated/lx-geometric-core.webp': svgData(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 700 500"><defs><linearGradient id="e" x1="0" x2="1"><stop stop-color="#a540ff"/><stop offset="1" stop-color="#34caff"/></linearGradient><filter id="g"><feGaussianBlur stdDeviation="8" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><rect width="700" height="500" fill="#050609"/><g transform="translate(350 235)" filter="url(#g)" fill="#0a0b11" stroke="url(#e)" stroke-width="3"><polygon points="0,-150 130,-75 130,75 0,150 -130,75 -130,-75"/><polygon points="0,-95 82,-48 82,48 0,95 -82,48 -82,-48"/><polygon points="0,-40 35,-20 35,20 0,40 -35,20 -35,-20" fill="#6d34ff" opacity=".55"/></g><g stroke="#7048ff" opacity=".18"><circle cx="350" cy="235" r="180" fill="none"/><circle cx="350" cy="235" r="210" fill="none"/></g><ellipse cx="350" cy="430" rx="130" ry="22" fill="#7530ff" opacity=".14"/></svg>`),
  'assets/generated/lx-system-monitor.webp': svgData(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 760 500"><defs><linearGradient id="e" x1="0" x2="1"><stop stop-color="#8d3dff"/><stop offset="1" stop-color="#31caff"/></linearGradient></defs><rect width="760" height="500" rx="28" fill="#07080c" stroke="#242938" stroke-width="2"/><text x="42" y="52" fill="#d8dbe7" font-family="monospace" font-size="22">LX SYSTEM NODE</text><circle cx="690" cy="44" r="7" fill="#4df3bd"/><text x="612" y="52" fill="#4df3bd" font-family="monospace" font-size="16">ONLINE</text><g font-family="monospace"><text x="42" y="112" fill="#777e91" font-size="16">CPU</text><text x="42" y="162" fill="#fff" font-size="42">28%</text><text x="220" y="112" fill="#777e91" font-size="16">MEMORY</text><text x="220" y="162" fill="#fff" font-size="42">62%</text><text x="420" y="112" fill="#777e91" font-size="16">NETWORK</text><text x="420" y="162" fill="#fff" font-size="42">1.3 GB/s</text></g><path d="M42 280 C95 250 120 330 175 290 S255 245 300 300 380 345 430 270 505 230 560 295 635 320 700 250" fill="none" stroke="url(#e)" stroke-width="5"/><g stroke="#202534" opacity=".7"><path d="M42 210H710M42 260H710M42 310H710M42 360H710"/><path d="M100 210V360M190 210V360M280 210V360M370 210V360M460 210V360M550 210V360M640 210V360"/></g><rect x="42" y="402" width="676" height="48" rx="12" fill="#0b0d14" stroke="#242938"/><text x="64" y="433" fill="#9d67ff" font-family="monospace" font-size="16">SYSTEM HEALTH</text><text x="600" y="433" fill="#50e4c1" font-family="monospace" font-size="16">98.7%</text></svg>`),
  'assets/generated/lx-code-dashboard.webp': svgData(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 760 500"><rect width="760" height="500" rx="28" fill="#07080c" stroke="#25293a" stroke-width="2"/><text x="42" y="54" fill="#8b93aa" font-family="monospace" font-size="18">LX_OBSIDIAN_CORE.JS</text><g font-family="monospace" font-size="21"><text x="65" y="120" fill="#a04bff">const</text><text x="132" y="120" fill="#dfe5f4"> lx = </text><text x="210" y="120" fill="#4fd3ff">new</text><text x="255" y="120" fill="#dfe5f4"> LXObsidian();</text><text x="65" y="165" fill="#dfe5f4">lx.init({</text><text x="95" y="205" fill="#7fd3ff">mode:</text><text x="175" y="205" fill="#57e2ad"> 'performance'</text><text x="95" y="245" fill="#7fd3ff">ai:</text><text x="145" y="245" fill="#a04bff"> true</text><text x="65" y="285" fill="#dfe5f4">});</text><text x="65" y="350" fill="#7fd3ff">lx.run();</text></g><rect x="430" y="95" width="260" height="270" rx="18" fill="#0b0d14" stroke="#222739"/><path d="M455 290 C490 250 515 295 550 230 585 170 615 280 665 180" fill="none" stroke="#8c45ff" stroke-width="4"/><circle cx="665" cy="180" r="6" fill="#31caff"/><text x="455" y="130" fill="#9097aa" font-family="monospace" font-size="16">PERFORMANCE</text><text x="455" y="335" fill="#52e1b1" font-family="monospace" font-size="16">ENGINE READY</text></svg>`)
};

const homePath = path.join(DIST, 'index.html');
if (fs.existsSync(homePath)) {
  let home = fs.readFileSync(homePath, 'utf8');
  for (const [src, data] of Object.entries(lxVectors)) home = home.split(src).join(data);
  home = home.split('assets/generated/lx-system-monitor.webp').join(lxVectors['assets/generated/lx-system-monitor.webp']);
  home = home.split('assets/generated/lx-code-dashboard.webp').join(lxVectors['assets/generated/lx-code-dashboard.webp']);
  home = home.replace('https://www.lxobsidianportal.co.za/assets/generated/lx-cube-swarm.webp','https://www.lxobsidianportal.co.za/assets/generated/lx-hero-core.webp');
  fs.writeFileSync(homePath, home);
  console.log('Embedded resilient LX vector artwork into dist/index.html');
}

// Inject the shared advanced experience layer into every public HTML document.
// This keeps all pages visually synchronized without duplicating markup across files.
for (const file of fs.readdirSync(DIST)) {
  if (!file.endsWith('.html')) continue;
  const filePath = path.join(DIST, file);
  let html = fs.readFileSync(filePath, 'utf8');
  if (!html.includes('js/experience.js')) {
    html = html.replace(/<\/body>/i, '  <script src="js/experience.js" defer></script>\n</body>');
    fs.writeFileSync(filePath, html);
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
