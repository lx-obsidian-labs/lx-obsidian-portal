// Deeper structure audit: body class, section patterns, heading hierarchy
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');
const pages = fs.readdirSync(root).filter(f => f.endsWith('.html'));

for (const file of pages) {
  const src = fs.readFileSync(path.join(root, file), 'utf8');
  const bodyClass = (src.match(/<body([^>]*)>/) || [])[1] || '(none)';
  const sections = [...src.matchAll(/<section([^>]*)>/g)].map(m => {
    const cls = (m[1].match(/class="([^"]*)"/) || [])[1] || '';
    const id = (m[1].match(/id="([^"]*)"/) || [])[1] || '';
    return cls.split(/\s+/).filter(Boolean)[0] || (id ? '#' + id : '(noclass)');
  });
  const heads = [...src.matchAll(/<h([1-3])[^>]*>([\s\S]*?)<\/h\1>/g)].map(m => {
    const t = m[2].replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim().slice(0, 48);
    return `h${m[1]}:${t}`;
  });
  const heroLike = /saas-hero|page-hero|inner-hero|vc-hero|syn-hero|hero/.test(src);
  console.log(`\n### ${file}`);
  console.log(`  body: ${bodyClass.trim()}`);
  console.log(`  sections: ${sections.join(' | ')}`);
  console.log(`  headings: ${heads.slice(0, 14).join(' | ')}`);
}
