// Roadmap announcement. Validation is the default; --send dispatches one topic broadcast.
import { createRequire } from 'node:module';
import { join } from 'node:path';

const require = createRequire(import.meta.url);
const root = process.env.FIREBASE_TOOLS_ROOT || (process.platform === 'win32'
  ? join(process.env.APPDATA, 'npm', 'node_modules', 'firebase-tools') : '/usr/local/lib/node_modules/firebase-tools');
const auth = require(join(root, 'lib', 'auth.js'));
const account = auth.getProjectDefaultAccount(process.cwd());
if (!account) throw new Error('Run firebase login first.');
const tokens = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform']);
const title = 'Live TV & Sports: in the plans';
const body = 'We’re planning Live TV and Sports for Vista Watch! We’re exploring the experience and will share more as plans develop. Availability and launch timing are not confirmed yet. Thanks for being part of Vista’s next chapter!';
const send = process.argv.includes('--send');
const message = {
  topic: 'vista_updates',
  data: { title, body, category: 'promotions', check_update: 'false',
    release_status: 'planned', announcement_id: 'live-tv-sports-planned-2026' },
  android: { priority: 'NORMAL', ttl: '259200s', collapse_key: 'live-tv-sports-planned-2026' }
};
const response = await fetch('https://fcm.googleapis.com/v1/projects/vista-cinema/messages:send', {
  method: 'POST', signal: AbortSignal.timeout(30000),
  headers: { Authorization: `Bearer ${tokens.access_token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ validate_only: !send, message })
});
const result = await response.json();
if (!response.ok) {
  console.error(`FCM ${response.status}: ${result.error?.message}`);
  process.exitCode = 1;
} else {
  console.log(JSON.stringify({ status: send ? 'accepted_for_delivery' : 'validated_only',
    topic: message.topic, name: result.name, title, body }, null, 2));
}
