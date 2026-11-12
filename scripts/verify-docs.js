// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const required = ['README.md','docs/MAINTAINER_GUIDE.md','docs/CONTACT_API_REFERENCE.md','docs/DOCUMENTATION_STANDARD.md','docs/ARCHITECTURE.md','docs/PRODUCT_API_REFERENCE.md','docs/SECURITY_REVIEW.md'];
for (const name of required) {
  const file = path.join(root,name);
  const body = fs.readFileSync(file,'utf8');
  assert.match(body,/^#\s+\S/m,name+' missing title');
  assert.ok(body.includes('LX Obsidian Labs'),name+' missing attribution');
  for (const match of body.matchAll(/\]\(([^)]+)\)/g)) {
    const target=match[1].split('#')[0];
    if (!target || /^[a-z][a-z0-9+.-]*:/i.test(target)) continue;
    assert.ok(fs.existsSync(path.resolve(path.dirname(file),target)),name+' broken link '+target);
  }
}
console.log('Portal documentation integrity verified: '+required.length+' core documents');
