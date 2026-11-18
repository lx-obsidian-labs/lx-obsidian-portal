# LX CV builder product and release plan

LX Obsidian Labs · 18 November 2026

## Product goal

Help a student, graduate, career changer or experienced applicant produce an accurate CV for a chosen job, from a phone, without creating an account. The Careers page is the entry point. Measure success by people completing an export and by the quality of CVs they can explain in an interview.

## Candidate journey

| Step | First release | Next improvements |
| --- | --- | --- |
| Start | Open the free editor from Careers. Enter a target role or begin with experience. | Offer a fictitious example for TVET students, recent graduates and career changers. Add local DOCX and text-PDF import with a review step before importing fields. |
| Collect evidence | Capture coursework, work placements, volunteering, freelance work, projects and formal experience. Preview education and skills in the same document. | Add prompts that draw out tools used, responsibilities, outcomes and verifiable numbers. Let applicants reorder entries and lead with education when that is their strongest evidence. |
| Tailor | Paste a short bullet or summary and relevant requirements. Review the optional AI rewrite and copy useful wording. | Group job requirements into evidenced, needs context and missing. Cite the source entry for every match. Ask a follow-up question when evidence is unclear. Let the user accept, edit or discard each suggestion and undo accepted changes. |
| Review | See the live one-column CV, remove empty sections and review every claim before export. | Flag inconsistent dates, duplicated claims, vague bullets, unusually long sections and missing contact information. Explain each issue and offer a direct edit path. |
| Export | Download editable DOCX or plain text. Use the browser print dialog for a selectable-text A4 PDF. | Add direct PDF download where print workflows cause friction. Offer a master CV and role-specific copies saved on the device. |

## Design and implementation

- Use the existing Careers URL, navigation and footer. Publish a single canonical public tool route linked from Careers and the sitemap. The builder does not send CVs to recruiting or create applications.
- Use a responsive editor and readable document preview. Stack the preview on narrow screens with a jump link. Test low-cost Android devices, keyboard use, zoom and screen-reader labels. Keep draft saving opt-in for shared computers.
- Keep the CV in the browser. Exports use the same document content as the preview. Single-column text, standard section names and contact details in the document body are intentional. Verify extracted reading order before claiming parser compatibility, and follow an employer's stated upload format.
- Keep AI on a dedicated Cloudflare Pages endpoint. Send only manually selected excerpts after agreement. Screen obvious identifiers, bound input and output, use server-only inference and ask the model to rewrite from user evidence. Treat the user as the final fact checker. Numbers added by the model are rejected, but that check cannot certify every factual claim.
- Keep rate-limit counters in a dedicated D1 database. The first release allows eight suggestions per connection IP per UTC day, with a shared limit of 100. Store a daily keyed IP digest and counts. Delete expired rows on use. Avoid inference request bodies and response text in analytics and logs. Set RESUME_AI_DISABLED=true and redeploy if inference needs to stop while the editor stays available.

## Release gates and measurement

| Gate | Evidence |
| --- | --- |
| Usable from a phone | A student with no job history can complete a CV using coursework and a placement. An experienced applicant can add several roles without horizontal scrolling. Check keyboard-only navigation and 200% zoom. |
| Exports carry the same content | Extract the text from generated PDF and DOCX. Check names with accents, standard section order, long entries over multiple pages and absence of site navigation or placeholder copy. Include Word and LibreOffice in the export compatibility pass. |
| AI stays within the source | Review at least 30 anonymised examples across students, job changers and experienced applicants. Require zero invented credentials, employers and metrics in the accepted benchmark outputs. Use adversarial job ads and malformed requests in testing. |
| Service degrades gracefully | Verify successful inference, opt-out, rate limiting, provider downtime and blocked browser storage. Confirm that manual editing and export are usable in each case. |
| Measure where users struggle | Start with a consent-aware pilot. Measure editor opens, first completed export, AI suggestion success and export failures using counts only. Ask a small set of users where they hesitated and prioritise changes by completion rate and review quality. |

## Delivery order after first release

1. Pilot with South African early-career users, career changers and experienced applicants. Fix completion and export friction first.
2. Ship evidence-linked job matching and reviewable edits. Start with a small synthetic and opt-in evaluation set. Show observable gaps and evidence, without an applicant-tracking-system score or prediction of hiring outcomes.
3. Add on-device import and CV versioning after measuring value and validating local processing. Review retention, data flows and applicable South African requirements before introducing any cloud sync, account storage or application integrations.

## References

- [Greenhouse guidance on résumé parsing and layout](https://support.greenhouse.io/hc/en-us/articles/200989175-Unsuccessful-resume-parse)
- [Cloudflare Workers AI data usage](https://developers.cloudflare.com/workers-ai/platform/data-usage/)
- [South Africa's Protection of Personal Information Act](https://www.justice.gov.za/legislation/acts/2013-004.pdf)
