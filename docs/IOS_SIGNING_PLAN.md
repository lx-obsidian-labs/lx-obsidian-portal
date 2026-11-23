# Vista iPhone installation: signing and distribution plan

## Outcome

Make Vista installable on ordinary iPhones through a public TestFlight invitation linked from the existing website. Follow with an App Store release after beta testing. GitHub macOS runners remain the build system; Cloudflare hosts the product page and installation link.

The existing native device build compiles and the five iOS tests pass. The outstanding prerequisite is Apple account/signing/distribution access, not another simulator build. Signing establishes an authorized application identity; TestFlight is the distribution channel and still uses Apple's signing workflow.

## Recommended delivery path

`iOS source → GitHub macOS build/test → signed device archive → App Store Connect → TestFlight → website invitation link`

The user flow becomes: open the Vista iOS webpage, tap Join the iPhone beta, install/open Apple's TestFlight app and accept the invitation. No tester UDID collection is required for TestFlight. An App Store distribution IPA hosted as a raw website download is not an ordinary-iPhone installation route.

## Phase 1 — Apple account and app identity

**Account-holder work:**

- Confirm an active Apple Developer Program membership, two-factor authentication and accepted agreements. Apple lists USD 99 per membership year, with regional pricing variations and eligible fee waivers.
- Choose individual enrollment or organization enrollment. Organization enrollment requires an eligible legal entity and typically a D-U-N-S number; the seller name follows enrollment type.
- Record the Apple Team ID.
- Register explicit bundle ID `com.lxobsidian.vistacinema`, provided it is available to the team. If another identifier is necessary, update the Xcode project, profile and App Store Connect record together.
- Create the Vista Cinema iOS app record in App Store Connect for the same bundle ID.

**Completion evidence:** active team, registered App ID and an App Store Connect app record accessible to the release operator.

## Phase 2 — Signing and CI credentials

Use explicit signing for the first reproducible CI release:

1. Create an Apple Distribution certificate and retain its matching private key. An exported `.p12` must include the private key; a downloaded `.cer` alone is insufficient.
2. Create an App Store Connect distribution provisioning profile for the same App ID, team and certificate. Apple also supports automatic profile management; that can be adopted after the first signing flow is verified.
3. Create an App Store Connect API key with the access required by the selected upload workflow. Record key ID and issuer ID. API upload credentials do not replace the code-signing identity.
4. Use a controlled macOS setup or signing bootstrap to create/export the signing identity. Routine builds then run on GitHub macOS; a permanently owned local Mac is not required.
5. Store the following in GitHub Actions secrets or a protected release environment. Do not paste key contents into chat or commit them.

| Proposed secret | Contents |
|---|---|
| `APPLE_TEAM_ID` | Developer Team ID |
| `IOS_DISTRIBUTION_P12_BASE64` | Base64-encoded certificate plus private key |
| `IOS_DISTRIBUTION_P12_PASSWORD` | Password protecting that export |
| `IOS_APPSTORE_PROFILE_BASE64` | Matching distribution provisioning profile |
| `ASC_KEY_ID` | App Store Connect API key ID |
| `ASC_ISSUER_ID` | App Store Connect API issuer ID |
| `ASC_PRIVATE_KEY_P8_BASE64` | Base64-encoded private API key |

Base64 is transport encoding, not encryption. These values remain secrets. Existing `TMDB_ACCESS_TOKEN` and `BACKUP_MOVIES_API_KEY` stay separate from signing credentials.

**Completion evidence:** CI can identify the distribution signing identity and validate the profile's team, application identifier, certificate and expiration before archiving.

## Phase 3 — Signed macOS build pipeline

Extend `.github/workflows/vista-ios.yml` with a separate signed release job:

- Keep the current simulator tests as the fast verification stage.
- Run signing/upload only on a deliberate release/manual trigger, not on pull requests.
- Create an ephemeral keychain and import the signing identity; install the profile for the duration of the job.
- Generate the Xcode project and set the confirmed team, signing identity and matching profile.
- Build a fresh generic iPhoneOS Release archive with signing enabled instead of `CODE_SIGNING_ALLOWED=NO`.
- Export for App Store Connect using export options supported by the runner's installed Xcode version.
- Verify signature, entitlements, bundle ID, version/build and provisioning identity. Use a new build number, such as 19, for the first signed upload after the current unsigned build 18.
- Upload through a current supported Xcode/Apple transport or pinned maintained tooling, using the API key.
- Await App Store Connect processing and report the actual build/processing status. A successful upload alone is not a TestFlight invitation.
- Remove temporary profiles, API key files and the ephemeral keychain when the job finishes.

**Completion evidence:** a signed device build appears in the correct App Store Connect app with successful processing and the expected version/build.

## Phase 4 — TestFlight installation

1. Configure beta description, feedback email, contact/review information and accurate testing instructions; resolve required export-compliance questions.
2. Add a small internal tester group and install the processed build on a real iPhone.
3. Exercise launch, catalog/search, saved titles, movie and series episodes, provider selection, app background/foreground behavior and connection failures. Include an iPad if iPad support is advertised.
4. Create an external tester group and submit the first external build for TestFlight review. External distribution is subject to Apple approval.
5. Once approved, enable the public TestFlight invitation link. Apple currently permits up to 10,000 external testers; start with a small beta capacity that can be supported.

**Completion evidence:** an external tester installs and launches Vista from the invitation on an ordinary iPhone, without developer-mode sideloading or collecting the phone's UDID.

## Phase 5 — Website installation rollout

- Update `vista-ios.html` on current website `master`, preserving the existing production site.
- Make the verified public TestFlight URL the primary Join the iPhone beta action. Leave developer archives as secondary technical resources, clearly labelled.
- Update `downloads.html`, Vista's product page and release notes to match the actual availability and iOS feature scope.
- Publish only the real invitation URL; do not invent a TestFlight URL or relabel an unsigned IPA as installable.
- Verify the link on iPhone Safari through TestFlight installation, then announce the beta to current users.

**Completion evidence:** the public website takes a new tester through successful iPhone installation, and site/API/build checks pass.

## Phase 6 — Public App Store release

After beta feedback, complete the App Store listing, screenshots, support/privacy details, age rating and required declarations. Resolve any App Review feedback, then link the approved App Store listing from the site. Describe the native iOS feature set accurately; Android-only account/poll/AI/download functionality is not yet included in this preview.

## Website-hosted testing option

If a small named group needs device installs before the external TestFlight beta is approved, an ad-hoc build can use a separate matching profile containing those registered device UDIDs. That route is device-restricted and must not be presented as an unrestricted public download. It does not remove the Apple Developer account/signing requirement.

## First blocker and ownership

The first required input is confirmation of an active Apple Developer membership and its Team ID. Enrollment, payment, identity checks and Apple approval are controlled by the account holder/Apple. Engineering can then implement and verify signing, upload and website integration. No guaranteed Apple enrollment/review turnaround is assumed.

## Official references checked 15 November 2026

- Enrollment, pricing and organization requirements: https://developer.apple.com/programs/enroll/
- TestFlight groups, public invitations and external review: https://developer.apple.com/testflight/
- App Store Connect provisioning and automatic profile management: https://developer.apple.com/help/account/provisioning-profiles/create-an-app-store-provisioning-profile/
