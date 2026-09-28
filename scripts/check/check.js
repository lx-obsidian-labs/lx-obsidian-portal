const fs = require('fs');
const r = fs.readFileSync('index.html');
console.log('Length:', r.length);
console.log('Type:', typeof r);
console.log('Is Buffer:', Buffer.isBuffer(r));
if (Buffer.isBuffer(r)) {
  console.log('First 80 chars:', r.toString('utf8').substring(0, 80));
}