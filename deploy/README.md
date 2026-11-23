# Vista Cinema - Deployment Package

Local staging folder for the public download page, the in-app update manifest, and the signed
release APKs.

## Current release status — October 29, 2026

- **Release package:** 1.0.9 / versionCode 16, signed at `app/build/outputs/apk/release/app-release.apk`. Both manifests set `mandatory: true`; verify `https://lxobsidianportal.co.za/vista-version.json` after deployment.
- **APK size / SHA-256:** 22,369,333 bytes / `041131BC0350B193B2FF8327CC95289FED0AAF73D10BB79A22C09BE09221DB05`. Release certificate matches 1.0.8.
- **GitHub website/APK repo:** https://github.com/lx-obsidian-labs/lx-obsidian-portal, branch `master`.
- Publish the signed release APK and update **root** `vista.html` and `vista-version.json` in the current website checkout, then deploy its `dist` output to Cloudflare Pages project `lxobsidianmainsite`.
- The coming-soon/kids-version-planned announcement was already accepted by FCM. No release date is confirmed.

Use the current `DEPLOYMENT.md` runbook. The 1.0.6 package details below are historical reference, not the current publication status.

> **Where the pieces actually live in production**
>
> * The **APKs** are hosted from GitHub at `lx-obsidian-labs/lx-obsidian-portal` and served over
>   raw.githubusercontent URLs. That is the only place the in-app updater and every download page
>   point to. They are *not* uploaded to Cloudflare Pages.
> * The **site** (`vista.html`, root `vista-version.json`) is in that same GitHub repository and
>   deployed to Cloudflare Pages as its `dist/` artifact. The previously documented Desktop
>   checkout path no longer exists; use a current checkout of remote `master`.
> * This folder is a **staging package only** so releases can be prepared and checksummed before
>   anything is pushed. `index.html` is a complete standalone page that happens to link straight to
>   the GitHub URLs, so nothing here has to be uploaded to host the downloads.
>
> Do **not** re-upload this folder as the Cloudflare site root — that approach is what caused the
> v1.0.6 APK to 404 while the site was still serving 1.0.5. The full runbook is in
> `DEPLOYMENT.md` (repo root).

## Contents

```
deploy/
  index.html                    - Standalone download landing page (links to GitHub raw APKs)
  privacy-policy.html           - Privacy policy page
  vista-version.json            - In-app update manifest (served to the app via the site)
  VistaCinema-v1.0.6.apk        - Signed release APK (phone + tablet; v1.0.6, versionCode 13)
  tv/
    VistaCinema-TV-v1.0.6.apk   - Same signed APK (TV edition downloads point here)
    README.md
  screenshots/                  - App screenshots for marketing (SVG)
  assets/                       - Additional assets
  archive/                      - Superseded APK builds (kept for reference, not linked)
  README.md                     - This file
```

> **The referral page lives outside this folder, in `../referral-site/`, on purpose.**
> It advertises "5 friends = 12 months free", which the current build cannot track or honour —
> publishing it now collects signups against a promise the product cannot keep. Keeping it out of
> `deploy/` means the whole folder can be shipped at any time without a stray link publishing it.
> Move it into `deploy/vista/` only once the Phase 1 referral backend is live, then re-add the
> "Invite friends" link to `index.html`.

## Quick Start

The downloads are live as soon as the APK exists in the GitHub repo:

1. Push `VistaCinema-v1.0.6.apk` to the repo root of
   `lx-obsidian-labs/lx-obsidian-portal`, and `VistaCinema-TV-v1.0.6.apk` to its `tv/` folder.
2. Update `vista-version.json` and `vista.html` in the site repo (root **and** `dist/`), then
   deploy `dist/` to Cloudflare Pages.
3. `index.html` here can be hosted anywhere (or alongside the Cloudflare site) — its buttons already
   point at the GitHub raw URLs above.

## APK Details

| Field | Value |
|-------|-------|
| Version | 1.0.6 |
| Version Code | 13 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |
| Size | ~21.0 MB (22,024,645 bytes) |
| Signing | Release keystore (release-key.jks, CN=Vista Cinema), APK Signature Scheme v2 |
| Editions | One signed APK registers both a phone and a TV launcher |

## SHA-256 Checksum

```
0C03D7EF829A94E053FB237B632C86279C6FFD0522AA80EE586D3A1A815D7523
```

Verify integrity with:
```bash
# Windows (PowerShell)
Get-FileHash VistaCinema-v1.0.6.apk -Algorithm SHA256

# Linux / macOS
shasum -a 256 VistaCinema-v1.0.6.apk
```

The phone/tablet and TV files are the same signed APK, so the checksum is identical.

## Release Notes - 1.0.6

Shipped to installed apps via `vista-version.json` (mandatory: true):

- New: **Movie of the Day** — a free film picked for you every day, matched to the season
  (Heritage Day, Freedom Day, Easter, Christmas and more). The pick skips anything you have
  already watched, the reminder respects the notification master switch, and tapping it lands on
  the Home card.
- New: share straight to WhatsApp, with a one-tap path and a fallback to the native share sheet
  (Facebook, X, TikTok, anything else) when WhatsApp is not installed.
- New: announcements can be targeted by country. Installs subscribe to `vista_updates_za` or
  `vista_updates_row` in addition to the global `vista_updates` topic.
- New: private, no-PII usage analytics (opens, playback, shares, Movie of the Day) collected on
  Vista's own site — nothing is sent to a third party.
- Fixed: sharing a movie or show sent people to `shoofaflam.tv` instead of Vista Cinema. The share
  link now points at the real Vista Cinema site.
- Improved: the TV build now scrolls properly with a remote — rows snap, the page follows focus,
  and posters stay in frame.

> Marked mandatory so the whole base moves onto the country topics and the fixed share links — that
> is what makes a South-Africa-only announcement possible. Delta from 1.0.5 is small.

## Release Notes - 1.0.5

Shipped to installed apps via `vista-version.json` (mandatory: true):

- New: in-app notification bell with announcements from the Vista team.
- New: on-device release notifications via Firebase Cloud Messaging (topic `vista_updates`).
- Redesigned: Settings - professional layout, real storage/cache sizes, honest toggles,
  subtitle size/colour controls.
- New: cinematic first-run onboarding (brand tour + name) and a welcome-back screen for
  returning viewers.
- New: Premium announcement with a live countdown to the early-2027 launch (R20/month).
- New: 3-day free guest trial for new viewers.
- New: voice-first search on the TV build.
- Fixed: TV mandatory updates can no longer be dismissed with the BACK key.

> **1.0.5 is the push-capable base build.** Push notifications only reach this version and
> newer, so this release is marked mandatory. Full FCM send instructions are in `DEPLOYMENT.md`.

The **in-app updater** offers this build to anyone running an older versionCode as soon as
`vista-version.json` points at a real APK. Install the manifest + APK together.

## Screenshots

Marketing screenshots are in the `screenshots/` folder (SVG format):

| Filename | Description |
|----------|-------------|
| `home.svg` | Home screen with continue watching + trending |
| `discover.svg` | Discover/browse with categories and genres |
| `details.svg` | Movie details with cast and where to watch |
| `player.svg` | Video player / playback screen |
| `search.svg` | Search screen with results |
| `profile.svg` | Profile and settings |

> **Tip:** to convert SVGs to PNG for social media, open them in a browser and screenshot, or
> use a tool like Figma/Canva.

## Releasing a new version

The full step-by-step runbook lives in **`DEPLOYMENT.md`** (repo root). Summary:

1. Bump `versionCode`/`versionName` in `app/build.gradle.kts`.
2. `.\gradlew.bat --no-daemon :app:assembleRelease` -> `app/build/outputs/apk/release/app-release.apk`
3. Verify signing + version (apksigner / aapt), compute SHA-256.
4. Copy the APK into this folder as `VistaCinema-v{version}.apk` and `tv/VistaCinema-TV-v{version}.apk`; move old builds into `archive/`.
5. Update `vista-version.json`, `index.html`, this README.
6. Push the APKs to the GitHub repo `lx-obsidian-labs/lx-obsidian-portal` (root + `tv/`).
7. Update `vista-version.json` and `vista.html` in the site repo, build `dist/`, and deploy it to
   Cloudflare Pages (full runbook in `DEPLOYMENT.md`).

## Keystore

**IMPORTANT:** Keep `release-key.jks` and `keystore.properties` (repo root) secure and backed
up. Losing the keystore means you cannot update the app for existing users.

## Notes

- Android shows a security warning when installing APKs from outside the Play Store.
- Users need to enable "Install from Unknown Sources" in their browser/device settings.
- HTTPS is required for the download to work on modern Android browsers.
