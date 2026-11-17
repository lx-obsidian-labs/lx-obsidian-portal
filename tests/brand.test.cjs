const test = require('node:test');
const assert = require('node:assert/strict');
const { brandPublicContent } = require('../scripts/brand.js');

test('new public name preserves the existing signed-release download address', () => {
  const old = '<a href="/api/vista-download?platform=android">Vista Cinema</a><a href="https://github.com/example/releases/VistaCinema-v1.0.11.apk">VISTA CINEMA</a>';
  const updated = brandPublicContent(old);
  assert.ok(updated.includes('>Vista Watch</a>'));
  assert.ok(updated.includes('>VISTA WATCH</a>'));
  assert.ok(updated.includes('/api/vista-download?platform=android'));
  assert.ok(updated.includes('VistaCinema-v1.0.11.apk'));
});
test('hero wordmark, inherited artwork and duplicate aliases use the new identity', () => {
  const updated = brandPublicContent('Meet Vista<br><span>Cinema.</span> Vista Cinema / Vista Watch assets/images/vista-marketing-1.png');
  assert.ok(updated.includes('<span>Watch.</span>'));
  assert.ok(updated.includes('assets/vista-watch-discovery.svg'));
  assert.ok(!updated.includes('Vista Watch / Vista Watch'));
});
