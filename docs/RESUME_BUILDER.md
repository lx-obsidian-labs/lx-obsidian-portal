# AI Résumé Builder

Maintainer: LX Obsidian Labs · https://www.lxobsidianportal.co.za

## Product goals

Make Careers useful between hiring rounds. A student, graduate, career changer or experienced professional should be able to turn a few real examples into a CV they can use for any employer. An expression of interest in LX remains a separate email action.

| Step | Experience | Acceptance criterion |
|---|---|---|
| Start | Follow **Build your CV** from Careers. Enter name, contact, current focus and evidence. Add employment, placements, volunteering, coursework and projects as appropriate. | Usable with a keyboard and on a narrow mobile screen. Visitors can create a draft without signing in. |
| Tailor | Paste relevant job requirements and choose a summary or experience-bullet suggestion. | AI sees bounded career text and advert context after an explicit action. It never receives the structured name, contact fields or the full saved draft. |
| Review | Compare the original and proposed wording. Edit, apply or dismiss the suggestion. | The text field changes only when the candidate applies a suggestion. New numbers are rejected by the API. The candidate checks non-numerical claims. |
| Export | Review readiness checks, download editable Word, or use the browser's A4 print/PDF control. | The document is plain, single-column and text selectable. Export excludes the advert, checks and site chrome. |
| Resume | Optionally save the draft on the current browser or clear it. | No default persistence or account sync. Clearing also removes the saved local draft. |

## Architecture and operations

- /resume-builder is a static page served by the existing Cloudflare Pages build. The Careers page keeps its canonical URL and email flow.
- The preview and Word export share the same data model. The DOCX is an Open XML package generated locally and the PDF uses print CSS. Descriptive headings and normal text support text extraction. ATS compatibility varies by product, so fixture-based text extraction and actual upload tests should precede any parser-specific claims.
- POST /api/resume-suggest uses existing server-side NVIDIA credentials, with no alternate provider for candidate text. Requests have same-origin checks, bounded bodies, explicit modes, strict output validation and a 25-second shared upstream deadline. Structured contact fields are never sent. The API masks common phone, email and link patterns in free text, which may still contain other personal information. Users review wording for facts.
- D1 stores only an HMAC-derived daily network bucket and counts. Maximums are 12 per daily network bucket and 200 global requests per UTC day. The next successful request removes earlier buckets. A missing binding, missing table or upstream failure leaves manual editing and export available.
- Apply only migrations/0004_resume_ai_quota.sql to the configured D1 database before enabling AI on the production deployment. Verify the exact Pages deployment and use a synthetic CV to smoke-test the route. Roll back through the existing Pages project. The isolated quota table can remain after rollback.
- Keep CV text, adverts and contact details out of logs, analytics and test artifacts. If tracking is added later, measure aggregate starts, exports, accepted suggestions, latency and failure rates without collecting CV contents.

## Quality gates and next iterations

1. Validate editing and export for three representative profiles: a TVET graduate with coursework, a career changer with transferable skills and an experienced candidate with multiple jobs. Test Unicode, long URLs, multi-page output, reloading saved drafts and approved or dismissed suggestions. Run the site build and automated tests, then exercise the live provider with synthetic text.
2. Benchmark usefulness with volunteer candidates. Track time to first export, completion rates, accept/edit/dismiss rates, and unsupported-claim frequency across a synthetic corpus. Evaluate whether the exported text retains reading order in common parsing tools. Start with a target of a usable first draft within ten minutes and investigate repeated abandonment points.
3. Prioritise enhancements using observed friction. Likely candidates are more section ordering controls, a master CV with role-specific variants, an evidence-to-requirement checklist, cover letters, and PDF/DOCX import with an extraction review. Test each addition for mobile complexity and privacy before enabling it.
