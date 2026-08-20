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

  function bodyClassKey(){
    for(var k in visualMap){if(body.classList.contains(k))return k;}
    return null;
  }

  function makeStage(src,a,b){
    var fig=document.createElement('figure');fig.className='lx-visual-stage';
    var img=document.createElement('img');img.src=src;img.alt='LX Obsidian Labs technical systems artwork';img.loading='eager';img.decoding='async';
    var meta=document.createElement('figcaption');meta.className='lx-visual-stage__meta';meta.innerHTML='<span>'+a+'</span><span>'+b+'</span><span>LIVE // 01</span>';
    fig.appendChild(img);fig.appendChild(meta);return fig;
  }

  var key=bodyClassKey();
  if(key){
    var hero=document.getElementById('main-content');
    if(hero){var c=hero.querySelector('.container');if(c&&!c.querySelector('.lx-visual-stage')){var v=visualMap[key];c.appendChild(makeStage(v[0],v[1],v[2]));}}
  }

  if(body.classList.contains('page-services')){
    var serviceImgs=['assets/generated/lx-ai-automation.webp','assets/generated/lx-product-ui.webp','assets/generated/lx-product-ui.webp','assets/generated/lx-hero-core.webp','assets/generated/lx-hero-core.webp','assets/generated/lx-product-ui.webp'];
    document.querySelectorAll('.service-block__visual').forEach(function(el,i){
      if(el.querySelector('img'))return;
      var img=document.createElement('img');img.src=serviceImgs[i%serviceImgs.length];img.alt='LX service capability visual';img.loading=i<2?'eager':'lazy';img.decoding='async';el.prepend(img);
    });
  }

  function addProductVisual(pageClass,src,alt){
    if(!body.classList.contains(pageClass))return;
    var hero=document.querySelector('.syn-hero .container');if(!hero||hero.querySelector('.lx-product-visual'))return;
    var wrap=document.createElement('figure');wrap.className='lx-product-visual';
    var img=document.createElement('img');img.src=src;img.alt=alt;img.loading='eager';img.decoding='async';wrap.appendChild(img);hero.appendChild(wrap);
  }
  addProductVisual('page-synapse','assets/synapse-screenshot-1.png','Synapse AI browser automation interface');
  addProductVisual('page-vista','assets/vista-marketing-1.png','Vista Cinema application interface');

  document.querySelectorAll('.portfolio-card,.knowledge-card,.card').forEach(function(card,i){card.style.setProperty('--lx-index',i+1);});

  var sections=document.querySelectorAll('main~section,.section');
  sections.forEach(function(section,i){if(i>0)section.setAttribute('data-lx-section',String(i).padStart(2,'0'));});
})();
