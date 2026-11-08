// Resolve a public GitHub Release asset to its CDN URL. Never buffer or proxy APK bytes.
const release = {
  android: { id: '723495986', filename: 'VistaCinema-v1.0.10.apk' },
  tv: { id: '723495987', filename: 'VistaCinema-TV-v1.0.10.apk' }
};
const repository = 'lx-obsidian-labs/lx-obsidian-portal';
const version = 'v1.0.10';

export async function onRequest(context) {
  const request = context.request;
  if (!['GET', 'HEAD'].includes(request.method)) return new Response('Method not allowed', { status: 405, headers: { Allow: 'GET, HEAD' } });
  const url = new URL(request.url);
  const platform = url.searchParams.get('platform') || 'android';
  const asset = Object.prototype.hasOwnProperty.call(release, platform) ? release[platform] : null;
  if (!asset) return new Response('Choose android or tv.', { status: 400 });
  const key = new Request(`${url.origin}/api/vista-download?platform=${platform}`);
  const cache = globalThis.caches?.default;
  const cached = cache ? await cache.match(key) : null;
  if (cached) return cached;
  const fallback = `https://github.com/${repository}/releases/download/${version}/${asset.filename}`;
  try {
    const upstream = await fetch(`https://api.github.com/repos/${repository}/releases/assets/${asset.id}`, {
      headers: { Accept: 'application/octet-stream', 'User-Agent': 'LX-Obsidian-Vista-Downloads' },
      redirect: 'manual', signal: AbortSignal.timeout(10_000)
    });
    const location = upstream.headers.get('Location');
    if (upstream.body) await upstream.body.cancel();
    const target = location ? new URL(location) : null;
    if (upstream.status !== 302 || target?.protocol !== 'https:' ||
        !['release-assets.githubusercontent.com', 'objects.githubusercontent.com', 'github-releases.githubusercontent.com'].includes(target.hostname)) {
      return Response.redirect(fallback, 302);
    }
    const response = new Response(null, { status: 302, headers: {
      Location: target.toString(), 'Cache-Control': 'public, max-age=120', 'X-Content-Type-Options': 'nosniff'
    } });
    if (cache) context.waitUntil(cache.put(key, response.clone()));
    return response;
  } catch {
    return Response.redirect(fallback, 302);
  }
}
