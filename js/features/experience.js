void(function(){'use strict';
  var body=document.body;
  if(!body||body.dataset.lxExperience==='1')return;
  body.dataset.lxExperience='1';

  var visualMap={
    'page-services':['assets/generated/lx-ai-automation.webp','SYSTEMS','AUTOMATION'],
    'page-about':['assets/generated/lx-hero-core.webp','OBSIDIAN','ENGINEERING'],
    'page-contact':['assets/generated/lx-product-ui.webp','DISCOVERY','DELIVERY'],
    'page-portfolio':['assets/generated/lx-product-ui.webp','PRODUCT','PROOF'],
    'page-blog':['assets/generated/lx-ai-automation.webp','RESEARCH','INSIGHTS'],
    'page-industries':['assets/generated/lx-hero-core.webp','SECTORS','SYSTEMS'],
    'page-partners':['assets/generated/lx-product-ui.webp','NETWORK','PARTNERS'],
    'page-advertise':['assets/generated/lx-product-ui.webp','AUDIENCE','GROWTH'],
    'page-faq':['assets/generated/lx-hero-core.webp','KNOWLEDGE','SUPPORT']
  };

  function bodyClassKey(){for(var k in visualMap){if(body.classList.contains(k))return k;}return null;}
  function makeStage(src,a,b){
    var fig=document.createElement('figure');fig.className='lx-visual-stage';
    var img=document.createElement('img');img.src=src;img.alt='LX Obsidian Labs technical systems artwork';img.loading='eager';img.decoding='async';
    var meta=document.createElement('figcaption');meta.className='lx-visual-stage__meta';meta.innerHTML='<span>'+a+'</span><span>'+b+'</span><span>LIVE // 01</span>';
    fig.appendChild(img);fig.appendChild(meta);return fig;
  }

  var key=bodyClassKey();
  if(key){var hero=document.getElementById('main-content');if(hero){var c=hero.querySelector('.container');if(c&&!c.querySelector('.lx-visual-stage')){var v=visualMap[key];c.appendChild(makeStage(v[0],v[1],v[2]));}}}

  if(body.classList.contains('page-services')){
    var serviceImgs=['assets/generated/lx-ai-automation.webp','assets/generated/lx-product-ui.webp','assets/generated/lx-product-ui.webp','assets/generated/lx-hero-core.webp','assets/generated/lx-hero-core.webp','assets/generated/lx-product-ui.webp'];
    document.querySelectorAll('.service-block__visual').forEach(function(el,i){if(el.querySelector('img'))return;var img=document.createElement('img');img.src=serviceImgs[i%serviceImgs.length];img.alt='LX service capability visual';img.loading=i<2?'eager':'lazy';img.decoding='async';el.prepend(img);});
  }

  function addProductVisual(pageClass,src,alt){
    if(!body.classList.contains(pageClass))return;
    var hero=document.querySelector('.syn-hero .container');if(!hero||hero.querySelector('.lx-product-visual'))return;
    var wrap=document.createElement('figure');wrap.className='lx-product-visual';
    var img=document.createElement('img');img.src=src;img.alt=alt;img.loading='eager';img.decoding='async';wrap.appendChild(img);hero.appendChild(wrap);
  }
  addProductVisual('page-synapse','assets/synapse-screenshot-1.png','Synapse AI browser automation interface');
  addProductVisual('page-vista','assets/vista-marketing-1.png','Vista Cinema application interface');

  function svgData(markup){return 'data:image/svg+xml;charset=UTF-8,'+encodeURIComponent(markup);}
  var defs='<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#11131c"/><stop offset=".48" stop-color="#080910"/><stop offset="1" stop-color="#020204"/></linearGradient><linearGradient id="v" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#a343ff"/><stop offset=".5" stop-color="#6a43ff"/><stop offset="1" stop-color="#35bfff"/></linearGradient><radialGradient id="gl"><stop stop-color="#7d48ff" stop-opacity=".42"/><stop offset="1" stop-color="#241059" stop-opacity="0"/></radialGradient><filter id="blur"><feGaussianBlur stdDeviation="24"/></filter><filter id="shadow"><feDropShadow dx="0" dy="22" stdDeviation="24" flood-color="#5d2fff" flood-opacity=".18"/></filter></defs>';

  var heroSvg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 1000">'+defs+'<rect width="1000" height="1000" fill="#020204"/><circle cx="500" cy="540" r="370" fill="url(#gl)" filter="url(#blur)"/><g opacity=".28" stroke="#7456ff" fill="none"><circle cx="500" cy="580" r="315"/><circle cx="500" cy="580" r="245"/><path d="M100 780H900M180 835H820"/></g><g filter="url(#shadow)" transform="translate(500 475)"><g transform="rotate(-12)"><rect x="-220" y="-220" width="180" height="180" rx="20" fill="url(#g)" stroke="#8b62ff" stroke-opacity=".55"/><rect x="-40" y="-280" width="190" height="190" rx="20" fill="url(#g)" stroke="#5f8cff" stroke-opacity=".55"/><rect x="130" y="-135" width="180" height="180" rx="20" fill="url(#g)" stroke="#5f8cff" stroke-opacity=".45"/><rect x="-285" y="-25" width="190" height="190" rx="20" fill="url(#g)" stroke="#9a4dff" stroke-opacity=".55"/><rect x="-80" y="-55" width="200" height="200" rx="20" fill="url(#g)" stroke="url(#v)" stroke-width="3"/><rect x="100" y="70" width="190" height="190" rx="20" fill="url(#g)" stroke="#6d56ff" stroke-opacity=".55"/><rect x="-220" y="150" width="190" height="190" rx="20" fill="url(#g)" stroke="#4b7cff" stroke-opacity=".45"/></g></g><ellipse cx="500" cy="825" rx="240" ry="42" fill="none" stroke="url(#v)" stroke-opacity=".55"/><ellipse cx="500" cy="825" rx="140" ry="24" fill="none" stroke="#8b3dff" stroke-opacity=".4"/><circle cx="500" cy="825" r="9" fill="#5fcfff"/></svg>';

  var monitorSvg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 760 470">'+defs+'<rect width="760" height="470" rx="28" fill="#05060a" stroke="#2e3140"/><text x="42" y="58" fill="#c9cbd3" font-family="monospace" font-size="20" letter-spacing="3">LX SYSTEM NODE</text><circle cx="670" cy="50" r="6" fill="#45e6a8"/><text x="688" y="57" fill="#45e6a8" font-family="monospace" font-size="14">ONLINE</text><path d="M42 92H718" stroke="#222531"/><g font-family="monospace"><text x="52" y="145" fill="#787d8c" font-size="15">CPU</text><text x="52" y="188" fill="#fff" font-size="42">28%</text><text x="215" y="145" fill="#787d8c" font-size="15">MEMORY</text><text x="215" y="188" fill="#fff" font-size="42">62%</text><text x="410" y="145" fill="#787d8c" font-size="15">NETWORK</text><text x="410" y="188" fill="#fff" font-size="42">1.3 GB/s</text></g><rect x="42" y="225" width="430" height="170" rx="14" fill="#080a10" stroke="#1c2030"/><polyline points="62,350 100,320 140,340 175,290 220,330 265,270 315,310 350,250 395,285 448,240" fill="none" stroke="url(#v)" stroke-width="4"/><g transform="translate(555 290)"><polygon points="0,-65 58,-32 58,32 0,65 -58,32 -58,-32" fill="#0a0c14" stroke="#7a56ff"/><polygon points="0,-34 30,-17 30,17 0,34 -30,17 -30,-17" fill="url(#v)" opacity=".65"/></g></svg>';

  function stepSvg(type){
    var shape='';
    if(type==='cube')shape='<g transform="translate(320 260) rotate(-12)" filter="url(#shadow)"><rect x="-135" y="-135" width="270" height="270" rx="70" fill="#06070b" stroke="#5e6070" stroke-width="6"/><rect x="-68" y="-68" width="136" height="136" rx="38" fill="#020204" stroke="url(#v)" stroke-width="8"/></g>';
    if(type==='sphere')shape='<g transform="translate(320 260)" filter="url(#shadow)" fill="#07080d" stroke="#656878" stroke-width="4"><circle r="155"/><path d="M-155 0H155M-120-95L120 95M120-95L-120 95M0-155V155"/><circle r="80" fill="none" stroke="#8c56ff"/></g>';
    if(type==='core')shape='<g transform="translate(320 260)" filter="url(#shadow)"><polygon points="0,-165 145,-80 145,80 0,165 -145,80 -145,-80" fill="#06070b" stroke="#747786" stroke-width="5"/><polygon points="0,-88 78,-43 78,43 0,88 -78,43 -78,-43" fill="#020204" stroke="url(#v)" stroke-width="8"/><circle r="25" fill="#7b4eff"/></g>';
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 520">'+defs+'<rect width="640" height="520" fill="#050609"/><circle cx="320" cy="285" r="210" fill="url(#gl)" filter="url(#blur)"/>'+shape+'<ellipse cx="320" cy="445" rx="150" ry="22" fill="none" stroke="#754dff" stroke-opacity=".45"/></svg>';
  }

  var codeSvg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 760 470">'+defs+'<rect width="760" height="470" rx="28" fill="#05060a" stroke="#2b2e3a"/><text x="42" y="58" fill="#858a99" font-family="monospace" font-size="18">LX_OBSIDIAN_CORE.JS</text><circle cx="678" cy="48" r="6" fill="#45e6a8"/><g font-family="monospace" font-size="20"><text x="62" y="130" fill="#8b4dff">const</text><text x="132" y="130" fill="#d8dbe5"> lx = new LXObsidian();</text><text x="62" y="170" fill="#6fa8ff">lx.init</text><text x="138" y="170" fill="#d8dbe5">({</text><text x="96" y="210" fill="#a6abb8">mode:</text><text x="174" y="210" fill="#4ee0ae">'performance'</text><text x="96" y="250" fill="#a6abb8">ai:</text><text x="140" y="250" fill="#8b4dff">true</text><text x="62" y="290" fill="#d8dbe5">});</text></g><rect x="420" y="112" width="280" height="220" rx="16" fill="#080a10" stroke="#1c2030"/><text x="448" y="148" fill="#8a8f9d" font-family="monospace" font-size="14">PERFORMANCE</text><polyline points="448,285 480,260 512,275 548,220 585,250 615,205 670,230" fill="none" stroke="url(#v)" stroke-width="4"/><rect x="42" y="370" width="658" height="58" rx="14" fill="#080a10" stroke="#1c2030"/><circle cx="72" cy="399" r="6" fill="#45e6a8"/><text x="92" y="406" fill="#9ca1af" font-family="monospace" font-size="15">ENGINE READY // AWAITING COMMAND</text></svg>';

  if(body.classList.contains('page-home-v2')){
    var replacements={
      'lx-cube-swarm.webp':svgData(heroSvg),
      'lx-system-monitor.webp':svgData(monitorSvg),
      'lx-hollow-cube.webp':svgData(stepSvg('cube')),
      'lx-geodesic-sphere.webp':svgData(stepSvg('sphere')),
      'lx-geometric-core.webp':svgData(stepSvg('core')),
      'lx-code-dashboard.webp':svgData(codeSvg)
    };
    document.querySelectorAll('img[src]').forEach(function(img){
      Object.keys(replacements).some(function(name){if(img.getAttribute('src').indexOf(name)!==-1){img.src=replacements[name];return true;}return false;});
    });
  }

  document.querySelectorAll('.portfolio-card,.knowledge-card,.card').forEach(function(card,i){card.style.setProperty('--lx-index',i+1);});
  var sections=document.querySelectorAll('main~section,.section');
  sections.forEach(function(section,i){if(i>0)section.setAttribute('data-lx-section',String(i).padStart(2,'0'));});
})();
