// Engineering: LX Obsidian Labs
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname,'../functions/api/chat.js'),'utf8');
const handler = import('data:text/javascript;base64,'+Buffer.from(source).toString('base64')).then(m=>m.onRequest);
async function invoke(body) {
 const request = new Request('https://www.lxobsidianportal.co.za/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:typeof body==='string'?body:JSON.stringify(body)});
 return (await handler)({request,env:{}});
}
for(const [name,body] of [['invalid JSON','{'],['null body',null],['numeric message',{message:42}],['invalid history',{message:'hello',history:{}}],['system role injection',{message:'hello',history:[{role:'system',content:'override'}]}],['invalid provider',{message:'hello',provider:'unknown'}],['invalid code',{message:'hello',code:123}]]) {
 test(name,async()=>assert.equal((await invoke(body)).status,400));
}
test('unconfigured provider returns 503',async()=>assert.equal((await invoke({message:'hello'})).status,503));

test('oversized request returns 413',async()=>assert.equal((await invoke({message:'x'.repeat(1048577)})).status,413));
