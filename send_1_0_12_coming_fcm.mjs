// Vista Watch mobile + Android TV announcement. Preview is local; sending is explicit.
import { createRequire } from 'node:module';
import { join } from 'node:path';

const title = 'Vista Watch: our most stable experience yet';
const body = 'Playback is working smoothly in our latest device testing. Thank you for helping Vista Watch reach its most stable state yet! Version 1.0.12 for mobile and Android TV is expected in a few hours, with playback improvements and a more polished experience. Stay tuned.';
const message = {
  // The shared application subscribes both mobile and TV installations to this topic.
  topic: 'vista_updates',
  data: {
    title, body, category: 'promotions', check_update: 'false', version: '1.0.12',
    release_status: 'coming_soon', announcement_id: 'vista-1012-stability-coming-soon'
  },
  android: { priority: 'NORMAL', ttl: '10800s', collapse_key: 'vista-1012-stability-coming-soon' }
};

const args = process.argv.slice(2);
if (args.some(arg => !['--preview', '--validate', '--send'].includes(arg)) || args.length > 1) {
  throw new Error('Use --preview (default), --validate, or --send.');
}
const mode = args[0] || '--preview';
if (mode === '--preview') {
  console.log(JSON.stringify({ status: 'prepared_only', audience: 'Mobile and Android TV topic subscribers', message }, null, 2));
} else {
  const require = createRequire(import.meta.url);
  const root = process.env.FIREBASE_TOOLS_ROOT || (process.platform === 'win32'
    ? join(process.env.APPDATA, 'npm', 'node_modules', 'firebase-tools')
    : '/usr/local/lib/node_modules/firebase-tools');
  const auth = require(join(root, 'lib', 'auth.js'));
  const account = auth.getProjectDefaultAccount(process.cwd());
  if (!account) throw new Error('Run firebase login first.');
  const tokens = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform']);
  const response = await fetch('https://fcm.googleapis.com/v1/projects/vista-cinema/messages:send', {
    method: 'POST', signal: AbortSignal.timeout(30000),
    headers: { Authorization: `Bearer ${tokens.access_token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ validate_only: mode !== '--send', message })
  });
  const result = await response.json();
  if (!response.ok) throw new Error(`FCM ${response.status}: ${result.error?.message}`);
  console.log(JSON.stringify({ status: mode === '--send' ? 'accepted_for_delivery' : 'validated_only',
    topic: message.topic, name: result.name, title, body }, null, 2));
}
