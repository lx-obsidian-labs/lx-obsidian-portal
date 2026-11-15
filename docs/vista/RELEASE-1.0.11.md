# Vista Cinema 1.0.11

Android version **1.0.11**, code **18**, optional update. Published 15 November 2026.

## Changes

- Actionable catalog failures and retry instead of empty successful results; independent TVMaze fallback.
- Earlier targeted embedded-provider ad cleanup, including nested frames on supported WebViews.
- Foundation-driven TV focus scrolling, focus restoration and padding for enlarged cards.
- Polished recovery screens, adaptive collection grids and explicit failed-page retry.
- Optional Help shape Vista poll in phone/tablet Home and Settings: sentiment, frustrations, improvement priority, discovery channel, optional comment and separate anonymous marketing quote permission.
- Optional NexStream backup movie embed after existing playback sources. Metadata and video are separate services; provider availability is not guaranteed.

## Android verification

- Signed APK size: **22,583,266 bytes**.
- SHA-256: `1590E86FC074AE039A2154ADA6BDAC3DFC0A4B6E75DA69A51BAD325DFEDED1C0`.
- Production certificate SHA-256: `a4fc4156be6fc87480c04a7defc5e811ba4006df4ed180345c429cd4d0a85318`.
- Release vital lint, 36 focused JVM tests and two poll-report tests passed. Physical-device TV, normal-user poll submission and live-provider checks remain pending.
- Phone/tablet and TV use the same signed APK with different launchers; phone/tablet features do not imply TV parity.

## iOS device developer build

- Native SwiftUI app for iPhone/iPad, iOS 17+, version 1.0.11/build 18.
- Actual **arm64 iPhoneOS** binary verified in the IPA; not the simulator application. macOS compilation, five XCTest cases and device archive generation passed.
- The IPA is **unsigned**. Apple signing/provisioning or TestFlight is required before installation. Do not describe it as an available ordinary-iPhone direct installation.
- Implemented: Home, search, details, episodes, embedded provider selection, backup movie source and local watchlist.
- Not yet ported: accounts/onboarding, account-backed polls, AI/voice, downloads, APNs and history/resume.
- IPA SHA-256: `0fc33859e0779c775965418d47890a9d63bb42037ceb8933cf76b51ed42a1422`.

[GitHub release and downloads](https://github.com/lx-obsidian-labs/lx-obsidian-portal/releases/tag/v1.0.11) · [macOS device-build evidence](https://github.com/lx-obsidian-labs/lx-obsidian-portal/actions/runs/43689490832)

Live TV and Sports remain planned, with timing and availability unconfirmed.
