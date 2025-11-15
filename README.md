# NutCracker

[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Google Play](https://img.shields.io/badge/Google%20Play-Download-brightgreen.svg)](https://play.google.com/store/apps/details?id=com.offlinelabs.nutcracker)

**Track your meals and exercises with complete privacy. No cloud sync, no data collection, just pure functionality.**

NutCracker is a privacy-first, offline-first calorie tracking app for Android. Built with modern Android technologies and designed to work seamlessly without an internet connection, NutCracker puts you in complete control of your nutrition and fitness data.

## Our Core Values

- **Offline First**: Your app should work when you need it most, even without internet connection
- **Privacy by Design**: Your data belongs to you. No tracking, no surveillance, no data collection
- **Open Source**: Transparency builds trust. All our code is open source and auditable
- **Focused Utility**: No bloat, no unnecessary features. Just fast, efficient software that works
- **Community**: Built by the community, for the community. No corporate interests or profit motives
- **User Empowerment**: Full control, no unnecessary limits. Our apps let you customize and optimize your experience as you see fit.

## Features

**Available in 🇬🇧 English, 🇪🇸 Spanish (Spain), and 🇧🇷 Portuguese (Brazil)**

### Nutrition Tracking
- **Calorie Tracking**: Monitor your daily calorie intake with an intuitive circular progress indicator
- **Macro Tracking**: Track carbohydrates, protein, fat, fiber, and sodium
- **TEF Bonus**: See the thermic effect of food bonus for protein-rich meals
- **Supplement Tracking**: Keep track of the daily consumption of up to 5 supplements. No more worries about forgetting them.

### Meal and exercise logging
- **Barcode Scanning**: Scan products barcodes to instantly get nutritional information, including NOVA and NUTRI score.
- **Exercise Database**: Access hundreds of preloaded exercises from an open-source database
- **Progress Tracking**: Monitor your exercise history and progress over time

### Privacy & Performance
- **100% Offline**: All core functionality works without internet connection
- **No Data Collection**: We don't even have a server, your data stays on your device
- **No Ads**: Clean, distraction-free experience
- **No Trackers**: Zero tracking or analytics services
- **Fast & Lightweight**: Optimized for performance with minimal resource usage

## Screenshots

<p float="left">
  <img src="https://github.com/off-lineLabs/off-lineLabs.github.io/blob/main/public/nutcracker-screenshot-1.png?raw=true" alt="Image 1" height="400" />
  <img src="https://github.com/off-lineLabs/off-lineLabs.github.io/blob/main/public/nutcracker-screenshot-2.png?raw=true" alt="Image 2" height="400" />
  <img src="https://github.com/off-lineLabs/off-lineLabs.github.io/blob/main/public/nutcracker-screenshot-3.png?raw=true" alt="Image 3" height="400" />
  <img src="https://github.com/off-lineLabs/off-lineLabs.github.io/blob/main/public/nutcracker-screenshot-4.png?raw=true" alt="Image 4" height="400" />
</p>

## Download & Installation

Download NutCracker directly from the Google Play Store:

<a href="https://play.google.com/store/apps/details?id=com.offlinelabs.nutcracker" target="_blank">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="100" />
</a>


### Requirements

- **Android 8.0 (API 26)** or higher

## About Offline Labs

NutCracker is part of the [Offline Labs](http://offline-labs.com/) project, a community-driven initiative creating free, open source, ad-free alternatives for everyday apps.

## Get Involved

We're always looking for more people to join us! Whether you're a developer, designer, tester, or just someone who cares about privacy, there's a place for you here.

- **Code Development**: Contribute to our open source projects
- **Bug Reports**: Help us make our apps more stable and reliable
- **Documentation**: Help improve documentation, write tutorials, or translate our apps
- **Community**: Join our discussions, share ideas, and help others

Visit [offline-labs.com](http://offline-labs.com/) to learn more and get involved.


---

## For Developers

### Building from Source

#### Prerequisites

- **Android Studio** (latest stable version recommended)
- **JDK 11** or higher
- **Android SDK** with API 26+ and build tools
- **Git** for cloning the repository

#### Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/off-lineLabs/nutcracker.git
   cd nutcracker
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned `nutcracker` directory
   - Wait for Gradle sync to complete

3. **Build the project**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```
   
   Or use ADB:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Generate release APK**:
   ```bash
   ./gradlew assembleRelease
   ```
   
   The APK will be located at: `app/build/outputs/apk/release/app-release.apk`

#### Versioning

NutCracker uses git-based versioning:
- **Version Code**: Automatically generated from git commit count
- **Version Name**: Automatically generated from the latest git tag (e.g., `v1.0.0`)

### Tech Stack

NutCracker is built with modern Android development tools and best practices:

#### Core Technologies
- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern declarative UI framework
- **Material Design 3**: Latest Material Design components and theming

#### Architecture & Libraries
- **Room Database**: Local SQLite database with type-safe queries
- **Hilt**: Dependency injection framework
- **Kotlin Coroutines**: Asynchronous programming
- **StateFlow**: Reactive state management

#### Features & Services
- **ML Kit Barcode Scanning**: Google's ML Kit for barcode recognition
- **Retrofit**: Type-safe HTTP client for API calls
- **OkHttp**: HTTP client with logging interceptor
- **Coil**: Image loading library for Compose
- **Timber**: Logging framework

#### Data Sources
- **Open Food Facts**: Food product database and nutritional information
  - API: `https://world.openfoodfacts.org/`
  - Open source, collaborative database
- **Free Exercise DB**: Exercise database maintained by Wrkout
  - Repository: [https://github.com/wrkout/exercises.json](wrkout/exercises.json) on GitHub
  - Comprehensive exercise library with instructions

### Project Structure

```
nutcracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/offlinelabs/nutcracker/
│   │   │   │   ├── data/           # Data layer (Room, repositories, services)
│   │   │   │   ├── ui/              # UI layer (Compose screens, components)
│   │   │   │   ├── util/            # Utilities and helpers
│   │   │   │   └── MainActivity.kt  # Main entry point
│   │   │   ├── res/                 # Resources (strings, layouts, assets)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                    # Unit tests
│   └── build.gradle.kts             # App-level build configuration
├── gradle/
│   └── libs.versions.toml          # Dependency version catalog
├── build.gradle.kts                 # Project-level build configuration
└── settings.gradle.kts              # Project settings
```

### Key Components

- **MainActivity**: Entry point, handles navigation and theme
- **FoodLogApplication**: Application class with dependency initialization
- **AppDatabase**: Room database with all entities and DAOs
- **FoodLogRepository**: Repository pattern for data access
- **DashboardScreen**: Main screen with calorie ring and meal list
- **SettingsScreen**: App settings and preferences
- **AnalyticsScreen**: Progress charts and analytics
- **UnifiedCheckInDialog**: Dialog for logging meals and exercises

### Contributing

We welcome contributions from the community! Here's how you can help:

#### How to Contribute

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes** following our coding standards
4. **Test thoroughly** to ensure everything works
5. **Commit your changes**: `git commit -m 'Add amazing feature'`
6. **Push to your branch**: `git push origin feature/amazing-feature`
7. **Open a Pull Request**

#### Contribution Guidelines

- **Code Standards**: Follow Kotlin coding conventions and Android best practices
- **Documentation**: Write clear, documented code with meaningful comments
- **Testing**: Test your changes thoroughly before submitting
- **Commits**: Keep commits focused and atomic with clear messages
- **License**: All contributions must comply with AGPL-3.0 license

#### Reporting Issues

Found a bug or have a feature request? Please open an issue on GitHub with:
- Clear description of the problem or feature
- Steps to reproduce (for bugs)
- Expected vs actual behavior
- Device/Android version information (if relevant)

#### Community Guidelines

- Be respectful and inclusive
- Help others learn and grow
- Share knowledge and experience
- Follow our code of conduct

For more information, visit the [Offline Labs contribution guidelines](http://offline-labs.com/).

### License

NutCracker is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.

This means:
- ✅ You can use, modify, and distribute the software
- ✅ You must make your modifications available under the same license
- ✅ You must include the original license and copyright notice
- ✅ If you modify the software and make it available over a network, you must provide the source code

#### Additional Restrictions

In addition to the AGPL-3.0 license, this software is subject to the following restrictions (see [LICENSE-exception.md](LICENSE-exception.md)):

- **Rebranding Requirement**: Modified versions must be clearly rebranded. Use of "NutCracker", "Offline Labs", or associated trademarks is prohibited without explicit permission.
- **Ad Prohibition**: Inclusion of advertising content is strictly forbidden.
- **Data Brokering Prohibition**: Collection, sale, or brokerage of user data is prohibited.

See the [LICENSE](LICENSE) file for the full license text and [LICENSE-exception.md](LICENSE-exception.md) for additional restrictions.

The Terms of Use and Privacy Policy are available in the repository at [app/src/main/res/raw/terms.html](app/src/main/res/raw/terms.html) and are also accessible in-app (Settings → Legal Information).

## Links & Resources

### Documentation
- **Terms of Use & Privacy Policy**: [app/src/main/res/raw/terms.html](app/src/main/res/raw/terms.html)
- **Community Discussions**: [GitHub Discussions](https://github.com/off-lineLabs/nutcracker/discussions)

### Data Sources
- **Open Food Facts**: [world.openfoodfacts.org](https://world.openfoodfacts.org/)
- **Free Exercise DB**: [https://github.com/wrkout/exercises.json](https://github.com/wrkout/exercises.json)

## Acknowledgments

- **Open Food Facts** for providing the food product database
- **@Wrkout** for maintaining the free exercise database
- All open source libraries and tools that make this project possible

---

*No trackers. No data collection. No financial interests. Just privacy-first software that works.*
