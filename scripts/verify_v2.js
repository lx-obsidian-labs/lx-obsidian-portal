var fs = require('fs');
var files = ['index.html', 'vista.html'];
for (var i = 0; i < files.length; i++) {
  var h = fs.readFileSync('dist/' + files[i], 'utf8');
  var hasV2 = h.includes('color-scheme') && h.includes('--violet') && h.includes('id="starfield"') && h.includes('id="cursorGlow"') && h.includes('loading-bar__fill');
  var noBroken = !h.includes('/deploy/');
  var apkCount = (h.match(/\.apk/gi) || []).length;
  console.log(files[i] + ': V2=' + hasV2 + ' NoBroken=' + noBroken + ' APK=' + apkCount);
}