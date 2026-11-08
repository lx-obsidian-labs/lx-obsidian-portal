const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../functions/api/vista-download.js'), 'utf8');
const handler = import('data:text/javascript;base64,' + Buffer.from(source).toString('base64')).then(m => m.onRequest);
const context = (suffix = '', method = 'GET') => ({request:new Request('https://www.lxobsidianportal.co.za/api/vista-download'+suffix,{method}),waitUntil(){}});

test('redirects to the GitHub asset CDN without reading APK bytes', async () => {
  const original = globalThis.fetch;
  let cancelled = false;
  try {
    globalThis.fetch = async (url, init) => {
      assert.match(url, /releases\/assets\/723495986$/);
      assert.equal(init.redirect, 'manual');
      return new Response(new ReadableStream({cancel(){cancelled=true;}}), {status:302,headers:{Location:'https://release-assets.githubusercontent.com/test.apk?token=public-temporary'}});
    };
    const response = await (await handler)(context());
    assert.equal(response.status,302);
    assert.equal(new URL(response.headers.get('Location')).hostname,'release-assets.githubusercontent.com');
    assert.equal(response.headers.get('Cache-Control'),'public, max-age=120');
    assert.equal(await response.text(),'');
    assert.equal(cancelled,true);
  } finally {globalThis.fetch=original;}
});
test('invalid platform cannot choose an arbitrary upstream or redirect target', async () => {
  const response = await (await handler)(context('?platform=https://untrusted.example'));
  assert.equal(response.status,400);
});
test('rejects unexpected upstream redirect hosts and uses the fixed release fallback', async () => {
  const original=globalThis.fetch;
  try {
    globalThis.fetch=async()=>new Response(null,{status:302,headers:{Location:'https://untrusted.example/payload.apk'}});
    const response=await(await handler)(context('?platform=tv'));
    assert.equal(response.headers.get('Location'),'https://github.com/lx-obsidian-labs/lx-obsidian-portal/releases/download/v1.0.10/VistaCinema-TV-v1.0.10.apk');
  } finally {globalThis.fetch=original;}
});
test('upstream outage retains a public GitHub fallback', async () => {
  const original=globalThis.fetch;
  try {
    globalThis.fetch=async()=>{throw new Error('Network unavailable');};
    const response=await(await handler)(context());
    assert.equal(response.status,302);
    assert.match(response.headers.get('Location'),/github\.com\/lx-obsidian-labs\/lx-obsidian-portal\/releases\/download\/v1\.0\.10\/VistaCinema-v1\.0\.10\.apk$/);
  } finally {globalThis.fetch=original;}
});
test('rejects mutation methods', async () => {
  assert.equal((await(await handler)(context('', 'POST'))).status,405);
});
