# LX AI Résumé Builder product and release plan

Maintainer: LX Obsidian Labs · 18 November 2026

## Product goal

Help a student, graduate, career changer or experienced applicant produce an accurate CV for any employer, from a phone, without signing in. Careers is the entry point. The LX expression-of-interest email remains a separate action.

## Candidate journey

| Step | Current release | Planned improvement |
| --- | --- | --- |
| Start | Open the free builder from Careers. Enter a target role, or begin with education and experience. | Offer example drafts for a TVET student, recent graduate and career changer. Add on-device Word and text-PDF import with editable field mapping. |
| Capture evidence | Add employment, placements, volunteering, freelance work, courses and projects. | Ask what the applicant did, which tools they used and what outcome they can verify. Let them reorder entries and lead with education when that is their strongest evidence. |
| Tailor to a job | Paste relevant job requirements. Review exactly what goes to AI before requesting a summary or experience bullets. Compare, edit, apply or dismiss the result. | Map each requirement to a source entry, a follow-up question or a gap. Show the evidence next to each suggestion. Add undo and role-specific copies of one master CV. |
| Review | Use the live preview and readiness checks. Check every claim before export. | Flag inconsistent dates, duplicated claims and unusually long entries. Explain each issue and make the relevant field easy to find. |
| Export | Download editable Word or use the browser print dialog for a selectable-text A4 PDF. | Add direct PDF download if mobile users struggle with the print dialog. Build a parser fixture suite covering applicant names, roles, dates and section order. |

## Implementation and operations

- Keep the existing Careers URL, navigation and footer. Publish the CV workspace at /resume-builder and list it in the sitemap.
- Keep the CV in the browser. Saving a draft is opt-in. Clear Draft removes the saved CV from the browser. The job advert is session-only and excluded from exports. The preview and Word export use the same document model.
- Use a single-column document, standard headings and contact details in the body. Test real text extraction and familiar recruiting-system imports before making parser-specific claims. Follow the employer's stated file-format instructions.
- POST /api/resume-suggest receives only approved text. The endpoint validates explicit consent and same-origin requests, masks common contact patterns and sends only whitelisted fields to Cloudflare Workers AI, with a disclosed OpenRouter fallback restricted to zero-data-retention endpoints. The candidate can redact other details before sending. The API rejects hallucinated numbers and malformed responses. The applicant remains responsible for verifying all claims.
- RESUME_USAGE is an isolated D1 binding. Apply db/resume/0001_usage.sql to that database before enabling inference. Store only a keyed daily network digest and counts. Reserve both the global and per-network quota atomically. Limits are 12 requests per network bucket and 200 total per UTC day. Older buckets are cleaned after a successful reservation. Configure RESUME_QUOTA_SECRET or use the existing JWT_SECRET fallback. Cloudflare AI and quota errors leave the editor and exports available.
- For an inference-only rollback, set RESUME_AI_DISABLED=true and redeploy. For a full rollback, use the previous successful Cloudflare Pages deployment. Keep résumé text, job ads and identifiers out of metrics, logs and test artifacts.

## Pilot and release criteria

| Area | Test and target |
| --- | --- |
| Mobile usability | A student without employment history can export a usable first CV in 10 minutes. Test lower-end Android, iOS Safari, keyboard-only operation and 200% zoom. Test with volunteers from all three starting profiles. |
| Document quality | Compare extracted PDF and DOCX text to the preview. Cover Unicode names, long URLs and multi-page output. Open and edit the Word export in Microsoft Word and LibreOffice. |
| AI accuracy | Review at least 30 synthetic or anonymised cases across the target profiles. Require zero invented credentials, employers or metrics in the accepted benchmark outputs. Include misleading job requirements, sparse evidence and malformed requests. |
| Reliability | Verify live inference and simulate unavailable AI, exhausted quota, browser storage failure and aborted requests. Manual editing and export must work in each case. |
| Measurement | Start with aggregate editor opens, first completed exports, suggestion success, and accept/edit/dismiss rates. Ask participants where they hesitated. Prioritise by completion friction and frequency of unsupported claims. |

## Delivery order

1. Pilot the live editor. Fix mobile and export issues before adding new panels.
2. Ship evidence-linked job matching and better readiness checks. Show support for each requirement and provide actionable gaps. Avoid speculative ATS scores or hiring predictions.
3. Add local import, master CVs and application-specific copies after validating their demand and privacy design. Review retention, service agreements and applicable South African requirements before introducing cloud sync, account storage or recruiting integrations.

## References

- [Greenhouse guidance on résumé parsing and layout](https://support.greenhouse.io/hc/en-us/articles/200989175-Unsuccessful-resume-parse)
- [Cloudflare Workers AI data usage](https://developers.cloudflare.com/workers-ai/platform/data-usage/)
- [South Africa's Protection of Personal Information Act](https://www.justice.gov.za/legislation/acts/2013-004.pdf)
