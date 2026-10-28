/* LX Obsidian Labs — safe network-first service worker.
   Never return HTML in response to CSS, JS, image or font requests. */
const CACHE = 'lx-obsidian-v21';
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE));
  self.skipWaiting();
});
self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const names = await caches.keys();
    await Promise.all(names.filter(name => name.startsWith('lx-obsidian-') && name !== CACHE).map(name => caches.delete(name)));
    await self.clients.claim();
  })());
});
self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.startsWith('/api/')) return;
  const isDocument = request.mode === 'navigate' || request.destination === 'document';
  event.respondWith((async () => {
    try {
      const response = await fetch(request, {cache:'no-store'});
      // HTML is never stored. Assets may be cached as an offline fallback.
      if (!isDocument && response.ok && response.type === 'basic') {
        const cache = await caches.open(CACHE);
        event.waitUntil(cache.put(request, response.clone()));
      }
      return response;
    } catch (error) {
      if (!isDocument) {
        const cached = await caches.match(request);
        if (cached) return cached;
      }
      return Response.error();
    }
  })());
});
