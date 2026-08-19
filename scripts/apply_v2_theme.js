const fs = require('fs');
const path = require('path');

const distDir = 'dist';
const pages = [
    'about.html',
    'advertise.html',
    'blog.html',
    'contact.html',
    'faq.html',
    'industries.html',
    'marketplace.html',
    'portfolio.html',
    'services.html',
    'synapse.html',
    'vista.html'
];

const themeCss = `
/* V2 Theme Variables */
:root {
  --violet: #7357ff;
  --violet-2: #9b8aff;
  --blue: #4a9cff;
  --line: rgba(255, 255, 255, 0.085);
  --line-2: rgba(255, 255, 255, 0.14);
  --shadow: 0 40px 100px rgba(0, 0, 0, .54);
  --shadow-violet: 0 28px 80px rgba(115, 87, 255, .12);
}

/* Make everything dark */
html { color-scheme: dark; }

/* Basic element styling using theme vars */
a { color: var(--violet); }
button { background: var(--violet); color: white; }
.hero-proof span::before { content: ''; width: 5px; height: 5px; border-radius: 50%; background: var(--violet-2); }

/* Loading bar */
.loading-bar__fill { width: 0; height: 100%; background: linear-gradient(90deg, var(--violet), var(--blue)); transition: width .3s ease; }

/* Chat widget */
.chat-widget__send { background: var(--violet); border: none; color: white; border-radius: 10px; padding: 8px 14px; cursor: pointer; font-size: .78rem; font-weight: 700; }

/* Starfield and cursor glow */
#starfield, .cursor-glow { display: none; }
@media (prefers-reduced-motion: no-preference) {
  #starfield, .cursor-glow { display: block; }
}

/* Product tab styling */
.product-tab.active { color: #fff; background: rgba(115,87,255,.07); box-shadow: inset 0 -2px 0 var(--violet); }

/* Product point indicator */
.product-point::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: var(--violet-2); box-shadow: 0 0 14px rgba(155,138,255,.48); }

/* Process step indicator */
.process-step:first-child::before { background: var(--violet-2); box-shadow: 0 0 15px rgba(155,138,255,.7); }

/* Pillar hover effect */
.pillar:hover { transform: translateY(-7px); border-color: rgba(115,87,255,.28); box-shadow: var(--shadow-violet); }

/* Final card */
.final-card { position: relative; overflow: hidden; padding: 76px; border: 1px solid rgba(115,87,255,.24); border-radius: 36px; background: radial-gradient(circle at 84% 48%, rgba(115,87,255,.24), transparent 27rem), linear-gradient(120deg, #12131c, #090a0f 73%); box-shadow: var(--shadow-violet); }
`;

function applyTheme(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;
    
    // 1. Add color-scheme meta tag if not present
    if (!content.includes('color-scheme')) {
        content = content.replace('<head>', '<head\n  <meta name="color-scheme" content="dark" />');
    }
    
    // 2. Add theme CSS - inject at start of <style> or create one
    if (content.match(/<style/)) {
        content = content.replace('<style', '<style\n/* V2 Theme */\n' + themeCss);
    } else {
        content = content.replace('</head>', '<style\n/* V2 Theme */\n' + themeCss + '\n</style>\n\n</head>');
    }
    
    // 3. Add starfield canvas and cursor glow before </body>
    const bodyEndIdx = content.lastIndexOf('</body>');
    if (bodyEndIdx > 0) {
        const starfieldHtml = '\n\n<canvas id="starfield" aria-hidden="true"></canvas>\n<div class="cursor-glow" id="cursorGlow" aria-hidden="true"></div>\n';
        content = content.substring(0, bodyEndIdx) + starfieldHtml + content.substring(bodyEndIdx);
    }
    
    // 4. Add cursor glow initialization script before closing </body>
    const script = '\n<script>\ndocument.addEventListener(\"DOMContentLoaded\", function() {\n  var cursorGlow = document.getElementById("cursorGlow");\n  if (cursorGlow && window.matchMedia("(pointer: fine)").matches) {\n    window.addEventListener("pointermove", function(e) {\n      cursorGlow.style.left = e.clientX + "px";\n      cursorGlow.style.top = e.clientY + "px";\n    }, { passive: true });\n  } else if (cursorGlow) {\n    cursorGlow.style.display = "none";\n  }\n});\n</script>\n';
    content = content.replace('</body>', script + '</body>');
    
    // 5. Ensure loading bar exists (check for loading-bar__fill)
    if (!content.includes('loadingBar__fill')) {
        const loadingBar = '\n<div class="loading-bar" aria-hidden="true">\n  <div class="loading-bar__fill" id="loadingBarFill"></div>\n</div>\n';
        // Insert after the announcement bar or at top of body
        const announcementIdx = content.indexOf('<div class="announcement"');
        if (announcementIdx > 0) {
            content = content.substring(0, announcementIdx) + loadingBar + content.substring(announcementIdx);
        } else {
            // Add at start of body
            const bodyStart = content.indexOf('<body');
            if (bodyStart > 0) {
                const bodyOpen = content.indexOf('>', bodyStart) + 1;
                content = content.substring(0, bodyOpen) + loadingBar + content.substring(bodyOpen);
            }
        }
    }
    
    // Write back only if changed
    if (content !== original) {
        fs.writeFileSync(filePath, content, 'utf8');
        console.log(`Updated: ${filePath}`);
    } else {
        console.log(`No changes needed: ${filePath}`);
    }
}

console.log('Applying V2 theme to all pages...\n');
for (const page of pages) {
    const filePath = path.join(distDir, page);
    if (fs.existsSync(filePath)) {
        applyTheme(filePath);
    } else {
        console.log(`Not found: ${filePath}`);
    }
}
console.log('\nDone!');