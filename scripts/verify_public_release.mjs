import { createHash } from 'node:crypto';

const base = 'https://www.lxobsidianportal.co.za';
const response = await fetch(`${base}/vista-version.json?verify=${Date.now()}`, {
  cache: 'no-store', signal: AbortSignal.timeout(30000)
});
if (!response.ok) throw new Error(`Manifest HTTP ${response.status}`);
const manifest = await response.json();
if (manifest.versionCode !== 19 || manifest.versionName !== '1.0.12') {
  throw new Error(`Unexpected live release: ${manifest.versionName}/${manifest.versionCode}`);
}
console.log('Live manifest:', manifest.versionName, manifest.versionCode, manifest.sha256);
for (const platform of ['android', 'tv']) {
  const download = await fetch(`${base}/api/vista-download?platform=${platform}`, {
    signal: AbortSignal.timeout(120000)
  });
  if (!download.ok) throw new Error(`${platform} HTTP ${download.status}`);
  const hash = createHash('sha256');
  let size = 0;
  for await (const chunk of download.body) { hash.update(chunk); size += chunk.length; }
  const sha256 = hash.digest('hex').toUpperCase();
  if (sha256 !== manifest.sha256.toUpperCase() || size !== manifest.sizeBytes) {
    throw new Error(`${platform}: public APK checksum/size mismatch`);
  }
  console.log(`${platform}: verified ${size} bytes, SHA-256 ${sha256}`);
}
for (const path of ['/vista', '/downloads']) {
  const page = await fetch(base + path, { signal: AbortSignal.timeout(30000) });
  const html = await page.text();
  if (!page.ok || !html.includes('1.0.12')) throw new Error(`${path}: release page not updated`);
  console.log(`${path}: live 1.0.12`);
}
