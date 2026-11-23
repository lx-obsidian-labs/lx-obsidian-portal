// Aggregate feedback using the existing Firebase CLI's owner/admin login. No user identities emitted.
import { createRequire } from 'node:module';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';

export const campaign = 'experience_1_0_11';
const options = {
  feeling: ['love', 'good', 'mixed', 'frustrated', 'new'],
  friction: ['none', 'playback', 'ads', 'catalog', 'navigation', 'downloads', 'other'],
  priority: ['reliability', 'discovery', 'offline', 'tv', 'assistant', 'design'],
  discovery: ['friend', 'whatsapp', 'social', 'search', 'website', 'other', 'skip']
};

export function summarize(documents) {
  const counts = Object.fromEntries(Object.entries(options).map(([id, values]) => [id, Object.fromEntries(values.map(value => [value, 0]))]));
  const result = { campaign, responses: 0, questions: counts, comments: 0, marketingQuotes: [] };
  for (const doc of documents) {
    const poll = doc.fields?.feedbackPolls?.mapValue?.fields?.[campaign]?.mapValue?.fields;
    const answers = poll?.answers?.mapValue?.fields;
    if (!answers || !Object.entries(options).every(([id, allowed]) => allowed.includes(answers[id]?.stringValue))) continue;
    result.responses++;
    for (const id of Object.keys(options)) counts[id][answers[id].stringValue]++;
    const comment = poll.comment?.stringValue?.trim();
    if (comment) result.comments++;
    // Only explicitly opted-in comments can be considered for marketing; no account identifiers.
    if (comment && poll.quoteAllowed?.booleanValue === true) result.marketingQuotes.push(comment.slice(0, 500));
  }
  return result;
}

async function main() {
  const require = createRequire(import.meta.url);
  const root = process.env.FIREBASE_TOOLS_ROOT || (process.platform === 'win32'
    ? join(process.env.APPDATA, 'npm', 'node_modules', 'firebase-tools') : '/usr/local/lib/node_modules/firebase-tools');
  const auth = require(join(root, 'lib', 'auth.js'));
  const account = auth.getProjectDefaultAccount(process.cwd());
  if (!account) throw new Error('Run firebase login with the project owner/admin account first.');
  const tokens = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform']);
  const endpoint = 'https://firestore.googleapis.com/v1/projects/vista-cinema/databases/(default)/documents:runQuery';
  const documents = [];
  let last;
  do {
    const response = await fetch(endpoint, {
      method: 'POST', signal: AbortSignal.timeout(30000),
      headers: { Authorization: `Bearer ${tokens.access_token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ structuredQuery: {
        from: [{ collectionId: 'viewers' }], select: { fields: [{ fieldPath: 'feedbackPolls' }] },
        orderBy: [{ field: { fieldPath: '__name__' }, direction: 'ASCENDING' }], limit: 500,
        ...(last ? { startAt: { values: [{ referenceValue: last }], before: false } } : {})
      } })
    });
    if (!response.ok) throw new Error(`Feedback report failed (HTTP ${response.status}). Check Firestore and project IAM access.`);
    const page = (await response.json()).flatMap(row => row.document ? [row.document] : []);
    documents.push(...page);
    last = page.length === 500 ? page.at(-1).name : null;
  } while (last);
  const report = summarize(documents);
  if (!process.argv.includes('--marketing-quotes')) delete report.marketingQuotes;
  console.log(JSON.stringify(report, null, 2));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
