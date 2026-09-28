import { all, get } from './_db.js';
import { error, handleOptions, json, requireAuth } from './_auth.js';

/**
 * GET /api/vista-stats
 *
 * Aggregates only: there is nothing personal in the analytics database to expose, so the read
 * side returns counts and nothing else. Auth accepts one of:
 *   - X-Vista-Stats-Key: <VISTA_STATS_KEY secret>   (the simple, app-local option)
 *   - Authorization: Bearer <JWT with role "admin"> (reuses the portal's JWT_SECRET)
 */
export async function onRequest(context) {
  const { request, env } = context;

  if (request.method === 'OPTIONS') return handleOptions(request);
  if (request.method !== 'GET') return error('Method not allowed', 405, request);

  const db = context.env.DB;
  if (!db) return error('Analytics database is not provisioned yet', 503, request);
  if (!await isAuthorized(request, env)) return error('Unauthorized', 401, request);

  const url = new URL(request.url);
  const days = Math.min(Math.max(parseInt(url.searchParams.get('days') || '30', 10) || 30, 1), 90);
  const since = Date.now() - days * 24 * 60 * 60 * 1000;

  const total = await get(
    context, 'SELECT COUNT(*) AS n FROM vista_events'
  );
  const byEvent = await all(
    context, 'SELECT event, COUNT(*) AS n FROM vista_events GROUP BY event ORDER BY n DESC'
  );
  const byCountry = await all(
    context, 'SELECT country, COUNT(*) AS n FROM vista_events WHERE country IS NOT NULL AND country != ? GROUP BY country ORDER BY n DESC',
    ''
  );
  const byDay = await all(
    context, 'SELECT DATE(received_at / 1000, \'unixepoch\') AS day, COUNT(*) AS n FROM vista_events WHERE received_at >= ? GROUP BY day ORDER BY day ASC',
    since
  );
  const byVersion = await all(
    context, 'SELECT app_version, COUNT(*) AS n FROM vista_events WHERE app_version IS NOT NULL GROUP BY app_version ORDER BY n DESC'
  );
  const byGuest = await all(
    context, 'SELECT is_guest, COUNT(*) AS n FROM vista_events GROUP BY is_guest'
  );

  return json({
    days,
    totalEvents: total?.n ?? 0,
    perEvent: byEvent?.results ?? [],
    perCountry: byCountry?.results ?? [],
    perDay: byDay?.results ?? [],
    perAppVersion: byVersion?.results ?? [],
    perGuestState: byGuest?.results ?? []
  }, 200, request);
}

async function isAuthorized(request, env) {
  const statsKey = env.VISTA_STATS_KEY;
  if (statsKey) {
    const header = request.headers.get('X-Vista-Stats-Key');
    if (header && header === statsKey) return true;
  }
  if (env.JWT_SECRET) {
    const user = await requireAuth(request, env.JWT_SECRET);
    if (user && user.role === 'admin') return true;
  }
  return false;
}