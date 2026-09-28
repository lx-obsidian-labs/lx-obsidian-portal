const fs = require('fs');
const h = fs.readFileSync('dist/index.html', 'utf8');
// Check for any data-color attributes
const matches = h.match(/data-color="[^"]*""/g);
console.log('matches type:', typeof matches);
if (matches !== null) {
  console.log('All data-color attributes found:');
  matches.forEach(m => console.log(' -', m));
} else {
  console.log('No data-color attributes found in index.html');
}
// Check for app-card references
console.log('app-card references:', (h.match(/app-card/g) || []).length);
console.log('app-modal references:', (h.match(/app-modal/g) || []).length);
// Check for marketplace-card
console.log('marketplace-card references:', (h.match(/marketplace-card/g) || []).length);
// Check for the color values used
console.log('--violet count:', (h.match(/--violet/g) || []).length);
console.log('--violet-2 count:', (h.match(/--violet-2/g) || []).length);
console.log('--blue count:', (h.match(/--blue/g) || []).length);