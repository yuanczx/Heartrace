# AGENTS.md

## Project Scope

These instructions apply to the entire repository.

Heartrace is an Android app for lightweight mood check-ins, focus sessions, and daily notes. Keep changes aligned with the existing Compose UI and Room-backed state model.

## Technology

- Kotlin with Jetpack Compose and Material 3
- Android Gradle Plugin and Gradle Wrapper
- Room for local persistence
- Hilt for dependency injection
- KSP for Room and Hilt code generation
- Minimum Android SDK: 30
- Compile and target SDK: 37
- Java source and target compatibility: 11

## Repository Layout

- `core/src/main/java/me/heartrace/data`: domain models and repository logic
- `core/src/main/java/me/heartrace/data/db`: Room entities, DAOs, and database migrations
- `core/src/main/java/me/heartrace/di`: Hilt providers
- `core/src/main/java/me/heartrace/ui/components`: reusable Compose components
- `core/src/main/java/me/heartrace/ui/theme`: Material theme and Heartrace color palettes
- `core/src/main/java/me/heartrace/ui/localization`: localized UI copy and context handling
- `core/src/main/res/values*`: core app strings and resources
- `feature/src/main/java/me/heartrace/feature/today`: Today screen, ViewModel, UI state, and components
- `feature/src/main/java/me/heartrace/feature/patterns`: Patterns screen, ViewModel, and UI state
- `feature/src/main/java/me/heartrace/feature/archive`: Archive screen, ViewModel, and UI state
- `feature/src/main/java/me/heartrace/feature/you`: You screen, ViewModel, and UI state
- `feature/src/main/java/me/heartrace/feature/shell`: App shell (`HeartraceApp`) and top-level ViewModel
- `feature/src/main/res/values*`: feature-specific English and Chinese string resources
- `app/src/main/java/me/heartrace`: Application entry (`HeartraceApplication`) and `MainActivity`

## Development Rules

- Preserve the existing single-activity Compose architecture unless a change requires otherwise.
- Keep user-visible state in `HeartraceViewModel` and persist durable state through `HeartraceRepository`; do not add screen-local substitutes for persisted data.
- Add a Room migration whenever an entity or schema changes. Update the database version and register the migration in `DatabaseModule`.
- Keep all new user-facing text in both `values/strings.xml` and `values-zh/strings.xml`, then expose it through `HeartraceCopy`.
- Use `HeartraceTheme.colors` or the dynamic component color properties for UI colors. Do not introduce hard-coded dark-only colors in screens.
- Prefer focused changes. Avoid unrelated formatting or dependency upgrades.
- Use ASCII in source and documentation unless existing localized resources require another character set.

## Verification

Run the Kotlin compilation task after source changes:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Run unit tests when the local Gradle distribution and dependencies are available:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

For database or UI changes, also inspect the generated Room code/build output and perform an install or emulator smoke test when an Android device is available. Do not treat a successful compile as proof that navigation, persistence, or theme switching works at runtime.

## Data and UI Contracts

- `HeartraceUiState` is the UI contract consumed by all screens. Add derived values there when multiple screens need the same data.
- Focus completions are stored as `FocusSessionEntity` records. Pattern metrics should be derived from those records rather than reintroducing fixed demo values.
- Mood check-ins are stored as timestamped `MoodEntryEntity` records. Selecting a mood is a durable event, not only a current-state update.
- Theme preference is persisted as part of `AppStateEntity`; changes must survive process recreation.
- The app currently supports English and Chinese. Keep language selection and theme selection independent.
