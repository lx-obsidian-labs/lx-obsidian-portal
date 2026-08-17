// Bump this whenever HTML/CSS behavior changes so production clients discard
// stale preview-era assets after the next deployment.
const CACHE = 'lx-obsidian-v7';
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

  // HTML and CSS must revalidate in production. A cache-first response here
  // can make Cloudflare deployments appear stuck on an older preview.
  var requestUrl = new URL(e.request.url);
  var isDocumentOrStyle = e.request.mode === 'navigate' ||
    requestUrl.pathname.endsWith('.html') ||
    requestUrl.pathname.endsWith('/css/style.css');

  if (isDocumentOrStyle) {
    e.respondWith(
      fetch(e.request).then(function (response) {
        if (response && response.status === 200 && response.type === 'basic') {
          caches.open(CACHE).then(function (cache) {
            cache.put(e.request, response.clone());
          });
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
          var copy = response.clone();
          caches.open(CACHE).then(function (cache) {
            cache.put(e.request, copy);
          });
        }
        return response;
      }).catch(function () {
        return caches.match('/index.html');
      });
    })
  );
});
