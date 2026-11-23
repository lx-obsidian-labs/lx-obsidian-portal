# LX Insider — weekly challenge schedule (first intake)

Weeks 1–2 are onboarding (assignments A1–A2). Challenges run weeks 3–8; weeks 9–12 are regression, recognition and the day-90 review. Dates are set when the intake opens (Gate A). Submissions: https://www.lxobsidianportal.co.za/insider-feedback, deadline Sunday 23:59 SAST. Fictional business records only.

| Week | Challenge | Assignment | Goal | Points |
|---|---|---|---|---|
| 3 | Money in, money out | A3 | Record 10 fictional income/expense entries across 3 categories; verify totals; edit and delete two entries | 2 (+1 on time) |
| 4 | Who owes you? | A4 + A5 | Create 5 fictional customers with outstanding balances; record part-payments; confirm balances and search | 2 (+1 on time) |
| 5 | Paperwork that looks professional | A6 | Create 2 invoices and 2 quotations; check PDF/share output for layout, totals and customer details | 2 (+1 on time) |
| 6 | Airplane mode business | A7 | One full work session offline: records, customers, documents; note exactly what needs connectivity | 2 (+1 on time) |
| 7 | Lose it, get it back | A8 | Back up, then restore; verify every record survives intact | 2 (+1 on time) |
| 8 | Slow phone, big books | A9 + A10 | 100+ records on the slowest device you have; TalkBack and large-font pass over core screens | 2 (+1 on time) |

Bug bounty within challenges: 3 points per verified reproducible bug, 5 for crashes or data loss. Points and tiers: see `docs/LX_INSIDER_CAMPAIGN.md` §6.

Operator checklist per week:
- [ ] Send challenge email (template 3) Monday.
- [ ] Log submissions in the register (`assignments_completed`, `bugs_verified`, `reward_points`).
- [ ] Verify reproducibility before awarding bug points.
- [ ] Thursday nudge only to testers who haven’t submitted (in-app or email, once).
- [ ] Publish the weekly fix list to testers (“you reported, we fixed”) — retention fuel.
- [ ] Run `node campaign/insider-dashboard.mjs --markdown` and file the daily report.
