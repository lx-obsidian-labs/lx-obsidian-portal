// Black Friday poll invitation. Default validates only; --send broadcasts once.
import { createRequire } from 'node:module';
import { join } from 'node:path';

const title = 'Our Black Friday wish? Roast us 😅';
const body = 'May your discounts be huge and your buffering tiny. 🖤\n\n'
  + 'Our Black Friday wishlist has one thing on it: your brutally honest opinion. '
  + 'Love Vista Watch? Tell us. Something still annoys you? This is your official invitation to roast it.\n\n'
  + 'On your phone, open Vista Watch → Settings → Help shape Vista Watch → Have your say. '
  + '4 quick questions. About a minute. Help us choose what to fix and improve next.\n\n'
  + 'Sign in to submit. Already answered? Thank you — your feedback counts. '
  + 'No shopping cart required. Just good taste and honest feedback. 🍿';
const message = {
  topic: 'vista_updates',
  data: {
    title, body, category: 'promotions', check_update: 'false',
    announcement_id: 'vista-black-friday-poll-2026'
  },
  android: { priority: 'NORMAL', ttl: '86400s', collapse_key: 'vista-black-friday-poll-2026' }
};
if (process.argv.slice(2).some(arg => arg !== '--send')) throw new Error('Use no arguments to validate, or --send to broadcast.');
if (Buffer.byteLength(JSON.stringify(message), 'utf8') > 4096) throw new Error('FCM payload too large.');
const require = createRequire(import.meta.url);
const root = process.env.FIREBASE_TOOLS_ROOT || (process.platform === 'win32'
  ? join(process.env.APPDATA, 'npm', 'node_modules', 'firebase-tools')
  : '/usr/local/lib/node_modules/firebase-tools');
const auth = require(join(root, 'lib', 'auth.js'));
const account = auth.getProjectDefaultAccount(process.cwd());
if (!account) throw new Error('Run firebase login first.');
const tokens = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform']);
const send = process.argv.includes('--send');
const response = await fetch('https://fcm.googleapis.com/v1/projects/vista-cinema/messages:send', {
  method: 'POST', signal: AbortSignal.timeout(30000),
  headers: { Authorization: `Bearer ${tokens.access_token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ validate_only: !send, message })
});
const result = await response.json();
if (!response.ok) throw new Error(`FCM ${response.status}: ${result.error?.message}`);
console.log(JSON.stringify({ status: send ? 'accepted_for_delivery' : 'validated_only',
  topic: message.topic, name: result.name, title, body }, null, 2));
