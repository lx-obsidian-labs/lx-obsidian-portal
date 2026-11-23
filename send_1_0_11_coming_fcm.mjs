// Coming-soon announcement. Defaults to validation; --send sends the topic announcement.
import { createRequire } from 'node:module';
import { join } from 'node:path';

const require = createRequire(import.meta.url);
const root = process.env.FIREBASE_TOOLS_ROOT || (process.platform === 'win32'
  ? join(process.env.APPDATA, 'npm', 'node_modules', 'firebase-tools') : '/usr/local/lib/node_modules/firebase-tools');
const auth = require(join(root, 'lib', 'auth.js'));
const account = auth.getProjectDefaultAccount(process.cwd());
if (!account) throw new Error('Run firebase login first.');
const tokens = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform']);
const title = 'Vista 1.0.11 is coming';
const body = 'We’re preparing Vista 1.0.11 with playback and catalog fixes, smoother TV navigation, a more polished experience and a new way to tell us how Vista feels. Thanks for helping shape what comes next. Stay tuned!';
const send = process.argv.includes('--send');
const message = {
  topic: 'vista_updates',
  data: { title, body, category: 'promotions', check_update: 'false', version: '1.0.11',
    release_status: 'coming_soon', announcement_id: 'vista-1011-coming-soon' },
  android: { priority: 'NORMAL', ttl: '259200s', collapse_key: 'vista-1011-coming-soon' }
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
