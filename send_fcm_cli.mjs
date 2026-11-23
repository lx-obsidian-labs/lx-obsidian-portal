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

// Release-available broadcast is guarded by the real public manifest and published guide.
const manifestResponse = await fetch('https://www.lxobsidianportal.co.za/vista-version.json', { signal: AbortSignal.timeout(20000), cache: 'no-store' });
if (!manifestResponse.ok) throw new Error('Could not verify the public release manifest.');
const manifest = await manifestResponse.json();
if (manifest.versionCode !== 19 || manifest.versionName !== '1.0.12' ||
    !/^https:\/\//.test(manifest.downloadUrl || '') || !/^[a-f\d]{64}$/i.test(manifest.sha256 || '') || !(manifest.sizeBytes > 0)) {
  throw new Error('Vista Watch 1.0.12 is not verified in the public manifest yet. No release-available notification was sent.');
}
const title = 'Vista Watch 1.0.12 is live — what’s new';
const body = 'Our latest stability update is ready for mobile and Android TV!\n\n'
  + 'WHAT CHANGED\n• Improved playback and catalog recovery; playback confirmed in our latest device testing.\n'
  + '• Refined TV navigation and playback-source controls.\n• Official Help & Support with reviewable problem reports.\n'
  + '• Clearer login error dialogs with recovery actions.\n• Expanded avatars, refreshed artwork and Vista Watch branding.\n\n'
  + 'QUICK TIPS\n• Tap this notification to check for the update, then approve installation.\n'
  + '• TV: press Menu or hold OK during playback to choose a source, adjust next-episode autoplay or open help. Back returns to video.\n'
  + '• Mobile: Settings > Account > Edit Avatar changes your avatar.\n'
  + '• Settings > Help & Support > Report a problem lets you review a report before sending it through your email app. TV users can scan the support QR.\n\n'
  + 'Full changelog & guide: https://www.lxobsidianportal.co.za/vista-1-0-12\n'
  + 'Thank you for helping improve Vista Watch. Stream availability still depends on providers.';

const message = {
  topic: 'vista_updates',
  data: {
    title,
    body,
    category: 'promotions',
    check_update: 'true',
    version: '1.0.12',
    release_status: 'live',
    mandatory: 'false',
    announcement_id: 'vista-1012-live-changelog',
    changelog_url: 'https://www.lxobsidianportal.co.za/vista-1-0-12'
  },
  android: {
    priority: 'HIGH',
    ttl: '604800s',
    collapse_key: 'vista-1012-live-changelog'
  }
};

const send = process.argv.includes('--send');
if (Buffer.byteLength(JSON.stringify(message), 'utf8') > 4096) throw new Error('Announcement exceeds FCM payload limit.');
const guideResponse = await fetch(message.data.changelog_url, { signal: AbortSignal.timeout(20000), cache: 'no-store' });
if (!guideResponse.ok || !(await guideResponse.text()).includes('Playback sources')) {
  throw new Error('The public changelog and usage guide must be live before announcing it.');
}
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
