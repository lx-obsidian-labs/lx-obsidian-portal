# Production lead delivery verification

The contact API uses D1 for persistence and an optional webhook for notification. Mocked unit tests alone cannot establish live delivery.

1. Confirm the deployed Cloudflare Pages `DB` binding and schema.
2. Submit a controlled test enquiry; require HTTP 201 and `saved: true` with a reference.
3. Verify the corresponding D1 row through authorized tooling; keep lead details private.
4. If enabled, verify authenticated webhook delivery and recorded notification status; failed notifications must not erase saved leads.
5. Verify rejection of untrusted origins, malformed payloads and unavailable storage.
6. Document owner approval, retention and rollback.

Status: production D1 persistence and notification delivery are not yet independently verified.
