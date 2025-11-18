# F-Droid Submission Information

This document contains information for F-Droid maintainers and contributors about the NutCracker app.

## App Information

- **Package Name**: `com.offlinelabs.nutcracker`
- **App Name**: NutCracker
- **Category**: Health & Fitness
- **License**: AGPL-3.0
- **Website**: https://offline-labs.com
- **Source Code**: https://github.com/off-lineLabs/nutcracker
- **Issue Tracker**: https://github.com/off-lineLabs/nutcracker/issues
- **Changelog**: https://github.com/off-lineLabs/nutcracker/blob/main/CHANGELOG.md

## Description

Privacy-first offline calorie tracker. No ads, no tracking, no data collection.

Track your meals and exercises with complete privacy. No cloud sync, no data collection, just pure functionality.

## Features

- Complete nutrition tracking (calories, macros, fiber, sodium)
- Barcode scanning for instant product information
- Search millions of food products via Open Food Facts
- Hundreds of preloaded exercises from open-source database
- Supplement tracking (up to 5 supplements)
- TEF (Thermic Effect of Food) bonus calculator
- Progress analytics and charts
- Fully offline functionality
- Multi-language support (English, Spanish, Portuguese)
- Material Design 3 UI with dark/light themes
- No ads, no tracking, no data collection

## FLOSS Compliance

### ✅ All Dependencies Are FLOSS

NutCracker uses only Free and Open Source Software (FLOSS) libraries:

#### Core Android (Apache 2.0)
- AndroidX libraries (Compose, Room, Navigation, DataStore, etc.)
- Material Design 3
- Jetpack Compose
- Kotlin stdlib and coroutines

#### ML Kit (Apache 2.0)
- `com.google.mlkit:barcode-scanning` - standalone, bundled model, no Play Services
- `com.google.android.gms:play-services-code-scanner` - GMS Core barcode UI

Note: The barcode scanning library uses GMS Core for the scanner UI, but falls back to manual entry if unavailable. This is acceptable for F-Droid as it degrades gracefully.

#### Third-party (Apache 2.0 / MIT)
- Retrofit + OkHttp (networking)
- Gson (JSON parsing)
- Coil (image loading)
- Timber (logging)
- Hilt (dependency injection)

### ❌ No Proprietary Dependencies

- No Firebase
- No Google Analytics
- No Crashlytics
- No ad networks
- No tracking services
- No proprietary crash reporting

### Data Sources

NutCracker uses two open-source databases:

1. **Open Food Facts** (ODbl v1.0)
   - URL: https://world.openfoodfacts.org/
   - Food product database with nutritional information
   - Collaborative, open database

2. **Free Exercise DB** (Public Domain)
   - URL: https://github.com/wrkout/exercises.json
   - Exercise database with instructions
   - Maintained by Wrkout community

Both databases are accessed via HTTP API and work without authentication.

## Build Information

### Versioning

NutCracker uses git-based versioning:
- **Version Code**: Git commit count (`git rev-list --count HEAD`)
- **Version Name**: Latest git tag without 'v' prefix (`git describe --tags --abbrev=0`)

For F-Droid builds, you may need to override versioning:
- Set `versionCode` explicitly in the F-Droid recipe
- Set `versionName` to match the release tag

### Build Requirements

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36
- **JDK**: 11 or higher
- **Gradle**: 8.13.0+ (wrapper included)
- **AGP**: 8.13.0+
- **Kotlin**: 2.2.20+
- **KSP**: Used for Room annotation processing

### Build Command

```bash
./gradlew assembleRelease
```

### Build Flags

For reproducible builds, consider:
- Setting fixed build timestamps
- Using consistent JDK version

### ProGuard/R8

The app currently has R8 code shrinking **disabled** (`isMinifyEnabled = false`) in release builds. ProGuard rules are defined in:
- `app/proguard-rules.pro`

## AntiFeatures

NutCracker has **NO** antifeatures:

- ✅ No ads
- ✅ No tracking
- ✅ No non-free network services (APIs are open and public)
- ✅ No non-free assets
- ✅ No non-free dependencies
- ✅ No upstream non-free

The app does make network requests to:
- `world.openfoodfacts.org` - for food product data (open API, no authentication)
- `raw.githubusercontent.com` - for exercise database (public GitHub content)

Both are optional features that degrade gracefully when offline.

## Screenshots

Screenshots are available in the repository:
- https://github.com/off-lineLabs/off-lineLabs.github.io/tree/main/public

F-Droid maintainers can use these or generate new ones from the app.

## Metadata

Metadata for F-Droid is provided in `fastlane/metadata/android/` directory following the standard structure:

```
fastlane/metadata/android/
└── en-US/
    ├── title.txt
    ├── short_description.txt
    ├── full_description.txt
    └── changelogs/
        └── 1.txt
```

## Testing

The app can be tested without any special setup:

1. Install the APK
2. Grant camera permission (optional, for barcode scanning)
3. All core features work offline
4. Network features (food search, exercise search) require internet

## Support & Contact

- **Issues**: https://github.com/off-lineLabs/nutcracker/issues
- **Discussions**: https://github.com/off-lineLabs/nutcracker/discussions
- **Website**: https://offline-labs.com/wemade/nutcracker

## F-Droid Submission Checklist

- [x] App is 100% FLOSS
- [x] All dependencies are FLOSS
- [x] No proprietary libraries or services
- [x] No ads or tracking
- [x] License file included (AGPL-3.0)
- [x] Source code publicly available
- [x] Metadata provided in standard format
- [x] Changelog maintained
- [x] Build instructions documented
- [x] README with comprehensive information

## Additional Notes

### ML Kit Barcode Scanner

The app uses `com.google.android.gms:play-services-code-scanner` for the barcode scanner UI. This library:

- Provides a pre-built camera UI for scanning barcodes
- Requires GMS Core to be installed on the device
- Does not send data to Google servers
- Processes everything on-device

F-Droid users without GMS Core can still use the app fully by:
- Searching for food by name
- Manually creating meals

The degraded experience is acceptable and the app remains fully functional.

### Internet Permissions

The app requests `INTERNET` permission for:
- Fetching food data from Open Food Facts API
- Downloading exercise database from GitHub
- All network requests are optional
- App works 100% offline for core functionality

### Camera Permissions

The app requests `CAMERA` permission for:
- Barcode scanning functionality
- Permission is optional (not required)
- Feature marked as not required: `android:required="false"`

---

**Thank you for considering NutCracker for F-Droid!**

This app represents our commitment to privacy, transparency, and user freedom. We believe in building software that respects users and puts them in control of their data.

If you have any questions or need additional information, please open an issue on GitHub.
