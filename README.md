# Heartrace

Heartrace is a small Android companion for building a gentler daily rhythm. It combines timestamped mood check-ins, short moment notes, and simple patterns derived from local activity.

The English app name is **Heartrace**; the Chinese app name is **心迹** (a trace of your mood).

## Features

- Tap `+` at any time to choose one of seven mood states and save a timestamped check-in, with or without a note. Multiple moments can be captured each day.
- Choose the date and time for each moment, including backfilled check-ins from earlier today or another day.
- See today\'s energy curve and moment timeline on Today.
- Switch the interface between English and Chinese.
- Manually switch between Dark and Light themes from `You -> Appearance`; the choice is persisted locally.

## Stack

- Kotlin
- Jetpack Compose and Material 3
- Room
- Hilt
- KSP

The app uses a single activity with screen-scoped MVVM. `TodayViewModel`, `PatternsViewModel`, and `YouViewModel` own screen state and events. `HeartraceRepository` exposes minimal data flows for each screen, and reusable Compose components render those states.

## Requirements

- Android Studio with Android SDK 37 installed
- JDK 11
- Android device or emulator running API 30 or newer

## Build and Run

From the repository root:

```powershell
.\gradlew.bat :app:assembleDebug
```

To verify Kotlin compilation without packaging:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

To run local unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Debug and release APKs use the checked-in test signing key automatically, so no manual signing setup is required for local builds. The key and its `android` password are for testing only and must not be used for production releases.

For a production-signed APK, create or obtain a private keystore and copy `keystore.properties.example` to `keystore.properties`. Set `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` to the real signing values; `keystore.properties` is ignored by Git. When that file is present and complete, `release` automatically uses the `production` signing config:

```powershell
keytool -genkeypair -v -keystore keystore\release.jks -alias heartrace-release -keyalg RSA -keysize 2048 -validity 10000
Copy-Item keystore.properties.example keystore.properties
# Edit keystore.properties with the generated keystore path and passwords.
.\gradlew.bat :app:assembleRelease
```

Keep the keystore and passwords backed up securely. Never commit either the keystore or `keystore.properties`; losing the production keystore prevents publishing updates signed with the same key.

Open the project in Android Studio and run the `app` configuration on a connected device or emulator. The application id is `me.heartrace`.

## Persistence

Heartrace stores app state, timestamped mood check-ins, and daily notes in the local Room database `heartrace.db`. No network service is required.

## Main Screens

- **Today**: timer, real mood-derived energy curve, and today\'s moment timeline.
- **Insights**: mood and energy trends derived from saved check-ins.
- **You**: language, theme, version information, and reset controls.

## Localization and Themes

English resources live under `app/src/main/res/values`; Chinese resources live under `app/src/main/res/values-zh`. Theme palettes are defined in `ui/theme/Theme.kt`, while components read the current palette through `HeartraceTheme.colors` so Light and Dark modes cover the full UI.
