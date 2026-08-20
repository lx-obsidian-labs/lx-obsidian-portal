// Bump this whenever HTML/CSS behavior changes so production clients discard
// stale preview-era assets after the next deployment.
const CACHE = 'lx-obsidian-v11';
const ASSETS = [
  '/',
  '/index.html',
  '/about.html',
  '/services.html',
  '/portfolio.html',
  '/contact.html',
  '/blog.html',
  '/faq.html',
  '/marketplace.html',
  '/synapse.html',
  '/industries.html',
  '/partners.html',
  '/css/style.css',
  '/assets/generated/lx-hero-core.webp',
  '/assets/generated/lx-ai-automation.webp',
  '/assets/generated/lx-product-ui.webp',
  '/assets/synapse-screenshot-1.png',
  '/assets/vista-marketing-1.png',
  '/js/experience.js',
  '/js/navigation.js',
  '/js/scroll.js',
  '/js/animations.js',
  '/js/app.js',
  '/js/features.js',
  '/js/seo.js',
  '/js/ux.js',
  '/robots.txt',
  '/sitemap.xml'
];
self.addEventListener('install',function(e){e.waitUntil(caches.open(CACHE).then(function(cache){return cache.addAll(ASSETS);}));self.skipWaiting();});
self.addEventListener('activate',function(e){e.waitUntil(caches.keys().then(function(keys){return Promise.all(keys.filter(function(k){return k!==CACHE;}).map(function(k){return caches.delete(k);}));}));self.clients.claim();});
self.addEventListener('fetch',function(e){
  if(e.request.method!=='GET')return;
  var u=new URL(e.request.url);
  var networkFirst=e.request.mode==='navigate'||u.pathname.endsWith('.html')||u.pathname.endsWith('/css/style.css')||u.pathname.endsWith('/js/experience.js');
  if(networkFirst){e.respondWith(fetch(e.request).then(function(r){if(r&&r.status===200&&r.type==='basic')caches.open(CACHE).then(function(c){c.put(e.request,r.clone());});return r;}).catch(function(){return caches.match(e.request).then(function(cached){return cached||caches.match('/index.html');});}));return;}
  e.respondWith(caches.match(e.request).then(function(cached){if(cached)return cached;return fetch(e.request).then(function(r){if(r&&r.status===200&&r.type==='basic')caches.open(CACHE).then(function(c){c.put(e.request,r.clone());});return r;}).catch(function(){return caches.match('/index.html');});}));
});
