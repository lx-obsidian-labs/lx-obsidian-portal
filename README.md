# LX Obsidian Portal

**Company website and product portfolio | LX Obsidian Labs**

This repository supports the web presence of LX Obsidian Labs and its software products.

**Website:** https://www.lxobsidianportal.co.za

**Development stage:** Active Development / Iterative Release (production status must be checked against the deployed site).  
**CI evidence (2026-11-07):** [Verify portal run 42397050121](https://github.com/lx-obsidian-labs/lx-obsidian-portal/actions/runs/42397050121) completed successfully at commit `a5f96f28d682d91931970f3be730b28e2e5f58a8`. Production lead delivery remains unverified.  
**Verification:** Historical CI evidence is linked above; current HEAD and live lead delivery remain unverified.  
**Implemented scope under review:** Static marketing pages, downloadable resources, Cloudflare Pages Functions contact endpoint and D1-backed lead persistence.  
**Next milestone:** Confirm CI for contact API regression coverage, then exercise production lead persistence and notification delivery with controlled test data.

The portfolio [evaluation standard](https://github.com/lx-obsidian-labs/lx-obsidian-labs/blob/main/PORTFOLIO_EVALUATION.md) separates maturity from engineering quality. Planned features do not reduce the engineering assessment.

> **Architecture:** Static HTML/CSS/JavaScript, custom Node.js build, Cloudflare Pages Functions and D1. CI targets master; actual production deployment configuration remains unverified.

## Project goals

- Present LX Obsidian Labs and its engineering services
- Help visitors explore software products and case studies
- Provide clear paths for business enquiries
- Maintain responsive, accessible and secure user experiences

These are project objectives, not assertions that every capability has been tested.

## Local development

Use Node.js 22. Run `npm ci`, `npm test`, `npm run build`, then `npx wrangler pages dev dist` for a built-output preview. Never publish production environment variables. Migration scripts require reconciliation before use.

## Engineering and release checklist

- [ ] Confirm architecture, framework and runtime versions
- [ ] Document routes, integrations and content ownership
- [ ] Verify all contact and conversion forms end-to-end
- [ ] Check mobile responsiveness and keyboard accessibility
- [ ] Run lint, build and automated tests
- [ ] Audit third-party packages and exposed credentials
- [ ] Confirm Cloudflare deployment configuration and rollback procedure
- [ ] Record performance evidence and screenshots
- [ ] Document deployment ownership and third-party attribution

## Inspectable engineering evidence

- [Build and verification workflow](.github/workflows/verify.yml)
- [Contact API behavior tests](tests/contact-api.test.cjs)
- [Release and link-integrity checks](scripts/verify.js)
- [Engineering case study](docs/ENGINEERING_CASE_STUDY.md)

Run `npm ci`, `npm test` and `npm run build` locally using Node.js 22, as configured in GitHub Actions. The contact tests cover origin, content type, malformed input, validation, unavailable storage, failed writes, honeypot behavior and successful persistence using a mocked D1 binding. Passing results and production delivery are not asserted until independently confirmed.

## Maintainer

**LX Obsidian Labs** — Nathan Vilane

[Engineering portfolio](https://github.com/lx-obsidian-labs) · [Contact](mailto:vilanenathan@gmail.com)

## Maintainer documentation

- [Maintainer and continuity guide](docs/MAINTAINER_GUIDE.md)
- [Contact API reference](docs/CONTACT_API_REFERENCE.md)
- [LX Obsidian Labs documentation standard](docs/DOCUMENTATION_STANDARD.md)
