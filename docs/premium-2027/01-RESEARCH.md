# Research register and implications

Researched **29 October 2026**. Source IDs are used throughout this programme. “Verified” means the cited page was retrieved, not that Vista has an approved account, contract or completed integration.

## Official sources successfully reviewed

| ID | Primary source | Finding used in the plan |
|---|---|---|
| S01 | [NVIDIA NIM general FAQ](https://docs.api.nvidia.com/nim/docs/product.md) | Developer Programme access is for prototyping/research/development/testing; the FAQ distinguishes production and commercial licensing. Production includes serving real end users. Confirm the applicable hosted-model/account terms before selling access. |
| S02 | [NVIDIA NIM developer overview](https://developer.nvidia.com/nim) | Free hosted endpoints are presented for prototyping; enterprise/partner options are presented for production. Hosted API access and self-hosted GPU licensing are different deployment decisions. |
| S03 | [TMDB API FAQ](https://developer.themoviedb.org/docs/faq) | Free developer use is non-commercial with attribution; revenue-oriented use requires contacting TMDB about a commercial licence. Attribution does not itself grant a commercial licence or playback rights. |
| S04 | [Payfast fees](https://payfast.io/fees/) | Published card fee: 3.2% + R2 per transaction, excluding VAT. Payout fee R8.70 excluding VAT; refund fee R2 excluding VAT. These are published list fees, not a negotiated Vista quote. |
| S05 | [Payfast subscriptions](https://payfast.io/features/subscriptions/) | Scheduled recurring card payments; subscription API supports viewing, updating, pausing and cancelling. A listed EFT method does not establish recurring EFT support. |
| S06 | [Official Payfast PHP SDK](https://github.com/Payfast/payfast-php-sdk) / [README](https://raw.githubusercontent.com/Payfast/payfast-php-sdk/master/README.md) | Official examples exist for custom checkout, recurring management, transaction queries and refunds. Sandbox support differs by operation. This is a reference for contract/test vectors, not a PHP dependency to paste into a Cloudflare Worker. |
| S07 | [Google Play Payments policy](https://support.google.com/googleplay/android-developer/answer/9858738?hl=en) | Play-distributed digital subscriptions generally require Play billing unless a documented exception/program applies. External-payment permissions depend on channel, region and programme eligibility. |
| S08 | [Firebase custom claims](https://firebase.google.com/docs/auth/admin/custom-claims) | Set claims in a privileged server environment; changes appear in newly issued tokens. Claims are bounded to 1000 bytes and are for access control, not profile storage. A claim is not a payment ledger. |
| S09 | [Cloudflare Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/) | Paid plan minimum $5/month; 10 million requests and 30 million CPU-ms included; static asset requests are free. Workers pricing does not include unlimited model inference. Logs pricing changes from December 2026, requiring a pre-launch recheck. |
| S10 | [Workers AI pricing](https://developers.cloudflare.com/workers-ai/platform/pricing/) | Daily 10,000-neuron allowance; paid overage $0.011/1000 neurons. Current 70B fallback lists $0.293/M input and $2.253/M output tokens. Per-account daily allowance is not per-user entitlement. |
| S11 | [SARS VAT](https://www.sars.gov.za/types-of-tax/value-added-tax/) | Page states current standard VAT 15%, and describes 2026 threshold changes. Model VAT-registered/non-registered cases; verify the actual company's position with its accountant rather than assuming either. |
| S12 | [Information Regulator POPIA resources](https://inforegulator.org.za/popia/) | Guidance covers lawful processing, information officers, children's information, direct marketing and data-subject rights. The page expressly includes push notifications in electronic communication examples. Operational announcements and sales campaigns require separate treatment. |
| S13 | [Consumer Protection Act, government source](https://www.gov.za/documents/consumer-protection-act) | Official route to the Act and amendments. Subscription terms need clear price/renewal/cancellation and review of applicable consumer rights; no invented statutory cancellation deadline is used in this plan. |
| S14 | [GitHub large-file guidance](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github) | Ordinary Git blocks files above 100 MiB and warns above 50 MiB. GitHub recommends releases for large-binary distribution. Current signed APK is about 21.3 MiB and remains in the repository as requested; monitor growth. |

## Access limitations: do not cite as verified facts

- Paystack South Africa pricing, subscriptions and webhook pages returned HTTP 403. It remains a comparison candidate; no current Paystack fee, minimum charge, supported recurring method or merchant approval is asserted.
- Payfast developer documentation is JavaScript-rendered and returned only its shell. The official SDK and product pages were readable, but exact ITN canonicalization, retry policy and API authentication must be verified against current full documentation/test vectors during the billing spike.
- Android developer-verification and Play Billing security guide requests timed out. Confirm current sideload/developer-verification requirements and any Play release plans by **6 November**, then again in December. Do not invent a January blocking rule from an unread page.
- Initial guessed NVIDIA FAQ/terms URLs returned 404; S01/S02 supersede them. The model-specific terms and account contract still require review.

## What changed because of research

1. **No business model based on “NVIDIA is free forever.”** Keep NVIDIA as the desired provider, but obtain production permission/capacity and cost evidence. A live HTTP 200 proves connectivity, not commercial entitlement.
2. **Commercial catalogue access is a first-week dependency.** A R20 subscription changes the revenue purpose; a catalogue API key and an iframe do not prove content distribution rights.
3. **Use a provisional Payfast hosted-card checkout design.** Its local recurring product and published fees are verifiable. Final choice depends on merchant approval, end-to-end tests, and an actual quote; do not build two gateways concurrently.
4. **Device TTS stays the January default.** S10 lists Aura-2 at $0.030/1000 characters; at an assumed R18/$, a 500-character reply is R0.27 before other costs. 150 replies would be R40.50. This is not a free voice replacement.
5. **Separate paid entitlements from identity and countdowns.** Custom claims can be a UI hint but cannot settle delayed webhooks, refunds, paid-through dates or duplicate renewals.
6. **Start narrow.** 22 accounts support interviews and a small pilot; they do not justify several independent apps, many billing providers or a recommender training pipeline.

## Source freshness and evidence capture

| Evidence | Recheck | Required result |
|---|---|---|
| AI production terms, model licence and commercial catalogue agreement | Before paid beta and 15 December | Written applicable terms/approval, account limits, quote and fallback route |
| Payment quote, VAT on fees, payout/refund costs, recurrence/ITN specification | Before implementation and 15 December | Versioned reference, approved merchant, sandbox vectors and permitted low-value live test |
| VAT treatment and consumer contract | Before taking money | Accountant/legal review of this company's facts; public terms reflect the chosen product |
| Workers/AI/log pricing and FX | 15 December and launch decision | Refreshed calculator inputs; no free-tier credits needed for viability |
| Android/Play channel requirements | 6 November and 15 December | Channel-specific decision recorded; sideload checkout is not assumed compliant for a later Play build |

Record URLs, access dates, account-specific restrictions and quote identifiers in private operational evidence. Keep contracts, personal account exports, payment tokens and credentials out of these public-facing docs. Third-party pages can change; the values above are a dated research snapshot.
