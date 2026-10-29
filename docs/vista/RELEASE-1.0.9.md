# Vista Cinema 1.0.9

Release package prepared October 29, 2026. VersionCode **16**. Required update for earlier versions.

## The headline: real NVIDIA-powered conversations

Vista's assistant now generates conversational replies through NVIDIA NIM instead of returning repeated command templates. It can follow up on the conversation, use your profile name and enabled taste preferences, explain Vista's features, and offer actions to open supported pages or Search. Cloud AI requires sign-in. The provider key remains in a Cloudflare Worker secret and is never included in the APK.

Verified model: **`nvidia/nemotron-3.5-lightning-30b-a3b`**. Authenticated live checks passed for page routing, app knowledge, profile name and follow-up memory with NVIDIA explicitly required. Cloudflare Workers AI remains a fallback.

## More in this update

- Floating chat from the Home FAB preserves your tab; the bottom AI tab opens full-screen chat.
- Foreground hands-free voice turns: tap the microphone once, speak, hear the reply, and continue. Say “stop listening” or tap Stop to finish.
- Highest-quality available Android voice selection and a shortcut to Android voice/language settings. Realism depends on the installed speech engine.
- AI/chat controls, profile assistant access, and playback recovery improvements.
- A dedicated kids version is **planned**, with no confirmed release date.

## Install the required update

Open Vista Cinema, tap **Install Update Now**, and approve Android's installer. The manifest sets `mandatory: true` for versions below versionCode 16. Push notifications prompt opening the app; they cannot silently install an APK or force Android to launch the app.

Both files below are the same signed APK, supporting phone/tablet and Android TV / Google TV:

- [Android APK](https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/VistaCinema-v1.0.9.apk)
- [Android TV APK](https://github.com/lx-obsidian-labs/lx-obsidian-portal/raw/master/tv/VistaCinema-TV-v1.0.9.apk)

**APK storage is GitHub only.** Cloudflare Pages serves the website, release docs and update manifest; the site build does not include APK files.

## Verified build details

- Package: `com.example.vista`
- VersionName / versionCode: `1.0.9` / `16`
- Minimum / target Android SDK: `24` / `35`
- Size: **22,369,333 bytes** (about 21.3 MiB)
- SHA-256: `041131BC0350B193B2FF8327CC95289FED0AAF73D10BB79A22C09BE09221DB05`
- Signing certificate SHA-256: `a4fc4156be6fc87480c04a7defc5e811ba4006df4ed180345c429cd4d0a85318`
- Certificate matches the published 1.0.8 APK.
- Release build and release lint checks passed. Nine backend tests passed. Physical-device voice and UI checks remain separate from build verification.

See the accompanying [changelog](CHANGELOG.md), [build report](BUILD_REPORT.md), [deployment runbook](DEPLOYMENT.md), and [AI backend notes](backend/README.md).
