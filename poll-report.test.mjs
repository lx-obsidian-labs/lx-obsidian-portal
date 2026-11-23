import test from 'node:test';
import assert from 'node:assert/strict';
import { summarize, campaign } from './poll-report.mjs';

function vote(quoteAllowed) {
  return { name: 'private-user-id', fields: { feedbackPolls: { mapValue: { fields: {
    [campaign]: { mapValue: { fields: {
      answers: { mapValue: { fields: Object.fromEntries(Object.entries({ feeling: 'mixed', friction: 'ads', priority: 'reliability', discovery: 'friend' })
        .map(([id, stringValue]) => [id, { stringValue }])) } },
      comment: { stringValue: 'Please make playback smoother.' }, quoteAllowed: { booleanValue: quoteAllowed }
    } } }
  } } } } };
}
test('counts honest sentiment and pain points without exposing identities', () => {
  const report = summarize([vote(false), vote(true)]);
  assert.equal(report.responses, 2);
  assert.equal(report.questions.feeling.mixed, 2);
  assert.equal(report.questions.friction.ads, 2);
  assert.equal(report.comments, 2);
  assert.deepEqual(report.marketingQuotes, ['Please make playback smoother.']);
  assert.ok(!JSON.stringify(report).includes('private-user-id'));
});
test('ignores unrelated profiles and malformed/incomplete votes', () => {
  assert.equal(summarize([{ fields: {} }, { fields: { feedbackPolls: {} } }]).responses, 0);
  const invalid = vote(true);
  invalid.fields.feedbackPolls.mapValue.fields[campaign].mapValue.fields.answers.mapValue.fields.feeling.stringValue = 'invented';
  assert.equal(summarize([invalid]).responses, 0);
});
