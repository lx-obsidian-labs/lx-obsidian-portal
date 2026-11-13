// Engineering: LX Obsidian Labs
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const handler = import('data:text/javascript;base64,'+Buffer.from(fs.readFileSync(path.join(__dirname,'../functions/_middleware.js'))).toString('base64')).then(m=>m.onRequest);
for(const [method,status] of [['GET',301],['HEAD',301],['POST',308],['OPTIONS',308]]) {
 test('canonical redirect preserves '+method,async()=>{
  const response=await (await handler)({request:new Request('https://lxobsidianportal.co.za/api/contact?q=1',{method})});
  assert.equal(response.status,status);
  assert.equal(response.headers.get('Location'),'https://www.lxobsidianportal.co.za/api/contact?q=1');
 });
}
test('canonical host passes through',async()=>{
 const expected=new Response('ok');
 assert.equal(await (await handler)({request:new Request('https://www.lxobsidianportal.co.za/'),next:()=>expected}),expected);
});
