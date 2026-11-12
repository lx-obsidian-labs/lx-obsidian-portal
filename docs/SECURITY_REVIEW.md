# Security review and release risks

Engineering attribution: LX Obsidian Labs — https://www.lxobsidianportal.co.za

Source review, not penetration testing.

| Finding | Required action |
|---|---|
| Contact body limit relies on Content-Length | Enforce actual byte limit and test chunked requests |
| Chat accepts user-controlled history and model | Validate schema, roles, provider/model and quotas |
| Chat provider calls lack explicit timeout | Add bounded timeouts and retry budget |
| AI output contains executable HTML | Verify sandbox isolation and prohibit privileged execution |
| CSP permits unsafe-inline and unsafe-eval | Inventory dependencies, migrate incrementally and test report-only policy |
| Build copies functions and docs into dist | Separate server source and internal documentation from static output after route audit |
| D1 migration scripts disagree with config | Locate authoritative schema and reconcile before executing |
| Webhook may transmit personal information | Approve destination, retention and access policy |

Do not publish secrets, customer records or private recovery information. Verify deployed function headers separately from static _headers. Report vulnerabilities privately through the organisation's authorised contact channel. No remediation is claimed merely because it is documented.
