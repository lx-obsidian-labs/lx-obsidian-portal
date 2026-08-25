const fs = require('fs');
let h = fs.readFileSync('index.html', 'utf8');
const end = h.lastIndexOf('</footer>');
const insert = '\n\n    <div class="apk-download">\n      <a class="btn btn-primary" href="https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/VistaCinema-v1.0.0.apk" target="_blank" rel="noopener noreferrer">\n        Download VistaCinema v1.0.0 APK\n      </a>\n      <a class="btn btn-ghost" href="https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/vista-debug.apk" target="_blank" rel="noopener noreferrer">\n        Download vista-debug APK\n      </a>\n    </div>\n';
h = h.substring(0, end) + insert + h.substring(end);
fs.writeFileSync('index.html', h);
console.log('APK links added, length:', h.length);