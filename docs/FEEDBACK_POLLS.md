# Current-user feedback and marketing polls

## User experience

Version 1.0.11 adds an optional **Help shape Vista** card to Home and Settings. Home invitations can be dismissed; Settings remains available. No automatic modal interrupts playback.

The four single-choice questions ask:

1. How are you feeling about Vista? Includes positive, mixed, frustrated and new-user answers.
2. What's getting in your way most? Includes playback, embedded ads, missing catalog, navigation, downloads and no issue.
3. What should we improve next? Reliability, discovery, offline, TV, assistant or design.
4. How did you hear about Vista? Referral, WhatsApp, social, search, website, other or prefer not to say.

An optional 500-character comment follows. Anonymous marketing quote permission is separate, unchecked by default, and is not consent to email/push marketing. Guests can answer but must sign in to submit.

## Storage and reporting

Responses use the existing Firebase project and the signed-in viewer's document:

`viewers/{uid}.feedbackPolls.experience_1_0_11`

Saved fields: choice IDs, optional comment, explicit quote permission, app version and server timestamp. A transaction with a stable campaign field records one response in normal app use and makes retry delivery idempotent. Profile updates now merge rather than overwrite this field. Submission shows success only after the Firebase transaction completes; failed/unconfirmed submissions retain answers while the poll remains open.

Firestore's deployed rules remain the access-control authority. Confirm owner-only reads/writes and support for the new `feedbackPolls` field using a signed-in device before launch; no Firestore rules were exported or changed here. Client validation and transaction deduplication are not a substitute for server rules against modified clients. Admin OAuth reporting succeeds, but does not prove normal-user write authorization.

Read aggregated sentiment, friction, priorities and marketing discovery channels:

```powershell
node poll-report.mjs
```

The report uses the existing Firebase CLI owner/admin login, selects only the feedback field from viewer documents, paginates results and emits no user IDs, names or emails. To include comments with explicit anonymous-quote permission:

```powershell
node poll-report.mjs --marketing-quotes
```

Review opted-in comments before publication; a user may still have typed identifying information. The default report shows comment counts only. Percentages/charts can be calculated from the returned question counts; "prefer not to say" remains its own discovery category.

The live read-only report was executed successfully and returned **zero responses** before rollout. No fabricated votes or production test responses were inserted.

## Reaching current installations

Existing installations need the 1.0.11 update to render the new native survey. Android 1.0.11 is now published; see `PUBLIC_RELEASE_1_0_11.md`. No poll-specific broadcast has been sent. Suggested invitation copy:

> Help shape Vista. How is the app really feeling—great, mixed or frustrating? Update Vista, then open Settings → Help shape Vista for four quick, optional questions. Tell us what to fix next.

The current TV UI is separate from the phone/tablet Home and Settings flows; the native survey entry is phone/tablet-only in this build. The TV option in the survey can still capture feedback from users who also use Vista on a TV.
