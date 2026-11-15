# Vista Cinema for iOS

Native **SwiftUI**, iPhone/iPad, **iOS 17+**. This first iOS preview shares TMDB identifiers and embed providers with Android. Version **1.0.11**, build **18**. Android's implementation remains separate.

## Implemented

- Dark, red-accented Home with featured title and independently failing catalog rows.
- Debounced movie/series search, cancellation/latest-query protection, explicit paging/retry.
- Movie and series details, season/episode selection and correct movie/TV ID namespaces.
- WKWebView provider selection, movie-only NexStream backup, early all-frame targeted overlay cleanup, network ad rules and popup/navigation restrictions.
- Local, typed movie/series watchlist with atomic file persistence and visible storage failures.
- Native tab navigation, adaptive grids, iPad support, Dynamic Type and accessibility labels.
- Five XCTest cases for namespace handling, URL encoding/navigation, search decoding and persisted watchlists.

Not yet ported: Firebase account/onboarding flows, account-backed polls, AI/voice companion, offline downloads, push notifications/APNs, watch history/resume and TV-specific screens. Live TV/Sports are roadmap items, not functional playback tabs. The app is a native preview, not full Android feature parity.

## Build locally on macOS

Requires Xcode 16+ and Node 22+:

```sh
brew install xcodegen
node ios/scripts/configure.mjs
xcodegen generate --spec ios/project.yml
open ios/Vista.xcodeproj
```

`configure.mjs` resolves `TMDB_ACCESS_TOKEN` and optional `BACKUP_MOVIES_API_KEY` from environment, root `.env`, then root `gradle.properties`. It writes an ignored `ios/Config/Secrets.xcconfig` and generates the app icon. Key values are never printed. A build without keys succeeds but shows a configuration error when loading the catalog.

Do not commit generated secret configuration, Xcode build output or credentials. Client catalog/embed keys are bundled into the compiled app, like the Android build; administrative/provider-server secrets do not belong here.

## GitHub macOS build

`.github/workflows/vista-ios.yml` generates the project, selects an available iPhone simulator, builds and runs XCTest on `macos-15`, and uploads:

- `Vista-iOS-Simulator`: zipped `Vista.app` for a **macOS iOS Simulator**, matching the runner's architecture (not an installable iPhone IPA).
- `Vista-iOS-Test-Results`: Xcode result bundle, including failed-run diagnostics when available.
- `Vista-iOS-Device-Unsigned`: actual arm64 iPhone device archive and an **unsigned** IPA. These are developer artifacts that require Apple signing/provisioning before installation; they are not simulator binaries and are not a direct-install distribution.

Optional repository Actions secrets: `TMDB_ACCESS_TOKEN`, `BACKUP_MOVIES_API_KEY`. Simulator builds don't need Apple signing. Trigger from Actions → Vista iOS → Run workflow, or push an iOS change. Pull requests from forks receive no repository secrets.

Simulator install on a Mac after extracting the artifact:

```sh
xcrun simctl install booted Vista.app
xcrun simctl launch booted com.lxobsidian.vistacinema
```

## Installable iPhone/TestFlight build

Requires Apple Developer team access, a registered bundle identifier matching `com.lxobsidian.vistacinema`, distribution signing/provisioning and App Store Connect configuration. In Xcode choose the team under Signing & Capabilities, select a generic iOS device, then Product → Archive → Distribute App. A simulator artifact cannot be renamed into an IPA or installed on an iPhone.

Physical-device validation is still needed for WKWebView playback, AirPlay/fullscreen behavior, media-provider cookies/redirects, offline handling and iPad rotation. No claim of App Store publication or successful live playback is made by creating the project or passing unit tests.
