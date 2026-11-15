import { writeFile } from 'node:fs/promises';
import { deflateSync } from 'node:zlib';

// Generate a self-contained Vista icon at build time: dark cinema background and a red V.
const size = 1024;
const raw = Buffer.alloc((size * 3 + 1) * size);
for (let y = 0; y < size; y++) {
  for (let x = 0; x < size; x++) {
    const offset = y * (size * 3 + 1) + 1 + x * 3;
    const glow = Math.max(0, 1 - Math.hypot(x - 512, y - 440) / 650);
    const left = x >= 190 + (y - 220) * 0.4 && x <= 340 + (y - 220) * 0.4;
    const right = x >= 684 - (y - 220) * 0.4 && x <= 834 - (y - 220) * 0.4;
    const mark = y >= 220 && y <= 800 && (left || right);
    raw[offset] = mark ? Math.round(255 - (y - 220) * 0.07) : Math.round(10 + glow * 20);
    raw[offset + 1] = mark ? 16 : 10;
    raw[offset + 2] = mark ? 35 : 13;
  }
}
function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const name = Buffer.from(type), length = Buffer.alloc(4), crc = Buffer.alloc(4);
  length.writeUInt32BE(data.length); crc.writeUInt32BE(crc32(Buffer.concat([name, data])));
  return Buffer.concat([length, name, data, crc]);
}
const header = Buffer.alloc(13);
header.writeUInt32BE(size, 0); header.writeUInt32BE(size, 4); header[8] = 8; header[9] = 2;
await writeFile(new URL('../Vista/Assets.xcassets/AppIcon.appiconset/AppIcon.png', import.meta.url),
  Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]), chunk('IHDR', header), chunk('IDAT', deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
console.log('Generated Vista iOS app icon.');
