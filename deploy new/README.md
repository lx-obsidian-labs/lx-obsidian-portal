# Vista Cinema - Deployment Package

## Contents

```
deploy/
  index.html                  - Download landing page
  privacy-policy.html         - Privacy policy page
  VistaCinema-v1.0.0.apk     - Signed release APK (7.5 MB)
  screenshots/                - App screenshots for marketing
  assets/                     - Additional assets
  README.md                   - This file
```

## Quick Start

1. Upload the entire `deploy/` folder to your web server
2. Ensure HTTPS is enabled (required for APK downloads on Android)
3. Share the URL with users to download and install

## APK Details

| Field | Value |
|-------|-------|
| Version | 1.0.0 |
| Version Code | 1 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |
| Size | ~7.5 MB |
| Signing | Release keystore (release-key.jks) |

## SHA-256 Checksum

```
A5379D16F5BC7BC955F091C124622DDFFC3B4C8AC7B621131AA76F620F9CE493
```

Verify integrity with:
```bash
# Windows (PowerShell)
Get-FileHash VistaCinema-v1.0.0.apk -Algorithm SHA256

# Linux / macOS
shasum -a 256 VistaCinema-v1.0.0.apk
```

## Screenshots

Marketing screenshots are in the `screenshots/` folder (SVG format):

| Filename | Description |
|----------|-------------|
| `home.svg` | Home screen with continue watching + trending |
| `discover.svg` | Discover/browse with categories and genres |
| `details.svg` | Movie details with cast and where to watch |
| `player.svg` | Video player / playback screen |
| `search.svg` | Search screen with results |
| `profile.svg` | User profile and settings |

> **Tip:** To convert SVGs to PNG for social media, open them in a browser and screenshot, or use a tool like Figma/Canva.

## Updating

When releasing a new version:
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Rebuild: `.\gradlew.bat --no-daemon assembleRelease`
3. Copy the new APK to `deploy/`
4. Rename it to `VistaCinema-v{version}.apk`
5. Update `index.html` download link and version info
6. Update the SHA-256 checksum
7. Upload to your server

## Keystore

**IMPORTANT:** Keep `release-key.jks` and `keystore.properties` secure and backed up.
Losing the keystore means you cannot update your app.

## Notes

- Android will show a security warning when installing APKs from outside the Play Store
- Users need to enable "Install from Unknown Sources" in their browser/device settings
- HTTPS is required for the download to work on modern Android browsers
