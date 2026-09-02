const CACHE = 'lx-obsidian-v19';
const ASSETS = [
  '/',
  '/css/lx.css',
  '/assets/generated/lx-hero-cluster-v3.webp',
  '/assets/generated/lx-step-discover-v3.webp',
  '/assets/generated/lx-step-engineer-v3.webp',
  '/assets/generated/lx-step-launch-v3.webp',
  '/assets/generated/lx-system-dashboard-v3.webp',
  '/assets/generated/lx-developer-dashboard-v3.webp',
  '/assets/generated/lx-home-hero-v2.webp',
  '/assets/generated/lx-hero-core.webp',
  '/assets/generated/lx-ai-automation.webp',
  '/assets/generated/lx-product-ui.webp',
  '/assets/vista-marketing-1.png',
  '/assets/synapse-screenshot-1.png',
  '/js/navigation.js',
  '/js/scroll.js',
  '/js/animations.js',
  '/js/app.js',
  '/js/features.js',
  '/js/seo.js',
  '/js/ux.js',
  '/js/experience.js',
  '/js/chat-widget.js',
  '/js/hero-scene.js',
  '/robots.txt',
  '/sitemap.xml'
];

self.addEventListener('install', function (e) {
  e.waitUntil(caches.open(CACHE).then(function (cache) { return cache.addAll(ASSETS); }));
  self.skipWaiting();
});

self.addEventListener('activate', function (e) {
  e.waitUntil(caches.keys().then(function (keys) {
    return Promise.all(keys.filter(function (k) { return k !== CACHE; }).map(function (k) { return caches.delete(k); }));
  }));
  self.clients.claim();
});

self.addEventListener('fetch', function (e) {
  if (e.request.method !== 'GET') return;
  var requestUrl = new URL(e.request.url);
  var isCritical = e.request.mode === 'navigate' || requestUrl.pathname.endsWith('.html') || requestUrl.pathname === '/css/lx.css' || requestUrl.pathname.includes('/assets/generated/lx-');

  if (isCritical) {
    e.respondWith(
      fetch(e.request).then(function (response) {
        if (response && response.status === 200 && response.type === 'basic') {
          // Don't cache HTML pages — they carry CSP headers that must stay fresh
          var isHtml = requestUrl.pathname.endsWith('.html') || requestUrl.pathname === '/';
          if (!isHtml) {
            var clone = response.clone();
            caches.open(CACHE).then(function (cache) { cache.put(e.request, clone); });
          }
        }
        return response;
      }).catch(function () {
        return caches.match(e.request).then(function (cached) {
          return cached || caches.match('/index.html');
        });
      })
    );
    return;
  }

  e.respondWith(
    caches.match(e.request).then(function (cached) {
      if (cached) return cached;
      return fetch(e.request).then(function (response) {
        if (response && response.status === 200 && response.type === 'basic') {
          // Don't cache HTML pages
          var isHtml = requestUrl.pathname.endsWith('.html');
          if (!isHtml) {
            var clone = response.clone();
            caches.open(CACHE).then(function (cache) { cache.put(e.request, clone); });
          }
        }
        return response;
      }).catch(function () {
        return caches.match('/index.html');
      });
    })
  );
});
