# AGENTS.md

These instructions apply to the entire repository.

## Project Overview

Heartrace is a local-first Android app for mood check-ins, short moment notes, personal insights, and daily reminders. Keep changes aligned with the existing single-activity Compose architecture, Room-backed persistence, and English/Chinese localization.

## Toolchain

- Kotlin with Jetpack Compose and Material 3
- Android Gradle Plugin and Gradle Wrapper
- Room with Coroutines/Flow
- Hilt for dependency injection
- KSP for Room and Hilt code generation
- AndroidX Navigation 3
- Minimum Android SDK: 30
- Compile and target SDK: 37
- Java source and target compatibility: 11

## Module Map

- `app/src/main/kotlin/me/heartrace`: `Application` and `MainActivity` entry points.
- `core/src/main/kotlin/me/heartrace/data`: domain models, `HeartraceDataSource`, `HeartraceRepository`, and reminder scheduling.
- `core/src/main/kotlin/me/heartrace/data/db`: Room entities, DAOs, and `HeartraceDatabase`.
- `core/src/main/kotlin/me/heartrace/di`: Hilt bindings and database providers.
- `feature/src/main/kotlin/me/heartrace/feature/today`: Today check-ins, moment editing, energy display, and Today ViewModel state.
- `feature/src/main/kotlin/me/heartrace/feature/patterns`: Insights history, trends, goals, streaks, badges, and related ViewModel state.
- `feature/src/main/kotlin/me/heartrace/feature/you`: profile, achievements, preferences, appearance, and reset controls.
- `feature/src/main/kotlin/me/heartrace/feature/shell`: Compose app shell, tab navigation, and app settings ViewModel.
- `uikit/src/main/kotlin/me/heartrace/ui/components`: reusable Compose components, attachment handling, navigation chrome, and color aliases.
- `uikit/src/main/kotlin/me/heartrace/ui/theme`: shared Material 3 theme and custom palette.
- `uikit/src/main/res/values*`: primary English and Chinese UI strings.
- `core/src/main/res/values*`: reminder notification strings.
- `docs`: testing and development notes.

All Kotlin sources are under `src/main/kotlin` or `src/test/kotlin`; do not assume the older `src/main/java` layout.

## Architecture and State

- Preserve the single `MainActivity` plus Compose shell unless a change genuinely requires another activity.
- Keep screen state in the relevant ViewModel: `TodayViewModel`, `PatternsViewModel`, `YouViewModel`, `AppSettingsViewModel`, or `NoteComposerViewModel`.
- Use `HeartraceDataSource` as the feature-layer persistence contract. Implement durable behavior in `HeartraceRepository`, not in a screen-local substitute.
- Derive Today and Insights UI from repository flows backed by Room. Do not reintroduce fixed demo values or duplicate database state in Compose.
- Keep navigation in `feature/shell`; the three primary destinations are Today, Insights, and You.
- Use `AdaptiveTwoPane` and the existing navigation rail/bottom bar behavior when adding screen content so compact and large screens remain supported.

## Data and Persistence Rules

- The Room database is `heartrace.db`, currently schema version 5, with `DailyNoteEntity`, `MoodEntryEntity`, and `AppStateEntity`.
- A mood selection is a durable `MoodEntryEntity` event. Multiple check-ins per day are supported; do not reduce the model to one current mood.
- `DailyNoteEntity.moodEntryId` links an optional moment note to one mood entry and has a unique index. Preserve that one-to-one relationship when editing or deleting records.
- Persist preferences through `AppStateEntity`: language, theme, reminders, nickname, avatar URI, and weekly goal days.
- Add a Room migration for every schema change. Bump the version in `HeartraceDatabase` and register the migration in `DatabaseModule`; do not use destructive migration as a shortcut.
- Imported images, recorded audio, and avatars are copied into app-private attachment storage by the UIKit helpers. When replacing or deleting an attachment, preserve the repository's cleanup behavior and never delete arbitrary external files.
- Keep reminder scheduling coordinated with `ReminderSchedulerPort`. Enabling reminders must persist the setting and update the Android alarm; boot and time-change receivers must continue to reschedule it.

## UI, Localization, and Theme

- Put new user-facing UI strings in both `uikit/src/main/res/values/strings.xml` and `uikit/src/main/res/values-zh/strings.xml`. Use `stringResource` in Compose and `context.getString` outside Compose.
- Put new reminder notification copy in both matching `core/src/main/res/values/strings.xml` and `core/src/main/res/values-zh/strings.xml` files.
- There is no `HeartraceCopy` abstraction; use Android resource IDs and keep English, Chinese, and system-language behavior working independently from theme selection.
- Read colors from `HeartraceTheme.colors`, `MaterialTheme.colorScheme`, or the existing aliases in `uikit/src/main/kotlin/me/heartrace/ui/components/Colors.kt`. Avoid hard-coded screen colors that only work in Dark mode.
- Request and handle runtime permissions through the existing Activity Result patterns. Voice recording requires microphone permission; Android 13+ reminder notifications require notification permission.
- Keep accessibility labels, content descriptions, and layout behavior in both supported languages.
- Use ASCII in source and documentation unless a localized resource or product name requires another character set.

## Development Rules

- Prefer focused changes and preserve existing naming, package structure, and dependency direction.
- Avoid unrelated formatting, dependency upgrades, or architecture migrations.
- Do not commit changes or create branches unless the user explicitly asks.
- Do not add new libraries when existing AndroidX, Compose, Room, Hilt, or UIKit helpers solve the problem.
- Update relevant tests and documentation when behavior or developer workflow changes.

## Verification

Run commands from the repository root. On Windows, use `gradlew.bat` instead of `./gradlew`.

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew spotlessCheck
```

For Room, permission, navigation, or Compose changes, also run connected tests when an emulator or device is available:

```bash
./gradlew :core:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Inspect generated Room/build output after schema changes. A successful compile does not prove navigation, persistence, permission prompts, reminder delivery, or theme switching work at runtime; perform an install or emulator smoke test for those changes.

## Change Checklist

Before handing off a change, confirm:

1. The implementation uses the existing module and state boundaries.
2. Persisted data changes include a migration and relevant tests.
3. New user-facing copy exists in English and Chinese resources.
4. Light theme, Dark theme, compact layouts, and large-screen layouts remain usable.
5. The narrowest relevant Gradle verification tasks pass, or any unavailable environment/device limitation is reported.
