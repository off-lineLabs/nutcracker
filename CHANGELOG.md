# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2026-11-21

### Added
- Redesigned Analytics: a summary of your average intake against your goal, tiles for average calories burned, average balance, days logged and logging streak, a clearer chart with a goal line, and a macro breakdown (carbs, protein, fat, fiber and salt) against your goals
- Analytics weeks and months now show up to 12 weeks and 12 months of history; tap a bar to see that period
- Redesigned Exercise tab in Analytics, matching the nutrition view
- Daily goals: link calories and macros with the arrow next to Calories. Down lets calories set the macros, Up lets the macros set the calories, and struck through keeps them independent. Your choice is remembered
- Daily goals: a "?" explains how the Real value is calculated
- Importing a backup now asks whether to merge it with your data (nothing is deleted) or replace your data with it; replacing saves a safety copy first
- Every delete now asks for confirmation, and deleted check-ins can be restored with Undo

### Changed
- Salt is shown and entered in milligrams everywhere (e.g. "1,500 mg")
- Redesigned backup screen with clearer export and import buttons and readable import results in dark mode
- Dates and decimal numbers follow your language (e.g. "12,5 g" in Spanish and Portuguese)

### Fixed
- Logging a recipe overwrote the nutrition of earlier recipe logs, added a stray item to your food list and could fail; existing recipe logs are repaired automatically
- Backups were missing deleted foods and recipe logs, so restoring could lose history, and recipe check-ins failed to import ("Meal ID must be a positive integer")
- Importing the same backup twice duplicated your data and the default supplement
- Choosing a file that is not a backup no longer changes your data
- Average balance per day in Analytics was not divided by the number of days
- Exercises picked from the exercise database left "Kcal per set" empty
- Meal details showed salt as "1500.0mg"
- The FAQ said daily goals are set in Settings; they are edited with the pencil on the home screen
- Rotating the phone or changing the language closed Settings, Analytics and Help

## [1.3.0] - 2026-11-20

### Added
- Redesigned value entry in every dialog (meals, serving size, recipe ingredients, check-ins, exercises and goals): unit labels, coloured nutrient rows, inline error messages, and Save is disabled until the input is valid
- Numbers can be typed with a comma or a dot (e.g. "12,5")
- Remaining English-only texts are now translated into Spanish and Portuguese, including unit names such as cup, slice and serving

### Changed
- Release builds are optimized and shrunk with R8, making the app about 35% smaller
- Salt is entered in grams in the meal form and the daily goal
- Calories with decimals are rounded to the nearest whole number instead of being rejected
- Dialogs keep what you typed when the screen rotates and scroll properly with the keyboard open on small phones and in landscape

### Fixed
- Weekly and monthly calorie targets in Analytics were wrong in Spanish and Portuguese
- Check-ins logged for another day could be saved one day early in time zones behind UTC
- Cardio check-ins could not be confirmed
- Editing a cardio or bodyweight exercise reset its default minutes or reps to 0
- Typing a decimal comma saved the value as 0
- The export confirmation message never appeared
- The import result dialog cut off its close button and numbers in Spanish and Portuguese
- The app could not be installed on devices without portrait support, such as some Chromebooks and XR headsets

## [1.2.0] - 2026-11-18

### Added
- Recipes: combine foods into a recipe, see its total nutrition and log it like any other meal
- Redesigned goal dialog with draggable macro bars and the TEF bonus
- Max weight shown on the exercise screen and in exercise details
- Recipes are included in database export/import backups

### Changed
- Upgraded the build toolchain (Android Gradle Plugin 9.5, Gradle 9.6) and Room 2.8.5
- Removed an unused dependency and debug logging

### Fixed
- Recipe nutrition now respects the unit chosen for each ingredient (e.g. kg vs g, L vs ml, servings)
- Dragging a macro bar a second time could jump back to an old value
- Missing Portuguese and Spanish translation for "Edit"

## [1.1.1] - 2025-11-09

### Changed
- Proper source for exercises

## [1.1.0] - 2025-11-09

### Added
- Support to new languages via Google Play

### Changed
- Edge-to-edge theming improvements

## [1.0.0] - 2025-11-09

### Added
- Complete nutrition tracking with calorie and macro monitoring
- Barcode scanning for instant product information
- Search millions of food products via Open Food Facts
- Hundreds of preloaded exercises from open-source database
- Supplement tracking for up to 5 daily supplements
- TEF (Thermic Effect of Food) bonus calculator
- Thumbwheel control for adjusting portion sizes
- Interactive tutorial screen for new users
- Material Design 3 UI with dark/light themes
- Multi-language support (English, Spanish, Portuguese)
- Fully offline functionality
- No ads, no tracking, no data collection

### Changed
- Faster and more efficient meal search
- Completely revamped and improved tutorial
- Interface improvements: smoother scrolling and color fixes
- Better colors for higher contrast on some dialogs

### Fixed
- R8 bug that would crash the app when opening the exercise dialog
- Hard-coded / untranslated strings on the nutrient details dialog
- List of muscle groups showing incomplete when searching for exercises


