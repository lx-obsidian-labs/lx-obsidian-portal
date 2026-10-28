// Uses the existing Firebase CLI login; no service-account key or legacy FCM server key.
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

const title = 'Vista Cinema v1.0.9 is Live! 🚀';
const body = 'Stream errors fixed with Smart Cache Recovery! Meet your new AI Assistant — talk or chat directly with Vista Cinema for personalized recommendations, cast details & spoiler-free guides. Tap to update!';

const message = {
  topic: 'vista_updates',
  notification: { title, body },
  data: {
    title,
    body,
    category: 'updates',
    check_update: 'true',
    version: '1.0.9',
    release_status: 'live'
  },
  android: {
    priority: 'HIGH',
    notification: {
      channel_id: 'vista_updates',
      tag: 'vista-109-live-release'
    }
  }
};

const send = process.argv.includes('--send');
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
    topic: message.topic,
    name: result.name,
    title,
    body
  }, null, 2));
}
