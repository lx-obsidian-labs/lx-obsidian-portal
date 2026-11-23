# R20 economics and commercial model

Research IDs refer to [01-RESEARCH.md](01-RESEARCH.md). Inputs are dated assumptions, not revenue forecasts or supplier contracts. Use [economics.json](economics.json) and run:

```powershell
node docs/premium-2027/economics.mjs
node docs/premium-2027/economics.mjs --json
node docs/premium-2027/economics.mjs --self-check
```

## Known numbers and unknown liabilities

- R20/month is the intended customer price. 22 registered accounts at 100% payment would produce only **R440 gross/month**. There is no evidence that all, or any, will pay.
- Payfast list card fee is 3.2% + R2 excluding VAT (S04): **R2.64 ex VAT, R3.036 including 15% VAT on that fee**, or 15.18% of a R20 charge before other costs.
- Payfast publishes payout R8.70 ex VAT and refund R2 ex VAT. Payout batching matters at this cohort size. Do not allocate one payout per customer by accident.
- VAT-registered sales at an inclusive R20 leave `20 / 1.15 = R17.3913` before processor fees. Non-registered sales do not simply deduct output VAT. Model both; actual input VAT recovery and electronic-service obligations need accountant confirmation (S11).
- Workers Paid minimum is $5/month (S09). R18/$ is an **illustrative FX input**, not a market quote. This yields R90 before other charges/tax.
- Current 70B fallback list price is $0.293/M input and $2.253/M output (S10). A turn with 6000 combined input tokens and 600 combined output tokens is about **R0.0559764** at the assumed FX. 150 turns cost **R8.39646**, before retries, storage, support or platform costs.
- A model-specific NVIDIA production price/permission, commercial TMDB quote and any content-distribution price are **unknown**. They must not be treated as zero in a launch decision.

## Base planning case

Customer price R20; conservative processor fee including VAT; refund reserve 2% of gross; support allowance R2/member; assistant 150 turns with the above token load; no cloud voice. Fixed operating budget R300/month is a placeholder including the R90 Worker estimate plus R210 other fixed operations. Four monthly payout fees are added separately, not hidden inside that placeholder.

For the non-VAT-registered, unreclaimed-fee case:

```text
Contribution/member = 20 - 3.036 - 0.40 - 2.00 - 8.39646 = R6.16754
Fixed operating cash including 4 payouts = 300 + (4 × 8.70 × 1.15) = R340.02
Illustrative break-even = ceil(340.02 / 6.16754) = 56 paying members
```

This excludes unquoted catalogue/AI licence/content costs and founder compensation. It is **not launch clearance**, even when the arithmetic contribution is positive. The calculator labels every scenario incomplete until those quotes are supplied.

Base acquisition is deliberately zero because no new-signup volume has been measured. That does not make free trials costless. The calculator separately charges `newTrialAccountsPerMonth × trialTurnsPerAccount × inferencePerTurn`; its 500-new-trials stress case adds about R279.88/month at full trial use. Report trial acquisition/burn separately from paid-member contribution and enforce a shared trial spending ceiling.

## Stress cases that shape the product

| Stress | Implication |
|---|---|
| VAT-registered sales with conservative unreclaimed input-fee VAT | Less net sales revenue at the same displayed R20; run separately, do not double-count tax |
| 25% conversion of 22 accounts | Five whole paying accounts under the calculator's conservative floor; not enough to recover the placeholder fixed budget |
| 500 paying members | Useful capacity scenario, not a growth prediction; include actual paying/support/usage distributions |
| 300 turns/member at 12,000 input + 1000 output tokens/turn | AI cost can exceed R20 before payment fees; unlimited plans are not viable at this assumed load |
| Paid neural TTS, 150 replies × 500 characters at $0.030/1000 | R40.50/member at assumed FX, voice alone; keep device TTS for launch |
| R2 catalogue allocation/member plus R500 monthly licensing minimum | Recalculate break-even; this placeholder is not a licence quote or proof of film rights |
| Free-tier credit disappears / all turns use paid fallback | Must remain financially bounded; free credits are excluded from base savings |
| Free signups consume their lifetime allocation but never convert | Trial inference is an acquisition cost, not paid-member usage; include it in monthly cash burn and defend against repeated-account abuse |

A recurring charge also carries future service/refund obligations. A discounted annual plan or 12-month referral reward creates liability long before infrastructure or content costs are known. Defer both.

### Calculator reference output

These are modelled monthly operating contributions after the configured fees, reserves, support, inference and fixed costs; they are not accounting profit. Unquoted commercial costs remain excluded and block a viability conclusion.

| Scenario | Paying accounts | Gross collections | Modelled operating contribution | Modelled break-even |
|---|---:|---:|---:|---:|
| 25% of current 22, floored to whole accounts | 5 | R100 | -R309.18 | 56 |
| All current 22 pay | 22 | R440 | -R204.33 | 56 |
| Reference base | 100 | R2,000 | R276.73 | 56 |
| VAT-registered, no assumed input-fee recovery | 100 | R2,000 | R15.86 | 96 |
| Base plus 500 new full-use free trials | 100 | R2,000 | -R3.15 | 101 |
| Base plus illustrative R500 fixed/R2-member licensing | 100 | R2,000 | -R423.27 | 202 |
| High-token/300-turn stress | 100 | R2,000 | -R2,777.69 | No break-even at negative per-member contribution |
| 150 cloud voice replies/member | 100 | R2,000 | -R3,773.27 | No break-even at negative per-member contribution |

Re-run the calculator after changing any assumption; this dated table is not automatically rewritten.

## Budget and quota policy

Proposed planning ceilings: R8.50 AI inference/member/month at reference usage; R0 cloud voice by default; global spending envelope tied to actual collections and founder-approved runway. Record both reserved and actual provider costs, including failed requests and fallbacks. Customer successful-turn counts alone understate spend.

The proposed 150-turn allowance uses nearly the entire R8.50 reference inference budget. It is acceptable only if measured p95 usage, retry overhead and provider quote leave room. If not, optimize context/model routing or change the communicated allocation **before purchase**, not silently mid-period. Published allowances must be deliverable under the worst supported routing policy.

## Payment and commercial decisions

Monthly R20 only for the first two renewal cycles. South Africa first; international users receive accurate availability messaging rather than an automatically converted local price. Payfast is provisional because its published local recurring product can be researched; Paystack comparison remains open due blocked research pages. Capture merchant approval for the actual product, not merely API credentials.

Do not sell catalogue-derived data/images until applicable commercial permission is settled (S03). Do not present the companion subscription as authorization to stream third-party titles. If the licensed companion offer cannot support R20, keep the public product free while renegotiating scope/cost; do not use optimistic subscriber counts to hide a negative contribution.

## Financial observation after launch

Daily: gross collected, verified grants, outstanding paid-but-not-active cases, reversals and unmatched transactions. Weekly: net revenue after applicable tax/fees, AI/token distribution, support minutes, actual fixed costs and cancellation reasons. Monthly: renewal retention and contribution by paid cohort. Cash receipts, accounting revenue and contribution are different measures.

No LTV/CAC or annual revenue claim from 22 registrations. Calculate payback only after actual acquisition spend, service cost and at least two renewal cohorts exist.
