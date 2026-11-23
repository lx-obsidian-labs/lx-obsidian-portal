import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';

const config = JSON.parse(await readFile(new URL('./economics.json', import.meta.url), 'utf8'));

function calculate(scenario) {
  const p = { ...config.defaults, ...scenario.overrides };
  for (const [key, value] of Object.entries(p)) {
    if (typeof value === 'boolean') continue;
    if (!Number.isFinite(value) || value < 0) throw new Error(`Invalid non-negative numeric assumption: ${key}`);
  }
  for (const key of ['outputVatRate', 'processorRate', 'feeVatRate', 'refundReserveRate']) {
    if (p[key] > 1) throw new Error(`${key} must be a fraction between zero and one.`);
  }
  if (p.fxZarPerUsd <= 0 || p.priceZar <= 0 || p.inferenceRetryMultiplier < 1) throw new Error('Invalid price, FX or retry multiplier.');
  let members = scenario.payingMembers;
  if (members == null) {
    if (!Number.isInteger(scenario.registeredAccounts) || scenario.registeredAccounts < 0 ||
        !Number.isFinite(scenario.conversionRate) || scenario.conversionRate < 0 || scenario.conversionRate > 1) {
      throw new Error('Invalid account/conversion scenario.');
    }
    members = Math.floor(scenario.registeredAccounts * scenario.conversionRate);
  }
  if (!Number.isInteger(members) || members < 0) throw new Error('Paying members must be a non-negative integer.');
  const feeVatFactor = 1 + (p.vatRegistered && p.feeVatRecoverable ? 0 : p.feeVatRate);
  const salesPerMember = p.priceZar / (p.vatRegistered ? 1 + p.outputVatRate : 1);
  const processorPerMember = (p.priceZar * p.processorRate + p.processorFixedZar) * feeVatFactor;
  const inferencePerTurn = (p.combinedInputTokensPerTurn * p.inputUsdPerMillion +
    p.combinedOutputTokensPerTurn * p.outputUsdPerMillion) / 1e6 * p.fxZarPerUsd * p.inferenceRetryMultiplier;
  const inferencePerMember = inferencePerTurn * p.turnsPerMember;
  const voicePerMember = p.cloudVoiceReplies * p.charactersPerVoiceReply / 1000 * p.voiceUsdPerThousandCharacters * p.fxZarPerUsd;
  const variablePerMember = processorPerMember + p.priceZar * p.refundReserveRate +
    p.supportPerMemberZar + inferencePerMember + voicePerMember + p.licencePerMemberZar;
  const contributionPerMember = salesPerMember - variablePerMember;
  const fixedCosts = p.workersFixedUsd * p.fxZarPerUsd + p.otherFixedZar + p.licenceFixedZar +
    p.payoutsPerMonth * p.payoutFeeExVatZar * feeVatFactor;
  const trialInference = p.newTrialAccountsPerMonth * p.trialTurnsPerAccount * inferencePerTurn;
  return {
    scenario: scenario.name, payingMembers: members, grossCollectionsZar: members * p.priceZar,
    netSalesZar: members * salesPerMember, processorPerMemberZar: processorPerMember,
    inferencePerTurnZar: inferencePerTurn, inferencePerMemberZar: inferencePerMember,
    cloudVoicePerMemberZar: voicePerMember, contributionPerMemberZar: contributionPerMember,
    fixedCostsZar: fixedCosts, trialInferenceZar: trialInference,
    operatingContributionZar: members * contributionPerMember - fixedCosts - trialInference,
    breakEvenPayingMembers: contributionPerMember > 0 ? Math.ceil((fixedCosts + trialInference) / contributionPerMember) : null,
    commercialReadiness: config.commercialCostsVerified ? 'INPUTS_MARKED_VERIFIED_REQUIRES_LAUNCH_REVIEW' : 'INCOMPLETE_UNQUOTED_COMMERCIAL_COSTS',
    unknownCosts: config.unknownCosts
  };
}

if (process.argv.includes('--self-check')) {
  const base = calculate({ name: 'base', payingMembers: 22 });
  assert.equal(base.grossCollectionsZar, 440);
  assert.ok(Math.abs(base.processorPerMemberZar - 3.036) < 1e-9);
  assert.ok(Math.abs(base.inferencePerMemberZar - 8.39646) < 1e-9);
  assert.equal(base.breakEvenPayingMembers, 56);
  assert.equal(calculate({ name: 'conversion', registeredAccounts: 22, conversionRate: 0.25 }).payingMembers, 5);
  assert.equal(calculate({ name: 'unviable', payingMembers: 100, overrides: { cloudVoiceReplies: 150 } }).breakEvenPayingMembers, null);
  assert.ok(calculate({ name: 'trial burn', payingMembers: 100, overrides: { newTrialAccountsPerMonth: 500 } }).trialInferenceZar > 279);
  assert.throws(() => calculate({ name: 'invalid', payingMembers: -1 }));
  assert.throws(() => calculate({ name: 'invalid', payingMembers: 1, overrides: { fxZarPerUsd: 0 } }));
  console.log('Economics sanity checks passed: cohort ceiling, fees, inference, break-even, fractional conversion, negative margin and invalid inputs.');
} else {
  const results = config.scenarios.map(calculate);
  if (process.argv.includes('--json')) {
    console.log(JSON.stringify({ researchDate: config.researchDate, results }, null, 2));
  } else {
    console.table(results.map(r => ({
      scenario: r.scenario, members: r.payingMembers,
      grossZar: r.grossCollectionsZar.toFixed(2), aiPerMemberZar: r.inferencePerMemberZar.toFixed(2),
      voicePerMemberZar: r.cloudVoicePerMemberZar.toFixed(2), trialBurnZar: r.trialInferenceZar.toFixed(2), contributionPerMemberZar: r.contributionPerMemberZar.toFixed(2),
      operatingContributionZar: r.operatingContributionZar.toFixed(2), breakEvenMembers: r.breakEvenPayingMembers ?? 'not viable'
    })));
    console.log('Planning estimates, not profit or a forecast. Free credits are not deducted. Unknown commercial/tax costs block a launch conclusion.');
    console.log(`Unverified inputs: ${config.unknownCosts.join('; ')}.`);
  }
}
