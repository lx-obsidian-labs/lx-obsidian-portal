// Moves the VELORA ticker band below the hero <section> on services + portfolio
// so the page opens with its h1 (uniform with every other page).
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

function moveBand(file, bandComment, anchorComment) {
  const p = path.join(ROOT, file);
  let html = fs.readFileSync(p, 'utf8');
  const start = html.indexOf(bandComment);
  if (start === -1) { console.log(file + ': band comment not found'); return; }
  // Band block runs from the comment to its closing </section>
  const endTag = '</section>';
  const end = html.indexOf(endTag, start);
  if (end === -1) { console.log(file + ': band end not found'); return; }
  const block = html.slice(start, end + endTag.length);
  if (html.indexOf(block, html.indexOf(block) + 1) !== -1) { console.log(file + ': ambiguous, skipped'); return; }
  // Remove it
  let out = html.slice(0, start) + html.slice(end + endTag.length);
  // Collapse the now-empty gap (leave a single blank line at removal point)
  out = out.replace(/\n{3,}/g, '\n\n');
  // Insert before the anchor comment (or after the hero section if absent)
  const anchor = out.indexOf(anchorComment);
  if (anchor === -1) { console.log(file + ': anchor not found'); return; }
  out = out.slice(0, anchor) + block + '\n\n' + out.slice(anchor);
  fs.writeFileSync(p, out);
  console.log(file + ': band moved below hero');
}

moveBand('services.html', '<!-- VELORA:TICKER-BLUR -->', '  <!-- AI & AUTOMATION -->');
moveBand('portfolio.html', '<!-- VELORA:TICKER-LINKS -->', '  <!-- PRODUCTS & PLATFORMS -->');
