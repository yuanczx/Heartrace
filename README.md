# Heartrace

[English](README.md) | [简体中文](README.zh-CN.md)

Heartrace (Chinese: **心迹**) is a local-first Android app for noticing your mood, capturing small moments, and learning from your own patterns over time. It is designed for lightweight daily check-ins rather than long-form journaling or social sharing.

## Screenshots

The screenshots show the Chinese interface with sample records on an Android emulator.

| Today | Insights | You |
| :---: | :---: | :---: |
| <img src="docs/screenshots/today-light.png" alt="Today: mood check-in and energy timeline in Light theme" width="240"> | <img src="docs/screenshots/insights-light.png" alt="Insights: weekly goals and daily trends in Light theme" width="240"> | <img src="docs/screenshots/you-light.png" alt="You: profile and preferences in Light theme" width="240"> |
| <img src="docs/screenshots/today-dark.png" alt="Today: mood check-in and energy timeline in Dark theme" width="240"> | <img src="docs/screenshots/insights-dark.png" alt="Insights: weekly goals and daily trends in Dark theme" width="240"> | <img src="docs/screenshots/you-dark.png" alt="You: profile and preferences in Dark theme" width="240"> |

## Features

- **Today**: record one or more check-ins each day using seven mood states and an optional energy level.
- **Moments**: attach text, multiple images, and a voice note to a check-in; choose or edit the recorded date and time.
- **Insights**: review day, month, and year history with mood and energy trends, active days, weekly goals, streaks, and achievement badges.
- **You**: edit a nickname and avatar, choose the weekly goal, switch reminders on or off, change language, change theme, and reset today's records.
- **Localization**: English, Chinese, and system language selection.
- **Themes**: persisted Light, Dark, and system theme selection with a custom Material 3 palette and layouts that adapt to larger screens.
- **Reminders**: an optional daily check-in notification scheduled through Android alarms.

All mood records, notes, preferences, images, and voice notes remain on the device. The app does not require a network service or account.

## Tech Stack

- Kotlin and Jetpack Compose
- Material 3
- Room and Kotlin Coroutines/Flow
- Hilt for dependency injection
- KSP for Room and Hilt code generation
- AndroidX Navigation 3
- Gradle Version Catalog and Gradle Wrapper

The app uses a single activity and a Compose navigation shell. Screen ViewModels consume the `HeartraceDataSource` contract, while `HeartraceRepository` coordinates Room flows, attachment cleanup, settings, and reminders.

## Project Layout

```text
app/       Application entry point, manifest, signing, and instrumentation tests
core/      Domain models, data-source contract, Room database, repository, reminders
feature/   Compose screens, screen ViewModels, UI state, navigation, and feature tests
uikit/     Shared Compose components, theme, attachment UI, and localized strings
docs/      Development and testing notes
```

The Gradle dependency direction is intentionally small:

```text
app -> feature -> core
app -> uikit -> core
feature -> uikit
```

## Requirements

- Android Studio with Android SDK Platform 37
- Android Studio's bundled JDK (build verified with JDK 21)
- An Android device or emulator running API 30 or newer

The project uses compile and target SDK 37, minimum SDK 30, and Java source/target compatibility 11. The application id is `me.heartrace`.

## Build and Run

From the repository root, use the Gradle wrapper:

```bash
# macOS/Linux
./gradlew :app:assembleDebug

# Windows PowerShell
.\gradlew.bat :app:assembleDebug
```

Open the project in Android Studio and run the `app` configuration on a connected device or emulator. The debug build is signed with the checked-in test key for local development.

Useful verification tasks:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew spotlessCheck
```

On Windows, replace `./gradlew` with `.\gradlew.bat`.

Connected Room and Compose tests require a device or emulator:

```bash
./gradlew :core:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

See [`docs/testing.md`](docs/testing.md) for test ownership and the optional JaCoCo coverage task.

## Release Signing

For local builds, the `release` variant falls back to the test signing key when no production signing properties are present. Do not publish an APK signed with that key.

To configure a production-signed build:

1. Copy `keystore.properties.example` to `keystore.properties`.
2. Set `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` to a private keystore's values.
3. Keep both the keystore and `keystore.properties` out of version control.
4. Build the release artifact:

```bash
./gradlew :app:assembleRelease
```

The release build enables code shrinking and resource shrinking. Losing the production keystore prevents publishing updates signed with the same key.

## Local Persistence

Room stores data in the app-private database `heartrace.db`. The current schema contains:

- `MoodEntryEntity`: timestamped mood and optional energy records.
- `DailyNoteEntity`: text and attachments linked to at most one mood entry.
- `AppStateEntity`: current preferences, profile data, and the latest mood fallback.

Imported images, voice recordings, and avatars are copied into the app-private `files/attachments` directory. No remote backup or synchronization is implemented.

## Localization and Themes

Most user-facing strings live in `uikit/src/main/res/values/strings.xml` and `uikit/src/main/res/values-zh/strings.xml`. Reminder notification strings live in the matching resource directories under `core`. Keep the English and Chinese resources in sync when adding copy.

The shared palette and Material 3 theme live in `uikit/src/main/kotlin/me/heartrace/ui/theme/Theme.kt`. Components should read colors from `HeartraceTheme.colors` or the aliases in `uikit/src/main/kotlin/me/heartrace/ui/components/Colors.kt` so both Light and Dark themes remain usable.

## License

No license has been declared in this repository yet.
