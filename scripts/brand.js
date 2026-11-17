const brand = require('../brand.json');

// One published identity across inherited page templates, shared navigation and dynamic UI.
// Literal replacements deliberately leave historical installer filenames and endpoint IDs intact.
function brandPublicContent(source) {
  return source
    .replaceAll('Vista Cinema / Vista Watch', brand.name)
    .replaceAll('Vista Cinema, also known as Vista Watch', brand.name)
    .replaceAll('Vista Cinema', brand.name)
    .replaceAll('VISTA CINEMA', brand.wordmark)
    .replaceAll('Meet Vista<br><span>Cinema.</span>', 'Meet Vista<br><span>Watch.</span>')
    .replaceAll('assets/images/vista-marketing-1.png', 'assets/vista-watch-discovery.svg')
    .replaceAll('assets/images/vista-marketing-2.png', 'assets/vista-watch-experience.svg')
    .replaceAll('assets/vista-marketing-1.png', 'assets/vista-watch-discovery.svg')
    .replaceAll('assets/vista-marketing-2.png', 'assets/vista-watch-experience.svg')
    .replaceAll('vista-cinema-install-guide.pdf', 'vista-watch-install-guide.pdf')
    .replaceAll('>VC</span>', '>VW</span>');
}

module.exports = { brand, brandPublicContent };
