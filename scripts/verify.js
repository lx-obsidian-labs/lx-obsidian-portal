const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const dist = path.resolve(__dirname, '../dist');
const required = ['index.html', 'pdf.html', '404.html', '_redirects', 'css/lx.css'];
for (const name of required) {
  if (!fs.existsSync(path.join(dist, name))) throw Error('Missing required asset: ' + name);
}
const home = fs.readFileSync(path.join(dist, 'index.html'), 'utf8');
const routes = fs.readFileSync(path.join(dist, '_redirects'), 'utf8');
if (!home.includes('href="/pdf"')) throw Error('Homepage PDF Studio link missing');
if (!fs.existsSync(path.join(dist, 'pdf/index.html'))) throw Error('PDF Studio directory route missing');
function check(dir) {
  for (const item of fs.readdirSync(dir, {withFileTypes:true})) {
    const file = path.join(dir, item.name);
    if (item.isDirectory()) check(file);
    else if (file.endsWith('.js') && !file.endsWith('.min.js')) {
      const result = cp.spawnSync(process.execPath, ['--check', file], {encoding:'utf8'});
      if (result.status !== 0) throw Error('JavaScript syntax error in ' + file + ': ' + result.stderr);
    }
  }
}
check(dist);
console.log('Release verification passed: required assets, PDF route, homepage link and JS syntax');
