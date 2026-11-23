# Vista Cinema 1.0.11 publication — 15 November 2026

## Live destinations

- Product and Android/TV downloads: https://www.lxobsidianportal.co.za/vista
- Download hub: https://www.lxobsidianportal.co.za/downloads
- Native iOS device build information: https://www.lxobsidianportal.co.za/vista-ios
- Android update manifest: https://www.lxobsidianportal.co.za/vista-version.json
- GitHub release: https://github.com/lx-obsidian-labs/lx-obsidian-portal/releases/tag/v1.0.11

Android **1.0.11 / 18** is published as an optional update (`mandatory: false`). iOS device artifacts are published as **unsigned developer downloads**, not ordinary-iPhone installation links. Direct website distribution does not remove Apple's signing/provisioning requirement.

## Publication and build evidence

| Item | Evidence |
|---|---|
| Website source | Current remote master preserved; commit `09ab12321c4fe6939498c5a94183060dfc743bc7` |
| Website checks | 32 API tests, documentation integrity and site build/local-link/navigation/download checks passed |
| Website CI | https://github.com/lx-obsidian-labs/lx-obsidian-portal/actions/runs/43691740313 — success |
| Cloudflare Pages production | `lxobsidianmainsite`, branch master, https://2b2704f4.lxobsidianmainsite.pages.dev |
| Android release | `app/build/outputs/apk/release/app-release.apk`; release vital lint and 36 focused JVM tests passed; two Node reporting tests passed |
| iOS source | `ios/native-preview-1.0.11`, commit `f7bbbec`; actual device archive workflow added |
| iOS macOS build | https://github.com/lx-obsidian-labs/lx-obsidian-portal/actions/runs/43689490832 — simulator compilation, five XCTest tests and iPhone device archive passed |

## Public artifact verification

Each file was downloaded through the actual production `/api/vista-download` endpoint, served from the allowlisted GitHub release CDN (no installer bytes uploaded to/proxied through Pages), and hashed from the received bytes.

| Platform | Filename | Bytes | SHA-256 |
|---|---|---:|---|
| Android | VistaCinema-v1.0.11.apk | 22,583,266 | `1590e86fc074ae039a2154ada6bdac3dfc0a4b6e75da69a51bad325dfeded1c0` |
| Android TV | VistaCinema-TV-v1.0.11.apk | 22,583,266 | `1590e86fc074ae039a2154ada6bdac3dfc0a4b6e75da69a51bad325dfeded1c0` |
| iOS device | Vista-iOS-Unsigned.ipa | 218,348 | `0fc33859e0779c775965418d47890a9d63bb42037ceb8933cf76b51ed42a1422` |
| iOS archive | Vista-iOS-Device-Archive.zip | 971,901 | `fe7fd21900368d42a60c5d9b1dd943d3f34ac2946a373eae8fd990a4a9ed5a6e` |

Android production certificate SHA-256 remains `a4fc4156be6fc87480c04a7defc5e811ba4006df4ed180345c429cd4d0a85318`. Phone and TV binaries are identical with different launchers.

The IPA's Mach-O executable was inspected as **arm64**, `CFBundleSupportedPlatforms = [iPhoneOS]`, bundle `com.lxobsidian.vistacinema`, version `1.0.11`, build `18`, with no signing resources. This is an actual device binary, not a renamed simulator app. Installation still requires an Apple Developer signing identity and matching provisioning profile, or TestFlight/App Store distribution. No Apple signing credentials were available; the user requested website-style distribution and was informed of this distinction.

## Site changes

- Latest version, hash, size, notes and Android/TV release assets updated together.
- iOS information/download page added and linked from product page/download hub/sitemap; explicitly unsigned and with actual implemented feature scope.
- Versioned redirect cache keys prevent a prior release's cached redirect serving the new manifest's download request.
- Poll data handling and anonymous-quote permission explained in the privacy policy.
- Existing homepage, shared navigation/footer, other products, DB binding and Cloudflare project retained. An outdated verifier expected retired homepage anchors; it now checks the actual Start here/Study tools anchors.
- Current APKs/IPA/archive live in GitHub Release assets. The Pages output excludes installers and signing material; the build guard now also rejects `.ipa`, `.p12` and `.mobileprovision` files.

## Outstanding device/signing checks

No ADB device was attached. Physical Android/TV behavior, signed-in regular-user Firestore poll writes, live provider/key availability, physical iPhone playback and Apple signing/distribution remain unverified. iOS accounts/polls, AI/voice, downloads/APNs and history/resume have not been ported. Live TV and Sports remain planned with availability/timing unconfirmed. These limitations are stated on the site and in the release notes.

Coming-soon and roadmap FCM messages were already accepted earlier; publication verification did not repeat those broadcasts.
