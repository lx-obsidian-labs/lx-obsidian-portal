/**
 * One-off repair: the working tree claimed Vista Cinema v1.0.7 with download
 * URLs (https://lxobsidianportal.co.za/VistaCinema-release.apk) that return 404.
 * Only v1.0.6 APKs exist (GitHub raw, verified 200). Restores truthful version
 * labels and working phone/TV download URLs. Idempotent.
 */
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const BROKEN = 'https://lxobsidianportal.co.za/VistaCinema-release.apk';
const PHONE = 'https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/VistaCinema-v1.0.6.apk';
const TV = 'https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/tv/VistaCinema-TV-v1.0.6.apk';

const vistaPath = path.join(ROOT, 'vista.html');
let h = fs.readFileSync(vistaPath, 'utf8');

let urlFixed = 0;
h = h.replace(new RegExp('<a\\b[^>]*href="' + BROKEN.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '"[^>]*>', 'g'), tag => {
  const url = /vc-dl--tv/.test(tag) ? TV : PHONE;
  urlFixed++;
  return tag.replace(BROKEN, url);
});

const verBefore = (h.match(/1\.0\.7/g) || []).length;
h = h.split('1.0.7').join('1.0.6');

fs.writeFileSync(vistaPath, h);
console.log('vista.html: ' + urlFixed + ' download URLs repaired, ' + verBefore + ' version labels corrected');

// vista-version.json → restore last-known-good (v1.0.6, GitHub raw, real changelog)
const { execSync } = require('child_process');
execSync('git checkout HEAD -- vista-version.json', { cwd: ROOT, stdio: 'inherit' });
console.log('vista-version.json restored to HEAD (v1.0.6)');
