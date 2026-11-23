import test from 'node:test';
import assert from 'node:assert/strict';
import { parseCsv, summarize, markdownReport, validateRegister, STATUSES } from './insider-dashboard.mjs';

const HEADER = 'applicant_id,received_at,full_name,play_email,country,device_model,android_version,experience,availability,categories,source_channel,consent_participate,consent_comms,status,play_added_at,opted_in_at,installed_at,last_active_at,assignments_completed,bugs_verified,reward_points,reward_approved,notes';

function row(id, overrides = {}) {
  const base = {
    applicant_id: id, received_at: '2026-11-20', full_name: 'Test Person', play_email: 'private@example.com',
    country: 'South Africa', device_model: 'TestPhone', android_version: '14', experience: 'Some',
    availability: '1–2 hours', categories: 'Any', source_channel: 'Vista Watch notification',
    consent_participate: 'yes', consent_comms: 'yes', status: 'Actively testing',
    play_added_at: '2026-11-01', opted_in_at: '2026-11-02', installed_at: '2026-11-03',
    last_active_at: '2026-11-22', assignments_completed: '2', bugs_verified: '1',
    reward_points: '7', reward_approved: 'no', notes: ''
  };
  const values = { ...base, ...overrides };
  return HEADER.split(',').map(key => {
    const value = values[key] ?? '';
    return /[",\n\r]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
  }).join(',');
}

test('parses quoted CSV fields and skips blank lines', () => {
  const csv = `${HEADER}\n${row('A1', { notes: 'has, comma and "quote"' })}\n\n`;
  const parsed = parseCsv(csv);
  assert.equal(parsed.length, 1);
  assert.equal(parsed[0].notes, 'has, comma and "quote"');
  assert.equal(parsed[0].applicant_id, 'A1');
});

test('counts the funnel, channels and verified work without exposing identities', () => {
  const rows = parseCsv([
    HEADER,
    row('A1'),
    row('A2', { status: 'Opted in', source_channel: 'WhatsApp', assignments_completed: '0', bugs_verified: '0', reward_points: '0' }),
    row('A3', { status: 'Inactive', opted_in_at: '2026-10-01', assignments_completed: '0', bugs_verified: '0', reward_points: '0' }),
    row('A4', { status: 'Interested', opted_in_at: '', assignments_completed: '0', bugs_verified: '0', reward_points: '0', reward_approved: 'yes' }),
    row('A5', { status: 'Bogus status', assignments_completed: '0', bugs_verified: '0', reward_points: '0' })
  ].join('\n'));
  const report = summarize(rows, '2026-11-23');
  assert.equal(report.applicants, 5);
  assert.equal(report.activeTesters, 1);
  assert.equal(report.byStatus['Opted in'], 1);
  assert.equal(report.continuousOptIn, 2);
  assert.equal(report.playRequirement.met, false);
  assert.equal(report.channels['Vista Watch notification'], 4);
  assert.equal(report.channels.WhatsApp, 1);
  assert.equal(report.assignmentsCompleted, 2);
  assert.equal(report.bugsVerified, 1);
  assert.equal(report.rewardsApproved, 1);
  assert.deepEqual(report.unknownStatus, ['A5']);
  assert.ok(!JSON.stringify(report).includes('private@example.com'));
  assert.ok(!JSON.stringify(report).includes('Test Person'));
});

test('the 12-tester / 14-day Play requirement is evidenced from opt-in dates', () => {
  const lines = [HEADER];
  for (let i = 0; i < 12; i++) lines.push(row(`T${i}`, { opted_in_at: '2026-11-05' }));
  lines.push(row('NEW', { opted_in_at: '2026-11-20' }));
  const report = summarize(parseCsv(lines.join('\n')), '2026-11-23');
  assert.equal(report.continuousOptIn, 12);
  assert.equal(report.playRequirement.met, true);
});

test('markdown report carries the headline numbers and flags bad statuses', () => {
  const report = summarize(parseCsv([HEADER, row('A1', { status: 'Bogus' })].join('\n')), '2026-11-23');
  const markdown = markdownReport(report);
  assert.match(markdown, /LX Insider daily report — 2026-11-23/);
  assert.match(markdown, /Active testers: 0 \/ 30/);
  assert.match(markdown, /Unknown statuses on 1 register rows/);
  assert.ok(markdown.includes('## Retention'));
  assert.equal(STATUSES.length, 11);
});

test('validation blocks unverified active testers, missing consent and broken chain-of-custody', () => {
  const rows = parseCsv([
    HEADER,
    row('A1'),
    row('A2', { status: 'Actively testing', installed_at: '' }),
    row('A3', { status: 'Consent received', consent_participate: 'no' }),
    row('A4', { status: 'Added to Google Play', play_added_at: '' }),
    row('A1'),
    row('A6', { play_email: 'not-an-email' }),
    row('A7', { reward_approved: 'yes', reward_points: '0' })
  ].join('\n'));
  const result = validateRegister(rows);
  assert.equal(result.ok, false);
  assert.ok(result.errors >= 6);
  assert.ok(result.issues.some(issue => issue.applicant_id === 'A2' && /installed_at/.test(issue.message)));
  assert.ok(result.issues.some(issue => issue.applicant_id === 'A3' && /consent/.test(issue.message)));
  assert.ok(result.issues.some(issue => issue.applicant_id === 'A4' && /play_added_at/.test(issue.message)));
  assert.ok(result.issues.some(issue => issue.applicant_id === 'A1' && /Duplicate/.test(issue.message)));
  assert.ok(result.issues.some(issue => issue.applicant_id === 'A7' && /zero verified points/.test(issue.message)));
});

test('a clean register passes validation', () => {
  const result = validateRegister(parseCsv([HEADER, row('A1'), row('A2', { status: 'Interested', received_at: '2026-11-22' })].join('\n')));
  assert.equal(result.ok, true);
  assert.equal(result.errors, 0);
});

test('retention alerts cover inactivity, 30/60/90-day check-ins and recognition', () => {
  const rows = parseCsv([
    HEADER,
    row('A1', { last_active_at: '2026-10-01', play_added_at: '2026-10-24' }),
    row('A2', { status: 'Testing completed', play_added_at: '2026-08-24' }),
    row('A3', { status: 'Inactive', last_active_at: '2026-09-01' }),
    row('A4', { play_added_at: '2026-09-24' }),
    row('A5', { reward_points: '30' })
  ].join('\n'));
  const report = summarize(rows, '2026-11-23');
  assert.deepEqual(report.retention.reengagement.ids, ['A1']);
  assert.deepEqual(report.retention.checkins.day30.ids, ['A1']);
  assert.deepEqual(report.retention.checkins.day60.ids, ['A4']);
  assert.deepEqual(report.retention.checkins.day90.ids, ['A2']);
  assert.deepEqual(report.retention.recognition.ids, ['A2', 'A5']);
  assert.ok(!JSON.stringify(report).includes('Test Person'));
});
