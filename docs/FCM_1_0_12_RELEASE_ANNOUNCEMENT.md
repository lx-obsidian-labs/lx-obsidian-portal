# Vista Watch 1.0.12 release announcement

Sent 17 November 2026 using `node send_fcm_cli.mjs --send`, after validation and publication of the changelog/guide.

- Audience: `vista_updates` topic, shared by mobile and Android TV installations.
- Title: **Vista Watch 1.0.12 is live — what’s new**.
- Announcement ID: `vista-1012-live-changelog`.
- FCM response: `accepted_for_delivery`.
- Message name: `projects/vista-cinema/messages/-367322524440597668`.
- Release: Android 1.0.12 / versionCode 19; optional update.
- Guide: https://www.lxobsidianportal.co.za/vista-1-0-12
- Website commit: `784aafe`.
- Deployment: https://580347b3.lxobsidianmainsite.pages.dev

The message covers playback/catalog recovery, TV navigation and sources, support reports, login recovery, avatars/artwork, installation instructions, the TV Menu/hold-OK shortcut, and the support QR. The live version manifest contains the expanded changelog and tips shown by the existing mobile/TV update dialogs.

FCM acceptance is not a per-device delivery receipt. This is a `promotions` announcement with `check_update=true`; existing release-notification preferences and device notification permissions apply. The app stores accepted announcements in its in-app bell history. Users already on 1.0.12 can read the public guide; their updater does not display a same-version update dialog.

Do not rerun the broadcast or the obsolete 1.0.12 coming-soon campaign as a test. Running `node send_fcm_cli.mjs` without `--send` validates only.
