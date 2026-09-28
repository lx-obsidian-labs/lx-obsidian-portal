const fs = require('fs');
const pages = ['about.html', 'advertise.html', 'blog.html', 'contact.html', 'faq.html', 'industries.html', 'marketplace.html', 'portfolio.html', 'services.html', 'synapse.html', 'vista.html'];
for (const p of pages) {
  const h = fs.readFileSync('dist/' + p, 'utf8');
  const dc = (h.match(/data-color="[^"]*"/g) || []).length;
  const ac = (h.match(/app-card/g) || []).length;
  const am = (h.match(/app-modal/g) || []).length;
  const mc = (h.match(/marketplace-card/g) || []).length;
  console.log(p + ': data-color=' + dc + ' app-card=' + ac + ' app-modal=' + am + ' marketplace-card=' + mc);
}