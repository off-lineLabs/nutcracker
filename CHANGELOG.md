# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.0] - Unreleased

### Added
- Gyms: the same machine can weigh differently at different gyms, so you can now keep separate exercises per gym. Create a gym with a name and an optional unit or location, edit it, or delete it
- Deleting a gym asks whether to keep its exercises as shared or remove them too
- A gym dropdown at the top of the Exercise tab: pick a gym to see its exercises followed by a Shared section, or choose All gyms to see the shared exercises. Your selection is remembered
- Assign an exercise to a gym, move it to another gym, or copy it to another gym (a copy starts with no history)
- Gym names show on exercise check-ins on Home and in the exercise details
- Backups now include your gyms
- A new Help entry explaining gyms
- Sort the Food and Exercise lists by your own order, name, date added or last check-in, ascending or descending, and group them (Food by meal type or NOVA; Exercise by gym, routine, muscle group or exercise type). In your own order, hold an item and drag it to move it. Your choices are remembered
- Routines: plan workouts like “Leg day” at a gym, with that gym’s and shared exercises in your own order. Group the Exercise list by routine to follow it
- Meal types: tag foods as Breakfast, Lunch, Dinner, Snack or your own types, and group the Food list by them
- Synced copies: when copying an exercise to another gym you can keep the copies in sync, so changing one updates the other. Stop syncing any time from the edit screen
- Choose muscle groups when adding or editing an exercise by searching the exercise database's list (shown in your language) or typing your own
- Backups now include your list order, routines, meal types and synced copies

### Changed
- The two buttons on the home screen are replaced by a tab bar with Food, Home and Exercise. Swipe between tabs or tap them
- The Food tab shows the meal picker directly (search, add, barcode scan, food database and recipes) and the Exercise tab shows your exercise list, both full height under the same header
- After logging from the Food or Exercise tab you stay on that tab and a "Logged · View" message appears; View takes you to Home
- Back from the Food or Exercise tab returns to Home
- The tutorial and Help now describe the tabs instead of the buttons
- Logging from the Food or Exercise tab uses the day shown at the top, so you can fill in a past day without changing the date in each check-in
- Analytics opens on the Exercise section when you come from the Exercise tab, and going back returns you to the same tab and day
- Picking a gym for an exercise uses a dropdown, and Copy to another gym opens a sheet listing each gym with its unit
- All gyms in the Exercise tab now lists every exercise, grouped by gym by default

### Fixed
- The meal details dialog stays open when the phone is rotated
- The Copy to another gym button was hard to read in dark mode

## [1.4.0] - 2026-11-22

### Added
- Redesigned Analytics: a summary of your average intake against your goal, tiles for average calories burned, average balance, days logged and logging streak, a clearer chart with a goal line, and a macro breakdown (carbs, protein, fat, fiber and salt) against your goals
- Analytics weeks and months now show up to 12 weeks and 12 months of history; tap a bar to see that period
- Redesigned Exercise tab in Analytics, matching the nutrition view
- Daily goals: link calories and macros with the arrow next to Calories. Down lets calories set the macros, Up lets the macros set the calories. Your choice is remembered
- Daily goals: each macro now shows its calories and its share of the total next to the grams
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
- Daily goals: with one macro locked, the target markers on the macro bars were in the wrong place; they now show their target grams and calories
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


