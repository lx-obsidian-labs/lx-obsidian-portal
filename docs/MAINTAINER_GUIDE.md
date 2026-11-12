# LX Obsidian Portal maintainer guide

Engineering attribution: LX Obsidian Labs — https://www.lxobsidianportal.co.za

## System overview
The portal is a static HTML/CSS/JavaScript site assembled by Node.js, not a React application. Cloudflare Pages serves dist; Pages Functions provide server-side endpoints. Wrangler config declares D1 binding DB. Product applications may have independent repositories and release lifecycles.

## Reproduce
Use Node.js 22. Run npm ci, npm test, npm run build, then npm run dev (builds and serves dist through Wrangler). For a preview without rebuilding run npx wrangler pages dev dist after building. Never use production secrets or customer records locally.

## Source map
scripts/build.js assembles output, synchronises shared navigation/footer from index.html, generates resources, normalises links and publishes nested PDF/Synapse entry points. scripts/verify.js validates output assets, links, JS syntax and marketing regressions. css/lx.css is canonical; legacy saas.css must not override it. functions/api/contact.js handles lead capture. tests/contact-api.test.cjs uses mocked D1. .github/workflows/verify.yml runs tests and build on master.

## Release
Verify CI for the exact commit, preview routes /, /pdf, /synapse/web, /contact and /downloads, inspect browser console and test controlled lead persistence. Deploy only with authorised credentials. npm run deploy targets lxobsidianmainsite. Verify actual Pages project and domain configuration before deploying. Roll back using last known-good Pages deployment; database changes need separate recovery.

## Continuity
Authorised successors need GitHub organisation, Cloudflare account, domain/DNS and billing access, plus private secret recovery and product repository access. Never commit credentials. Record named owners privately. A successor must independently clone, build, preview and demonstrate rollback; no such exercise is verified.

## Known blockers
Migration scripts reference lx-portal while Wrangler declares lx-obsidian-db; the referenced initial SQL file was not retrievable. Do not execute migration or seed commands until reconciled. Production lead delivery and current deployment remain unverified. Build excludes docs from static output; the GitHub repository may still be public. Never place private operational details in it.
