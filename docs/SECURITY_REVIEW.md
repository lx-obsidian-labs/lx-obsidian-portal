# Security review and release risks

Engineering attribution: LX Obsidian Labs — https://www.lxobsidianportal.co.za

Source review, not penetration testing.

| Finding | Required action |
|---|---|
| Contact body limit | Actual streamed byte limit implemented; regression tests added, CI pending |
| Chat accepts user-controlled history and model | Schema and roles validated; model allowlist and quotas outstanding |
| Chat provider calls | 15-second per-attempt timeout and 25-second aggregate upstream deadline added; runtime verification pending |
| AI output contains executable HTML | Verify sandbox isolation and prohibit privileged execution |
| CSP permits unsafe-inline and unsafe-eval | Inventory dependencies, migrate incrementally and test report-only policy |
| Build copies functions into dist | Docs excluded and guard added; server-source packaging still requires route audit |
| D1 migration scripts disagree with config | Migration and seed commands disabled; recover authoritative schema before restoring them |
| Webhook may transmit personal information | Approve destination, retention and access policy |

Do not publish secrets, customer records or private recovery information. Verify deployed function headers separately from static _headers. Report vulnerabilities privately through the organisation's authorised contact channel. No remediation is claimed merely because it is documented.
