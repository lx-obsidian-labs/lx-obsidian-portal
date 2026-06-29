const CACHE = 'lx-obsidian-v2';
const ASSETS = [
  '/',
  '/services',
  '/marketplace',
  '/portfolio',
  '/about',
  '/blog',
  '/contact',
  '/faq',
  '/404',
  '/css/style.css',
  '/css/variables.css',
  '/css/reset.css',
  '/css/layout.css',
  '/css/components.css',
  '/css/animations.css',
  '/css/features.css',
  '/css/responsive.css',
  '/js/navigation.js',
  '/js/scroll.js',
  '/js/animations.js',
  '/js/app.js',
  '/js/features.js',
  '/js/appstore.js',
  '/js/seo.js',
  '/js/ads.js',
  '/robots.txt',
  '/sitemap.xml',
  '/ads.txt'
];

self.addEventListener('install', function (e) {
  e.waitUntil(
    caches.open(CACHE).then(function (cache) {
      return cache.addAll(ASSETS);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(
        keys.filter(function (k) { return k !== CACHE; })
          .map(function (k) { return caches.delete(k); })
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', function (e) {
  if (e.request.method !== 'GET') return;

  e.respondWith(
    caches.match(e.request).then(function (cached) {
      if (cached) return cached;

      return fetch(e.request).then(function (response) {
        if (response && response.status === 200) {
          var copy = response.clone();
          caches.open(CACHE).then(function (cache) {
            cache.put(e.request, copy);
          });
        }
        return response;
      }).catch(function () {
        return caches.match('/404');
      });
    })
  );
});
