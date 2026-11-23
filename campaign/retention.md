# LX Insider — 90-day retention programme

Goal: keep 30 active testers engaged through day 90, then grow the reliable core to 100. Retention is earned through feedback loops, recognition and visible fixes — not through pressure or review incentives.

## Rhythm

| Moment | Trigger | Action | Owner |
|---|---|---|---|
| Day 0–14 | Consent → install | Onboarding email (template 2), assignment A1, confirmation within 2 working days | Operator |
| Weekly | Each challenge | Challenge email (template 3), Thursday nudge to non-submitters (once), weekly “you reported, we fixed” note | QA lead |
| Day 30 | 30–36 days after Play add | Check-in: what’s working, what’s still broken, adjust preferred categories | Operator |
| Day 60 | 60–66 days after Play add | Check-in + progress summary (assignments, verified bugs, points) | Operator |
| Day 90 | 90–96 days after Play add | Programme review, reward approval (template 4), invite to next intake | Programme manager |
| ≥14 days inactive | No activity logged | One re-engagement attempt (template 5); then mark Inactive, no penalty | Operator |
| Testing completed or ≥25 points | Verified work | Recognition candidate: thank-you post or profile mention, only with written consent | Community lead |

`node campaign/insider-dashboard.mjs --markdown` reports all trigger counts; the JSON output includes pseudonymous applicant IDs for operator follow-up. `node campaign/insider-dashboard.mjs --validate` blocks a register with unverified active statuses, missing consent or broken opt-in dates.

## Rules

- Rewards follow verified work (assignments, reproducible bugs, useful feedback), never ratings, reviews or raw installs.
- No reward amount or prize is promised before management approval; recognition posts require consent.
- Re-engagement is one attempt only — no spam, no guilt, no repeated DMs.
- Inactive testers can rejoin any future intake without penalty.
- Paid testing projects are separate, approved contracts — never framed as employment.
- Use fictional business records throughout; no real customer or financial data in test reports.

## Weekly retention checklist

- [ ] Publish the fix list: what testers reported, what actually changed, what is still open.
- [ ] Verify reproducibility before awarding bug points.
- [ ] Update `campaign/insider-register.csv` statuses and dates (including `last_active_at`).
- [ ] Run `--validate`; fix every error before reporting.
- [ ] Run `--markdown`; file the daily report.
- [ ] Follow up the alert IDs from the retention section: re-engagement, check-ins, recognition.
