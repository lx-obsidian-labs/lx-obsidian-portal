const fs = require('fs');
const pages = ['index.html', 'about.html', 'advertise.html', 'blog.html', 'contact.html', 'faq.html', 'industries.html', 'marketplace.html', 'portfolio.html', 'services.html', 'synapse.html', 'vista.html'];
let allOk = true;
for (const p of pages) {
  const h = fs.readFileSync('dist/' + p, 'utf8');
  const issues = [];
  // Check required elements
  if (!h.includes('color-scheme')) issues.push('missing color-scheme');
  if (!h.includes('--violet')) issues.push('missing --violet');
  if (!h.includes('--violet-2')) issues.push('missing --violet-2');
  if (!h.includes('id="starfield"')) issues.push('missing starfield');
  if (!h.includes('id="cursorGlow"')) issues.push('missing cursorGlow');
  if (!h.includes('loading-bar__fill')) issues.push('missing loading-bar__fill');
  // Check no broken links
  if (h.includes('/deploy/')) issues.push('has /deploy/ links');
  // Check for APK links consistency
  const apkCount = (h.match(/\.apk/gi) || []).length;
  if (p === 'index.html' && apkCount !== 4) issues.push('wrong apk count: ' + apkCount);
  if (p === 'vista.html' && apkCount !== 3) issues.push('wrong apk count on vista: ' + apkCount);
  
  if (issues.length > 0) {
    console.log(p + ': ' + issues.join(', '));
    allOk = false;
  } else {
    console.log(p + ': OK');
  }
}
console.log('\n' + (allOk ? 'ALL PAGES OK' : 'SOME ISSUES FOUND'));