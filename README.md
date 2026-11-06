# LX Obsidian Portal

**Company website and product portfolio | LX Obsidian Labs**

This repository supports the web presence of LX Obsidian Labs and its software products.

**Website:** https://www.lxobsidianportal.co.za

**Development stage:** Active Development / Iterative Release (production status must be checked against the deployed site).  
**Verification:** Build checks and contact API tests are configured; passing CI and live lead-delivery results remain unverified.  
**Implemented scope under review:** Static marketing pages, downloadable resources, Cloudflare Pages Functions contact endpoint and D1-backed lead persistence.  
**Next milestone:** Confirm CI for contact API regression coverage, then exercise production lead persistence and notification delivery with controlled test data.

The portfolio [evaluation standard](https://github.com/lx-obsidian-labs/lx-obsidian-labs/blob/main/PORTFOLIO_EVALUATION.md) separates maturity from engineering quality. Planned features do not reduce the engineering assessment.

> **Verification status:** The repository's exact build process, framework, deployment branch and current live status require confirmation against its source and hosting configuration.

## Project goals

- Present LX Obsidian Labs and its engineering services
- Help visitors explore software products and case studies
- Provide clear paths for business enquiries
- Maintain responsive, accessible and secure user experiences

These are project objectives, not assertions that every capability has been tested.

## Local development

Inspect the repository structure and build manifest first. Use the package manager, runtime version and commands declared by the project. Never publish production environment variables.

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
