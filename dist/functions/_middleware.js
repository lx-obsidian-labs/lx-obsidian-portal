export async function onRequest(context) {
  const { request } = context;
  const url = new URL(request.url);

  // Redirect apex domain to www
  if (url.hostname === 'lxobsidianportal.co.za') {
    url.hostname = 'www.lxobsidianportal.co.za';
    return Response.redirect(url.toString(), 301);
  }

  // Pass through everything else
  return context.next();
}
