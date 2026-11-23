// LX Insider beta-tester recruitment. Validates by default; --send broadcasts one wave.
// Sends to installed LX Obsidian Labs apps (Vista Watch), never to a purchased or scraped list.
import { pathToFileURL } from 'node:url';
import { createRequire } from 'node:module';
import { readFile, appendFile, mkdir } from 'node:fs/promises';
import { dirname, join } from 'node:path';

export const PRODUCT_URL = 'https://www.lxobsidianportal.co.za/pocket-business';
export const APPLY_URL = 'https://www.lxobsidianportal.co.za/insider';
export const PLAY_TESTING_URL = 'https://play.google.com/apps/testing/com.obsidianlabs.lxpocketbusiness';
export const SEND_LOG_FILE = 'campaign/fcm-send-log.jsonl';
export const FCM_ENDPOINT = 'https://fcm.googleapis.com/v1/projects/vista-cinema/messages:send';

export const AUDIENCES = {
  za: 'vista_updates_za',
  row: 'vista_updates_row',
  global: 'vista_updates'
};

// Claims the programme cannot make without written management approval.
const FORBIDDEN_CLAIMS = [
  /\bguarantee/i,
  /\bsalary\b/i,
  /\bjob offer\b/i,
  /\bfull[- ]time\b/i,
  /\bpositive review/i,
  /\bfive[- ]star\b/i,
  /\b5[- ]star\b/i,
  /\bR\s?\d/,
  /\bfree forever\b/i
];

export const WAVES = {
  1: {
    id: 'lx-insider-recruit-2026-w1',
    ttl: '259200s',
    title: 'LX Insider: test early, shape what we build',
    body: 'Vista Watch users get the first invite to LX Insider, the beta testing community behind LX Obsidian Labs.\n\n'
      + 'Test early builds of LX Pocket Business: record income and expenses, track customer debts, and create invoices and quotations. Then tell us exactly what broke and how to reproduce it.\n\n'
      + 'Who we need: Android 8.0 or later, an active Google Play account, and honest feedback. Applying is free, voluntary and takes about two minutes.\n\n'
      + 'Apply: lxobsidianportal.co.za/insider\n\n'
      + 'Test. Shape. Earn. Repeat.'
  },
  2: {
    id: 'lx-insider-recruit-2026-w2',
    ttl: '172800s',
    title: 'LX Insider: small groups, real feedback',
    body: 'We are onboarding this intake in small groups so every report gets read.\n\n'
      + 'If you already applied for the LX Pocket Business beta, watch your inbox for the Google Play testing link and installation steps. You do not need to apply twice.\n\n'
      + 'Not applied yet? Android 8.0 or later, an active Google Play account and honest feedback are the only requirements.\n\n'
      + 'Apply: lxobsidianportal.co.za/insider\n\n'
      + 'Testing is voluntary. Rewards are based on verified testing work, never on reviews.'
  },
  3: {
    id: 'lx-insider-recruit-2026-w3',
    ttl: '86400s',
    title: 'Last call: LX Insider beta intake',
    body: 'This is the final reminder for the current LX Insider intake for LX Pocket Business.\n\n'
      + 'Testers install early builds, complete short assignments, and report bugs with screenshots and reproduction steps. Useful reports are what qualify for programme rewards.\n\n'
      + 'We onboard in small groups so each tester gets support. Apply: lxobsidianportal.co.za/insider\n\n'
      + 'Participation is voluntary and no payment is required to join.'
  }
};

export function complianceIssues(title, body) {
  const text = `${title}\n${body}`;
  return FORBIDDEN_CLAIMS.filter(pattern => pattern.test(text)).map(pattern => pattern.source);
}

export function buildMessage({ wave = 1, audience = 'za', applyUrl = APPLY_URL } = {}) {
  const copy = WAVES[wave];
  if (!copy) throw new Error(`Unknown wave "${wave}". Use 1, 2 or 3.`);
  const topic = AUDIENCES[audience];
  if (!topic) throw new Error(`Unknown audience "${audience}". Use za, row or global.`);
  if (!/^https:\/\//.test(applyUrl)) throw new Error('Apply URL must use HTTPS.');
  const issues = complianceIssues(copy.title, copy.body);
  if (issues.length) throw new Error(`Copy contains claims requiring approval: ${issues.join(', ')}`);
  const display = url => url.replace(/^https:\/\//, '').replace(/^www\./, '').replace(/\/$/, '');
  const body = applyUrl === APPLY_URL
    ? copy.body
    : copy.body.split(display(APPLY_URL)).join(display(applyUrl));
  if (!body.includes(display(applyUrl))) {
    throw new Error('Copy must show the apply address so it works without notification deep links.');
  }
  return {
    topic,
    data: {
      title: copy.title,
      body,
      category: 'promotions',
      check_update: 'false',
      announcement_id: copy.id,
      action_url: applyUrl,
      program: 'lx-insider',
      wave: String(wave)
    },
    android: { priority: 'NORMAL', ttl: copy.ttl, collapse_key: copy.id }
  };
}

export async function fetchWithRetry(fetchImpl, url, options = {}, { attempts = 3, delayMs = 400, timeoutMs = 25000 } = {}) {
  let lastError;
  for (let attempt = 0; attempt < attempts; attempt++) {
    try {
      return await fetchImpl(url, { ...options, signal: AbortSignal.timeout(timeoutMs) });
    } catch (error) {
      lastError = error;
      if (attempt < attempts - 1) await new Promise(resolve => setTimeout(resolve, delayMs * (attempt + 1)));
    }
  }
  throw new Error(`Request to ${url} failed after ${attempts} attempts: ${lastError?.message}`);
}

export async function hasBeenSent(announcementId, audience, { file = SEND_LOG_FILE, read = readFile } = {}) {
  let text;
  try {
    text = await read(file, 'utf8');
  } catch (error) {
    if (error.code === 'ENOENT') return false;
    throw error;
  }
  return text.split('\n').filter(Boolean).some(line => {
    try {
      const entry = JSON.parse(line);
      return entry.announcement_id === announcementId && entry.audience === audience;
    } catch {
      return false;
    }
  });
}

export async function recordSend(entry, { file = SEND_LOG_FILE, append = appendFile, makeDir = mkdir } = {}) {
  await makeDir(dirname(file), { recursive: true });
  await append(file, `${JSON.stringify({ ...entry, logged_at: new Date().toISOString() })}\n`);
}

export async function verifyTargets({ fetchImpl = fetch, applyUrl = APPLY_URL, retryOptions = {} } = {}) {
  const checks = [];
  async function check(name, url, marker) {
    const response = await fetchWithRetry(fetchImpl, url, { redirect: 'follow', cache: 'no-store' }, retryOptions);
    const text = await response.text();
    const present = !marker || text.toLowerCase().includes(marker.toLowerCase());
    checks.push({ name, url, status: response.status, verified: response.ok && present, marker });
    if (!response.ok || !present) throw new Error(`${name} did not pass verification (HTTP ${response.status}).`);
    return response;
  }
  await check('product page', PRODUCT_URL, 'Pocket Business');
  await check('application page', applyUrl, 'lx insider');
  const testing = await fetchWithRetry(fetchImpl, PLAY_TESTING_URL, { redirect: 'follow', cache: 'no-store' }, retryOptions);
  const note = /accounts\.google\.com/.test(testing.url)
    ? 'Google sign-in wall reached; confirm with a non-enrolled Google account before sending.'
    : 'Reached without a sign-in redirect; confirm the invite still lists the tester account.';
  checks.push({
    name: 'Google Play testing link',
    url: PLAY_TESTING_URL,
    status: testing.status,
    verified: testing.ok,
    note
  });
  if (!testing.ok) throw new Error(`Google Play testing link did not pass verification (HTTP ${testing.status}).`);
  return checks;
}

async function main() {
  const args = process.argv.slice(2);
  const send = args.includes('--send');
  const localOnly = args.includes('--local-only');
  const reader = key => args.find(arg => arg.startsWith(`--${key}=`))?.split('=').slice(1).join('=');
  if (args.some(arg => !['--send', '--local-only'].includes(arg) && !/^--(audience|wave|apply-url)=/.test(arg))) {
    throw new Error('Usage: node send_insider_recruitment_fcm.mjs [--wave=1|2|3] [--audience=za|row|global] [--apply-url=https://...] [--local-only | --send]');
  }
  if (send && localOnly) throw new Error('Choose either --local-only or --send, not both.');
  const wave = Number(reader('wave') ?? 1);
  const audience = reader('audience') ?? 'za';
  const applyUrl = reader('apply-url') ?? APPLY_URL;
  if (send && (!reader('wave') || !reader('audience'))) {
    throw new Error('Refusing to broadcast: pass --wave and --audience explicitly with --send.');
  }

  const message = buildMessage({ wave, audience, applyUrl });
  if (Buffer.byteLength(JSON.stringify(message), 'utf8') > 4096) throw new Error('Message exceeds the FCM payload limit.');
  const checks = await verifyTargets({ applyUrl });
  const summary = { topic: message.topic, wave, announcement_id: message.data.announcement_id, checks, title: message.data.title, body: message.data.body };
  if (localOnly) {
    console.log(JSON.stringify({ status: 'local_only_validated', ...summary, note: 'Copy and destinations checked locally; no FCM request was made.' }, null, 2));
    return;
  }
  if (send && await hasBeenSent(message.data.announcement_id, audience)) {
    throw new Error(`Wave ${wave} for ${audience} is already logged as sent. Do not rerun a broadcast; check ${SEND_LOG_FILE}.`);
  }

  const require = createRequire(import.meta.url);
  const root = process.env.FIREBASE_TOOLS_ROOT || (process.platform === 'win32'
    ? join(process.env.APPDATA, 'npm', 'node_modules', 'firebase-tools')
    : '/usr/local/lib/node_modules/firebase-tools');
  const auth = require(join(root, 'lib', 'auth.js'));
  const account = auth.getProjectDefaultAccount(process.cwd());
  if (!account) throw new Error('Run firebase login first.');
  const tokens = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform']);

  const response = await fetchWithRetry(fetch, FCM_ENDPOINT, {
    method: 'POST',
    headers: { Authorization: `Bearer ${tokens.access_token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ validate_only: !send, message })
  }, { attempts: send ? 1 : 3, delayMs: 500, timeoutMs: 30000 });
  const result = await response.json();
  if (!response.ok) {
    console.error(`FCM ${response.status}: ${result.error?.message}`);
    process.exitCode = 1;
    return;
  }
  if (send) {
    await recordSend({ announcement_id: message.data.announcement_id, audience, topic: message.topic, wave, name: result.name });
  }
  console.log(JSON.stringify({
    status: send ? 'accepted_for_delivery' : 'validated_only',
    ...summary,
    name: result.name,
    note: 'FCM acceptance is not a per-device delivery receipt. Promotions preferences and OS notification permission still apply.'
  }, null, 2));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
