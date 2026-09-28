import { error, handleOptions, json } from './_auth.js';

/**
 * POST /api/collect
 *
 * First-party, privacy-safe event ingest for the Vista Cinema Android app.
 *
 * Design rules the payload and the handler both honour:
 *  - No personally identifying values are ever stored: no email, no account id, no device
 *    advertising id, no exact coordinates. `country` is inferred server-side from the request
 *    edge (cf.country), so the client cannot leak it and a VPN user is measured by the
 *    destination, not the home address.
 *  - Event names are allowlisted, so a client build can never invent a chart.
 *  - Payloads are plain string maps, size-capped per key and per batch, so a bad release can
 *    only ever grow a moderately wide row, never an unreadable one.
 *  - Ingestion is fire-and-forget from the app: a failure here is a 4xx/5xx JSON, never a
 *    retry storm.
 *
 * Body shape (accepted inline or as a batch):
 *   { "events": [ { "name": "...", "appVersion": "1.0.6", "isGuest": false,
 *                   "locale": "en-ZA", "payload": { "mediaType": "movie" } } ] }
 */
export async function onRequest(context) {
  const { request } = context;

  if (request.method === 'OPTIONS') return handleOptions(request);
  if (request.method !== 'POST') return error('Method not allowed', 405, request);
  if (request.headers.get('Content-Type')?.includes('application/json') !== true) {
    return error('Content-Type must be application/json', 415, request);
  }

  const db = context.env.DB;
  if (!db) {
    return error('Analytics database is not provisioned yet', 503, request);
  }

  // Shared key set per deployment. Optional so the endpoint works locally before secrets exist,
  // required that the operator set one for production.
  const expectedKey = context.env.VISTA_ANALYTICS_KEY;
  if (expectedKey) {
    const provided = request.headers.get('X-Vista-Analytics-Key');
    if (!provided || provided !== expectedKey) {
      return error('Unauthorized', 401, request);
    }
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return error('Invalid JSON body', 400, request);
  }

  const incoming = Array.isArray(body?.events) ? body.events : (body?.name ? [body] : []);
  if (incoming.length === 0) return error('No events provided', 400, request);
  if (incoming.length > 50) return error('Too many events in one request (max 50)', 400, request);

  const country = request.cf?.country || null;
  const rows = [];
  let accepted = 0;

  for (const ev of incoming) {
    const row = normalizeEvent(ev, country);
    if (!row) continue;
    rows.push(row);
    accepted++;
  }

  if (rows.length === 0) return json({ accepted: 0, rejected: incoming.length }, 200, request);

  const stmts = rows.map((r) =>
    db.prepare(
      `INSERT INTO vista_events (event, app_version, is_guest, locale, country, payload, received_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      r.event,
      r.appVersion,
      r.isGuest ? 1 : 0,
      r.locale,
      r.country,
      JSON.stringify(r.payload),
      r.receivedAt
    )
  );

  await db.batch(stmts);

  return json({ accepted, rejected: incoming.length - accepted }, 200, request);
}

const ALLOWED_EVENTS = new Set([
  'app_open',
  'playback_started',
  'playback_completed',
  'share_whatsapp',
  'share_x',
  'share_facebook',
  'share_sheet',
  'motd_card_shown',
  'motd_opened',
  'motd_dismissed',
  'motd_reminder_delivered'
]);

function normalizeEvent(ev, country) {
  const name = typeof ev?.name === 'string' ? ev.name.trim() : '';
  if (!ALLOWED_EVENTS.has(name)) return null;

  const appVersion = typeof ev.appVersion === 'string' ? ev.appVersion.slice(0, 16) : null;
  const locale = typeof ev.locale === 'string' ? ev.locale.slice(0, 16) : null;

  let payload = {};
  if (ev.payload && typeof ev.payload === 'object' && !Array.isArray(ev.payload)) {
    for (const [key, value] of Object.entries(ev.payload)) {
      if (key.length > 24) continue;
      const flat = String(value).trim().slice(0, 200);
      if (flat) payload[key] = flat;
    }
  }

  return {
    event: name,
    appVersion,
    isGuest: ev.isGuest === true,
    locale,
    country,
    payload,
    receivedAt: Date.now()
  };
}