const fs = require('fs');
let h = fs.readFileSync('dist/vista.html', 'utf8');
// Replace deploy/VistaCinema-v1.0.0.apk with GitHub raw URL
// Use string replacement instead of regex
h = h.replace('deploy/VistaCinema-v1.0.0.apk', 'https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/VistaCinema-v1.0.0.apk');
// Also replace any other deploy/ paths with github raw
// But only replace the apk references, not all deploy/ paths
// Write back
fs.writeFileSync('dist/vista.html', h);
// Check
const d = (h.match(/\\/deploy/gi) || []).length;
console.log('deploy/ references remaining:', d);
// Check for apk references
const a = (h.match(/\.apk/gi) || []).length;
console.log('.apk references:', a);
// Show the apk link context
const idx = h.indexOf('VistaCinema');
if (idx > -1) {
  console.log('VistaCinema context:', h.substring(idx-20, idx+80));
}
"