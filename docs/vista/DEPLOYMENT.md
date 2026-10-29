# Vista Cinema deployment runbook

Last verified: **October 29, 2026**.

## Current status

| Component | Verified status |
|---|---|
| Public version before this release | **1.0.8**, versionCode **15** |
| Release package | **1.0.9**, versionCode **16**, mandatory update |
| Local build | Debug APK built successfully: `app/build/outputs/apk/debug/app-debug.apk` |
| Signed 1.0.9 release | Built; signature, version and certificate continuity with 1.0.8 verified |
| AI backend | Deployed; NVIDIA page actions, app knowledge, profile name and follow-up conversation passed live tests |
| Announcement | Coming-soon FCM message accepted for delivery to `vista_updates` |
| Kids version | Planned; no release date confirmed |

The live update manifest at `https://lxobsidianportal.co.za/vista-version.json` advertised 1.0.8 before this release. Root and staged manifests now describe 1.0.9 / 16 with `mandatory: true`. Verify the live endpoint after publication. The signed APK is 22,369,333 bytes, SHA-256 `041131BC0350B193B2FF8327CC95289FED0AAF73D10BB79A22C09BE09221DB05`.

## Confirmed GitHub and hosting destinations

- **Vista website and APK repository:** https://github.com/lx-obsidian-labs/lx-obsidian-portal
- **Default branch:** `master`
- **Public website:** https://lxobsidianportal.co.za
- **Download page:** https://lxobsidianportal.co.za/vista.html
- **Update endpoint used by Android:** https://lxobsidianportal.co.za/vista-version.json
- **Cloudflare Pages project:** `lxobsidianmainsite`, with `dist` as its build output directory, per the repository's `wrangler.toml`.
- **Separate AI Worker:** https://vista-assistant.feedpatelmusic.workers.dev

The website repository root contains `vista.html`, `vista-version.json`, existing APKs and `tv/`. The updater reads the manifest from the **site root**, not `/deploy/vista-version.json`. APK download URLs point to GitHub raw files.

The native Android workspace currently has the same repository configured as `origin`, but its local tree is the Android project. Prepare website changes in a checkout of the **current remote `master`**, preserving the existing site. The previously documented Desktop website folder no longer exists at that path. The separate `lx-obsidian-labs-website` repository points to a different Vercel site and is not the verified Vista download repository.

## 1. Build and verify the release APK

The app is configured as versionName `1.0.9`, versionCode `16`. One binary serves phones, tablets and Android TV / Google TV.

```powershell
.\gradlew.bat --no-daemon :app:assembleRelease --console=plain
```

Expected output: `app/build/outputs/apk/release/app-release.apk`. Signing uses the existing local `release-key.jks` and `keystore.properties`.

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs "app/build/outputs/apk/release/app-release.apk"
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt.exe" dump badging "app/build/outputs/apk/release/app-release.apk"
Get-FileHash "app/build/outputs/apk/release/app-release.apk" -Algorithm SHA256
```

Verify package `com.example.vista`, versionCode `16`, versionName `1.0.9`, both launchers and the same signing certificate as the installed production app. Record the actual release size and checksum after building. Release minification remains disabled; see `BUILD_REPORT.md`.

## 2. Publish APKs and update website files

In a current checkout of `lx-obsidian-labs/lx-obsidian-portal`:

1. Add the verified signed release as root `VistaCinema-v1.0.9.apk`. If the website's TV link uses a separate filename, place the same signed binary at `tv/VistaCinema-TV-v1.0.9.apk` and update that link.
2. Update root `vista.html` and root `vista-version.json` together. The local `deploy/` directory is staging material, not the website root.
3. Use this manifest structure with accurate release notes:

   ```json
   {
     "versionCode": 16,
     "versionName": "1.0.9",
     "downloadUrl": "https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/VistaCinema-v1.0.9.apk",
     "changelog": "NVIDIA-powered conversational assistant, profile-aware discovery, floating chat, hands-free voice turns, page navigation actions, and AI/voice settings.",
     "mandatory": true
   }
   ```

4. Inspect that checkout's current `package.json` and deployment workflow, build its `dist` artifact, and publish through its configured Cloudflare Pages workflow. The verified Pages configuration names `lxobsidianmainsite`; GitHub asset publication alone does not establish that the website deployment completed.
5. Push only the intended site files and release artifacts to the confirmed repository. Keep `.env`, credentials, signing files and debug APKs out of publication.

The publication commit contains only APKs, release docs, download-page changes, the update manifest and the build inclusion of docs. Native application source and signing credentials are not part of the website publication. APKs remain in GitHub; the website build excludes them from Cloudflare Pages.

## 3. Deploy and verify AI separately

See `backend/README.md` for the full contract and configuration. Root `.env` takes precedence over inherited environment variables so the saved project key is used. Deployment uploads the NVIDIA key as a Cloudflare **Worker secret**; Android receives responses, never the key.

```powershell
node backend/deploy.mjs --provider-check --model=nvidia/nemotron-3.5-lightning-30b-a3b
node backend/deploy.mjs
node backend/smoke.mjs --require-nvidia
```

The current model passed both an actual provider completion and authenticated Firebase → Cloudflare → NVIDIA checks. The smoke script verifies page actions, profile/app grounding and follow-up memory, and deletes its temporary Firebase identity. Workers AI provides fallback if NVIDIA fails.

## 4. FCM announcements

The existing `send_fcm_cli.mjs` uses Firebase CLI authentication for the `vista-cinema` project. A date-free coming-soon announcement was already accepted earlier. The release payload is now:

- **Title:** `Vista Cinema 1.0.9 — Update required`
- **Body:** `Your smarter NVIDIA-powered AI assistant is here. Open Vista, tap Install Update Now, then approve Install to get floating chat, hands-free voice and personalized help. A kids version is also in planning.`
- **Topic:** `vista_updates`
- **Data:** `release_status=live`, `check_update=true`, `mandatory=true`, `kids_version=planned`.

Send the release announcement only after verifying the public mandatory manifest and GitHub APK downloads. The past requested release date remains omitted. FCM prompts the user to open Vista; Android requires the user to approve installation. Do not rerun a broadcast as a deployment check.

```powershell
firebase login:list
# Validate only; does not broadcast:
node send_fcm_cli.mjs
# Broadcast only when another announcement is intended:
node send_fcm_cli.mjs --send
```

Before announcing a live release, update the script's wording and release-status/update-check fields to match the successfully published APK and manifest.

## 5. Verify public release and device behavior

- Fetch `https://lxobsidianportal.co.za/vista-version.json` and confirm the published version and APK URL.
- Download the APK from that exact URL and verify it matches the signed build's checksum.
- Test an upgrade from 1.0.8, retaining profile and watchlist data.
- Verify FAB chat preserves the current tab and the bottom AI tab opens full-screen chat.
- Test sign-in, profile personalization, title search, page actions and spoiler settings.
- On a physical device, verify microphone permission, voice quality, listening/speaking turn-taking, “stop listening”, and dismissal/background shutdown.

## Windows build recovery

The missing `shrunk-classpath-snapshot.bin` failure and missing generated classes were resolved by stopping stale compiler/Gradle processes, avoiding overlapping IDE/terminal builds, and using conservative project settings: one worker, no parallel builds, in-process Kotlin compilation, and disabled Kotlin/KSP incremental compilation.

With Android Studio's active build cancelled:

```powershell
.\gradlew.bat --stop
.\gradlew.bat --no-daemon --no-build-cache :app:clean :app:assembleDebug --console=plain
```

This clean build passed in **7m 38s**, with **45 tasks executed**. See `gradle.properties.example` for the settings; the local `gradle.properties` contains credentials and remains ignored.
