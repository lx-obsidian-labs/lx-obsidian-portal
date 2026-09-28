const fs = require('fs');

// Read features.css
let f = fs.readFileSync('css/features.css', 'utf8');
// Remove app-card__icon data-color rules
f = f.replace(/\.app-card__icon\[data-color="cyan"\] { [^}]+}/g, '');
f = f.replace(/\.app-card__icon\[data-color="red"\] { [^}]+}/g, '');
// Remove app-modal__icon data-color rules
f = f.replace(/\.app-modal__icon\[data-color="cyan"\] { [^}]+}/g, '');
f = f.replace(/\.app-modal__icon\[data-color="red"\] { [^}]+}/g, '');
// Write back
fs.writeFileSync('css/features.css', f);
console.log('features.css updated');

// Check remaining data-color or color rules
const f2 = fs.readFileSync('css/features.css', 'utf8');
const remaining = (f2.match(/data-color=/g) || []).length;
console.log('data-color rules remaining in features.css:', remaining);

// Read components.css
let c = fs.readFileSync('css/components.css', 'utf8');
// Remove marketplace-card__logo data-color rules
c = c.replace(/\.marketplace-card__logo\[data-color="cyan"\] { [^}]+}/g, '');
c = c.replace(/\.marketplace-card__logo\[data-color="red"\] { [^}]+}/g, '');
// Write back
fs.writeFileSync('css/components.css', c);
console.log('components.css updated');

// Check remaining
const c2 = fs.readFileSync('css/components.css', 'utf8');
const remaining2 = (c2.match(/data-color=/g) || []).length;
console.log('data-color rules remaining in components.css:', remaining2);