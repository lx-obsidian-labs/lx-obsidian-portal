# Vista Cinema 1.0.10

**7 November 2026 · versionCode 17 · optional update**

## What's new

- **Smart TV responsiveness & Android device detection:** the included device detector classifies phones, tablets, Android TV / Google TV, Fire TV and supported remote-driven TV boxes using system features, TV UI mode and touchscreen/D-pad signals. TV focus feedback and D-pad row/grid scrolling improve large-screen navigation. These TV improvements are included alongside the new phone features.
- Festive Home collections from November 1 to January 5, a Christmas countdown, rotating picks and a seasonal visibility preference.
- Movie suggestions with visible reasons, meaningful-viewing and watchlist signals, explicit genre/negative feedback, runtime/year constraints and more varied picks.
- Catalog-backed AI actions: show your watchlist, offer actual resume progress, compare exact movie metadata and prepare a download review. Numbered follow-ups refer to the current results. Save/remove operations need confirmation; opening a download review does not mean a download has started.
- Downloads from supported direct HLS, DASH and progressive sources, a persistent unmetered queue, pause/resume, fresh-source retry, delete confirmation and a separate cache-only player for completed items. Embedded/DRM/expiring sources may not support downloading.
- Searchable Settings with AI & Voice and Downloads control centres, storage summaries, native language/caption preferences and system permission links.
- Accurate onboarding retained: first-time guest/sign-in/profile setup goes through the introduction, names are optional, completion is saved before leaving, and About offers a replay. Local library/onboarding data is preserved by an in-place signed upgrade.
- Provider fake-robot ad filtering and bounded network/source recovery. Vista's own AdMob remains in place.

The new festive, offline, advanced Settings and agentic chat screens are phone/tablet flows. The same APK includes the Android TV launcher, but interface and feature availability differ. No cross-device library sync, season-batch downloads, offline DRM license support or active paid subscription is promised by this release.

## Downloads and verification

- [Android phone/tablet APK](https://lxobsidianportal.co.za/api/vista-download?platform=android)
- [Android TV APK](https://lxobsidianportal.co.za/api/vista-download?platform=tv)
- [GitHub Release and direct asset alternatives](https://github.com/lx-obsidian-labs/lx-obsidian-portal/releases/tag/v1.0.10)
- Package: `com.example.vista`.
- Version: `1.0.10` / `17`.
- Android minimum/target: `24` / `35` (Android 7.0+).
- Exact size: **22,501,177 bytes**, approximately **21.5 MiB**.
- SHA-256: `31BADF75DFBA417D3D3871BB76084311AE293A06C468C5399BCB1EDC88DA5B1A`.
- Signing-certificate SHA-256: `a4fc4156be6fc87480c04a7defc5e811ba4006df4ed180345c429cd4d0a85318`, matching the documented 1.0.9 production certificate.
- Phone and TV filenames contain the same verified signed binary.

**APKs are hosted in GitHub.** Cloudflare Pages serves the website, release notes, generated installation guide and update manifest; its build excludes installers and signing files.

The first-party download links resolve public GitHub Release assets to their CDN URLs and return a redirect; they do not buffer, proxy or store the APK on Cloudflare. Both GitHub asset SHA-256 digests match the release hash above. This route avoids the GitHub web/raw download hop that returned gateway timeouts on a tested network. The direct GitHub Release remains a fallback.

## Verification scope

Release build/lint and signing/package/version verification passed. JVM regressions and Android test APK compilation passed. Twelve backend tests and three actual NVIDIA action-contract probes passed. These checks do not establish universal provider/network/TV compatibility. Final physical-device offline-playback, onboarding UI and production signed-upgrade matrix coverage remains pending following device disconnection; source-specific download availability is not guaranteed.

## Installation and onboarding

Install over the existing production app using Android's installer; do not uninstall just to update. A matching signature preserves local data. New users choose sign-in or the three-day guest trial, then receive the introduction. Existing users retain completion and can replay the tour in Settings → About. Cloud AI requires sign-in; complete eligible offline copies open from Library → Downloads.

[Full changelog](https://lxobsidianportal.co.za/docs/vista/CHANGELOG.md) · [Build verification](https://lxobsidianportal.co.za/docs/vista/BUILD_REPORT.md) · [Downloads centre](https://lxobsidianportal.co.za/downloads) · [Privacy](https://lxobsidianportal.co.za/privacy-policy.html) · [Terms](https://lxobsidianportal.co.za/vista-terms.html)
