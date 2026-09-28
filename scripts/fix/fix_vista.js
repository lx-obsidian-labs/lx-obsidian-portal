const fs = require('fs');
let h = fs.readFileSync('dist/vista.html', 'utf8');
// Global replace - replace ALL occurrences case-insensitively
// Use string replace with a function or multiple replaces
// First, let's just replace the exact string
h = h.replace(/deploy\/VistaCinema-v1.0.0.apk/gi, 'https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/VistaCinema-v1.0.0.apk');
// Write back
fs.writeFileSync('dist/vista.html', h);
// Verify
console.log('Has old deploy/:', h.includes('deploy/'));
console.log('.apk count:', (h.match(/\.apk/gi) || []).length);
console.log('GitHub raw present:', h.includes('github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master'));
// Show context
const idx = h.indexOf('VistaCinema');
if (idx > -1) {
  console.log('VistaCinema context:', h.substring(idx-20, idx+80));
}