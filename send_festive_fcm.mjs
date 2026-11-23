// Festive greeting only. Validation is the default; --send dispatches one topic broadcast.
import { createRequire } from 'node:module';
import { join } from 'node:path';

const require = createRequire(import.meta.url);
const root = process.env.FIREBASE_TOOLS_ROOT || (process.platform === 'win32'
  ? join(process.env.APPDATA, 'npm', 'node_modules', 'firebase-tools')
  : '/usr/local/lib/node_modules/firebase-tools');
const auth = require(join(root, 'lib', 'auth.js'));
const account = auth.getProjectDefaultAccount(process.cwd());
if (!account) throw new Error('Run firebase login first.');
const tokens = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform']);

const title = 'Vista 1.0.10 is coming ✨';
const body = 'Wishing you a wonderful start to the festive season! We’re preparing Vista 1.0.10 with festive picks, better movie suggestions and downloads. Here’s to great stories and memorable movie nights. Stay tuned!';
const send = process.argv.includes('--send');
const message = {
  topic: 'vista_updates',
  // Data-only delivery lets VistaMessagingService respect the viewer's notification preference
  // in both foreground and background; this greeting is not a mandatory update notification.
  data: { title, body, category: 'promotions', check_update: 'false', announcement_id: 'festive-welcome-2026' },
  android: { priority: 'NORMAL', ttl: '86400s', collapse_key: 'festive-welcome-2026' }
};
const response = await fetch('https://fcm.googleapis.com/v1/projects/vista-cinema/messages:send', {
  method: 'POST',
  signal: AbortSignal.timeout(30000),
  headers: { Authorization: `Bearer ${tokens.access_token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ validate_only: !send, message })
});
const result = await response.json();
if (!response.ok) {
  console.error(`FCM ${response.status}: ${result.error?.message}`);
  process.exitCode = 1;
} else {
  console.log(JSON.stringify({
    status: send ? 'accepted_for_delivery' : 'validated_only',
    topic: message.topic, name: result.name, title, body
  }, null, 2));
}
