# Vista Watch — contact integration plan

Maintainer: LX Obsidian Labs. Primary mailbox: nathan@lxobsidianportal.co.za. Confirmed aliases route to the same mailbox: sales, info, support, accounts, hello, partnerships, admin, careers. Do not expose credentials or use personal Gmail addresses in application-facing resources.

## Release scope
- Android phone/tablet and Android TV: Settings > Help & support and About > Contact; link to https://www.lxobsidianportal.co.za/vista#vista-support. Add email intents using ACTION_SENDTO and mailto:support@lxobsidianportal.co.za; provide a copy-address and website fallback for TVs or devices with no mail app. Use remote-friendly focus states.
- iOS: Settings > Help & support; open mailto with the system URL handler, fall back to the support page or copy address. Native iOS preview remains separate from Android release scope.
- Technical support: support@lxobsidianportal.co.za. Suggested subject: Vista Watch | Support | app version. Collect device model, OS version, app version and optional issue description; never auto-attach viewing history, logs, authentication tokens, payment details or identifiers. Preview any diagnostics and obtain user consent before sending.
- Privacy, data rights and rights complaints: info@lxobsidianportal.co.za; link to /privacy-policy and /vista-terms. Billing: accounts@lxobsidianportal.co.za only when relevant; do not advertise subscriptions as live. Partnerships: partnerships@lxobsidianportal.co.za, primarily on website.
- Play Console and distribution listings: update developer support email after mailbox delivery is tested. Confirm sender-alias permissions before replying from aliases; all incoming aliases share one mailbox.

## Acceptance checklist
1. Inventory existing email constants, localized strings, manifests, support routes and documentation; replace outdated personal/company email addresses.
2. Verify inbound email for all aliases and outgoing mail for primary mailbox, then test alias From permissions separately.
3. Test mailto, copy-address fallback, browser fallback, TV D-pad navigation and accessibility on phone, TV and iOS where supported.
4. Verify privacy/terms links and no sensitive diagnostics sent automatically.
5. Run unit/UI tests, build Android and TV APKs and iOS preview where applicable; publish only after validation and release review.

Status: website contact changes planned for this commit; native integration and delivery tests are not completed by this document.
