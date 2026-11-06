# Engineering case study: LX Obsidian Portal

**Category:** Web engineering, static-site build tooling, Cloudflare deployment integration

## Problem and context

LX Obsidian Labs maintains a web presence that introduces its products and supports business enquiries. The repository includes a custom build pipeline for assembling deployable site assets.

## Implementation evidence

The project manifest declares a Node-based build command, a verification command and a Wrangler deployment command. The build script copies HTML, JavaScript, assets and Cloudflare Functions into `dist/`, generates downloadable resources and assembles route-specific pages. The verification script checks required output assets, routing assumptions and JavaScript syntax.

A GitHub Actions workflow runs the build on pushes and pull requests to `master`, with Node 22 and locked dependency installation.

## Engineering decisions

- **Custom build step:** Assembles a deployable `dist/` directory rather than relying on a frontend framework's standard build.
- **Route checks:** The verifier asserts the presence of important pages and avoids a duplicate PDF entry point.
- **Syntax validation:** Generated JavaScript is checked for syntax errors.
- **CI sequencing:** Build and verification are combined in `npm run build` because verification depends on generated output.

## Evidence and limitations

**Confirmed by source:** `package.json`, `scripts/build.js`, `scripts/verify.js`, `.github/workflows/verify.yml`.

**Not yet independently confirmed:** A passing CI run, production deployment health, successful end-to-end lead submissions, security testing, performance results and user impact.

## Reproduction

```bash
npm ci
npm run build
```

Requires Node.js 22. Do not run `npm run deploy` without authorized deployment credentials and release approval.

## Next improvements

- Capture a passing workflow run and link it here
- Add automated browser tests for navigation and contact flow
- Document Cloudflare configuration and rollback
- Publish verified screenshots and performance measurements

**Maintainer:** LX Obsidian Labs — Nathan Vilane
