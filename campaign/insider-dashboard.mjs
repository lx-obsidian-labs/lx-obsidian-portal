// LX Insider recruitment dashboard. Reads the tester register and emits the daily report.
// No identities are printed; counts only. Run: node campaign/insider-dashboard.mjs [--csv=path] [--date=YYYY-MM-DD] [--markdown]
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

export const STATUSES = [
  'Prospect', 'Invited', 'Interested', 'Consent received', 'Added to Google Play',
  'Opted in', 'Application installed', 'Actively testing', 'Testing completed',
  'Reward approved', 'Inactive'
];

export const TARGETS = { applicants: 100, active: 30, community: 100, playOptIn: 12, playDays: 14 };

export const REQUIRED_COLUMNS = [
  'applicant_id', 'received_at', 'play_email', 'source_channel', 'consent_participate',
  'status', 'play_added_at', 'opted_in_at', 'installed_at', 'last_active_at',
  'assignments_completed', 'bugs_verified', 'reward_points', 'reward_approved'
];

const CHANNEL_LABELS = {
  whatsapp: 'WhatsApp', facebook: 'Facebook', linkedin: 'LinkedIn', tiktok: 'TikTok',
  reddit: 'Reddit', website: 'LX Obsidian Labs website',
  'vista watch notification': 'Vista Watch notification'
};

export function channelName(value) {
  const key = (value || '').trim().toLowerCase();
  if (!key) return 'Unattributed';
  return CHANNEL_LABELS[key] || (value || '').trim();
}

const ENROLLED_STATUSES = ['Opted in', 'Application installed', 'Actively testing', 'Testing completed', 'Reward approved'];

export function parseCsv(text, requiredColumns = []) {
  const source = (text || '').replace(/^\uFEFF/, '');
  const rows = [];
  let row = [];
  let field = '';
  let quoted = false;
  for (let i = 0; i < source.length; i++) {
    const char = source[i];
    if (quoted) {
      if (char === '"') {
        if (source[i + 1] === '"') { field += '"'; i++; } else quoted = false;
      } else field += char;
    } else if (char === '"') {
      quoted = true;
    } else if (char === ',') {
      row.push(field); field = '';
    } else if (char === '\n' || char === '\r') {
      if (char === '\r' && source[i + 1] === '\n') i++;
      row.push(field); field = '';
      if (row.some(value => value.trim() !== '')) rows.push(row);
      row = [];
    } else field += char;
  }
  row.push(field);
  if (row.some(value => value.trim() !== '')) rows.push(row);
  if (!rows.length) return [];
  const header = rows[0].map(value => value.trim());
  const missing = requiredColumns.filter(column => !header.includes(column));
  if (missing.length) throw new Error(`Register is missing required columns: ${missing.join(', ')}`);
  return rows.slice(1).map(values => Object.fromEntries(header.map((key, index) => [key, (values[index] ?? '').trim()])));
}

export function daysBetween(from, to) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(from || '') || !/^\d{4}-\d{2}-\d{2}$/.test(to || '')) return null;
  const start = Date.parse(`${from}T00:00:00Z`);
  const end = Date.parse(`${to}T00:00:00Z`);
  if (Number.isNaN(start) || Number.isNaN(end)) return null;
  if (new Date(start).toISOString().slice(0, 10) !== from || new Date(end).toISOString().slice(0, 10) !== to) return null;
  return Math.floor((end - start) / 86400000);
}

export function validateRegister(rows) {
  const issues = [];
  const seen = new Set();
  const add = (row, severity, message) => issues.push({ applicant_id: row.applicant_id || '(missing)', severity, message });
  const advanced = ['Consent received', 'Added to Google Play', 'Opted in', 'Application installed', 'Actively testing', 'Testing completed', 'Reward approved'];
  const enrolled = ['Opted in', 'Application installed', 'Actively testing', 'Testing completed', 'Reward approved'];
  for (const row of rows) {
    if (!row.applicant_id) { add(row, 'error', 'Missing applicant_id'); continue; }
    if (seen.has(row.applicant_id)) add(row, 'error', 'Duplicate applicant_id');
    seen.add(row.applicant_id);
    if (!STATUSES.includes(row.status)) add(row, 'error', `Unknown status "${row.status}"`);
    if (!row.received_at) add(row, 'error', 'Missing received_at');
    if (!row.play_email) add(row, 'warning', 'Missing play_email');
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(row.play_email)) add(row, 'error', 'Invalid play_email');
    if (advanced.includes(row.status) && !/^(yes|true)$/i.test(row.consent_participate || '')) {
      add(row, 'error', 'Advanced status without participation consent');
    }
    if (row.status === 'Added to Google Play' && !row.play_added_at) add(row, 'error', 'Added to Google Play without play_added_at');
    if (enrolled.includes(row.status) && !row.opted_in_at) add(row, 'error', 'Enrolled status without opted_in_at');
    if (row.status === 'Actively testing' && !row.installed_at) add(row, 'error', 'Actively testing without installed_at verification');
    const optInDelay = daysBetween(row.play_added_at, row.opted_in_at);
    if (optInDelay !== null && optInDelay < 0) add(row, 'error', 'opted_in_at precedes play_added_at');
    if (/^(yes|true|approved)$/i.test(row.reward_approved || '') && !(Number(row.reward_points) > 0)) {
      add(row, 'error', 'Reward approved with zero verified points');
    }
    for (const [column, label] of [['assignments_completed', 'assignments completed'], ['bugs_verified', 'bugs verified'], ['reward_points', 'reward points']]) {
      const value = Number(row[column] || 0);
      if (!Number.isFinite(value) || value < 0) add(row, 'error', `Invalid ${label} value`);
    }
    if (!row.source_channel) add(row, 'warning', 'Missing source_channel; attribution becomes Unattributed');
  }
  const errors = issues.filter(issue => issue.severity === 'error').length;
  const warnings = issues.length - errors;
  return { ok: errors === 0, errors, warnings, issues };
}

export function summarize(rows, today = new Date().toISOString().slice(0, 10)) {
  if (!Array.isArray(rows)) throw new Error('Register rows must be an array.');
  const byStatus = Object.fromEntries(STATUSES.map(status => [status, 0]));
  const channels = {};
  const unknownStatus = [];
  let applicants = 0;
  let continuousOptIn = 0;
  let assignmentsCompleted = 0;
  let bugsVerified = 0;
  let rewardPoints = 0;
  let rewardsApproved = 0;
  for (const row of rows) {
    if (!row.applicant_id) continue;
    if (row.received_at) applicants++;
    if (STATUSES.includes(row.status)) byStatus[row.status]++;
    else unknownStatus.push(row.applicant_id);
    const channel = channelName(row.source_channel);
    channels[channel] = (channels[channel] || 0) + 1;
    const days = daysBetween(row.opted_in_at, today);
    if (days !== null && days >= TARGETS.playDays && ENROLLED_STATUSES.includes(row.status)) continuousOptIn++;
    assignmentsCompleted += Number(row.assignments_completed) || 0;
    bugsVerified += Number(row.bugs_verified) || 0;
    rewardPoints += Number(row.reward_points) || 0;
    if (/^(yes|true|approved)$/i.test(row.reward_approved || '')) rewardsApproved++;
  }
  const retention = {
    reengagement: { count: 0, ids: [] },
    checkins: { day30: { count: 0, ids: [] }, day60: { count: 0, ids: [] }, day90: { count: 0, ids: [] } },
    recognition: { count: 0, ids: [] }
  };
  for (const row of rows) {
    if (!row.applicant_id || row.status === 'Inactive' || row.status === 'Prospect' || row.status === 'Invited') continue;
    const inactiveDays = daysBetween(row.last_active_at, today);
    if (inactiveDays !== null && inactiveDays >= 14) {
      retention.reengagement.count++;
      retention.reengagement.ids.push(row.applicant_id);
    }
    const enrolledDays = daysBetween(row.play_added_at, today);
    if (enrolledDays !== null) {
      for (const day of [30, 60, 90]) {
        if (enrolledDays >= day && enrolledDays < day + 7) {
          const key = `day${day}`;
          retention.checkins[key].count++;
          retention.checkins[key].ids.push(row.applicant_id);
        }
      }
    }
    if (row.status === 'Testing completed' || (Number(row.reward_points) || 0) >= 25) {
      retention.recognition.count++;
      retention.recognition.ids.push(row.applicant_id);
    }
  }
  const active = byStatus['Actively testing'];
  return {
    date: today,
    registerEntries: rows.filter(row => row.applicant_id).length,
    applicants,
    applicantsTarget: TARGETS.applicants,
    byStatus,
    unknownStatus,
    activeTesters: active,
    activeTarget: TARGETS.active,
    continuousOptIn,
    playRequirement: {
      needed: TARGETS.playOptIn,
      days: TARGETS.playDays,
      met: continuousOptIn >= TARGETS.playOptIn
    },
    channels,
    assignmentsCompleted,
    bugsVerified,
    rewardPoints,
    rewardsApproved,
    averageAssignmentsPerActiveTester: active ? Math.round((assignmentsCompleted / active) * 10) / 10 : 0,
    retention
  };
}

export function markdownReport(report) {
  const lines = [
    `# LX Insider daily report — ${report.date}`,
    '',
    `- Applicants: ${report.applicants} / ${report.applicantsTarget} target`,
    `- Active testers: ${report.activeTesters} / ${report.activeTarget} target`,
    `- Continuous opt-in ≥${report.playRequirement.days} days: ${report.continuousOptIn} (Play requirement ${report.playRequirement.met ? 'MET' : `needs ${report.playRequirement.needed}`})`,
    `- Assignments completed: ${report.assignmentsCompleted} · verified bugs: ${report.bugsVerified}`,
    `- Reward points: ${report.rewardPoints} · rewards approved: ${report.rewardsApproved}`,
    `- Avg assignments per active tester: ${report.averageAssignmentsPerActiveTester}`,
    '',
    '## Funnel',
    ...STATUSES.map(status => `- ${status}: ${report.byStatus[status]}`),
    '',
    '## Channels',
    ...Object.entries(report.channels).sort((a, b) => b[1] - a[1]).map(([channel, count]) => `- ${channel}: ${count}`),
    '',
    '## Retention',
    `- No activity for ≥14 days: ${report.retention.reengagement.count}`,
    `- Day-30 check-ins due: ${report.retention.checkins.day30.count}`,
    `- Day-60 check-ins due: ${report.retention.checkins.day60.count}`,
    `- Day-90 reviews due: ${report.retention.checkins.day90.count}`,
    `- Recognition candidates: ${report.retention.recognition.count}`
  ];
  if (report.unknownStatus.length) lines.push('', `⚠ Unknown statuses on ${report.unknownStatus.length} register rows — fix before reporting.`);
  return lines.join('\n');
}

async function main() {
  const args = process.argv.slice(2);
  const reader = key => args.find(arg => arg.startsWith(`--${key}=`))?.split('=').slice(1).join('=');
  if (args.some(arg => !/^--(csv|date)=/.test(arg) && !['--markdown', '--validate'].includes(arg))) {
    throw new Error('Usage: node campaign/insider-dashboard.mjs [--csv=path] [--date=YYYY-MM-DD] [--markdown] [--validate]');
  }
  const csvPath = reader('csv') ?? 'campaign/insider-register.csv';
  const today = reader('date') ?? new Date().toISOString().slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(today)) throw new Error('--date must be YYYY-MM-DD.');
  const rows = parseCsv(await readFile(csvPath, 'utf8'), REQUIRED_COLUMNS);
  if (args.includes('--validate')) {
    const validation = validateRegister(rows);
    console.log(JSON.stringify(validation, null, 2));
    if (!validation.ok) process.exitCode = 1;
    return;
  }
  const report = summarize(rows, today);
  console.log(args.includes('--markdown') ? markdownReport(report) : JSON.stringify(report, null, 2));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
