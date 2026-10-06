# Heartrace testing

The test suite is split by ownership and execution environment:

- `core/src/androidTest` runs repository and Room tests against an in-memory SQLite database. It covers value bounds, note/check-in joins, attachment metadata, preferences, reset behavior, recent history, and Insights updates when the weekly goal changes without emitting again for unrelated preferences.
- `feature/src/test` runs ViewModel and navigation tests on the JVM with a deterministic coroutine main dispatcher and an in-memory data-source fake. It covers success, failure, loading, editing, and boundary states for Today, note composition, Patterns, You, settings, and shell navigation. Metrics tests also cover duplicate daily check-ins, consecutive weekly goals, future weeks, and daylight-saving transitions. History tests cover inclusive starts and exclusive ends for day, month, and year ranges.
- `app/src/androidTest` contains Compose behavior tests for the three primary tabs, back-to-today navigation, and achievement back handling. It waits for the destination to become visible after each animated tab transition. The achievement regression test opens achievements, switches to Insights, dispatches system back, and checks that the host receives it. Returning to You preserves achievements, where system back still closes them.

Run the JVM suite with:

```powershell
.\gradlew.bat :feature:testDebugUnitTest :app:testDebugUnitTest
```

Compile production sources with:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Run the Room and Compose tests on a connected emulator or device with:

```powershell
.\gradlew.bat :core:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Jacoco is opt-in because the agent is not needed for normal test execution. Enable it in an environment with the Jacoco artifact available:

```powershell
.\gradlew.bat :feature:testDebugUnitTest :app:testDebugUnitTest -PenableCoverage
```

On Linux and macOS, replace `.\gradlew.bat` with `./gradlew`. The feature task
is required: the app module currently has no JVM test sources.

For performance checks, use a large history with several check-ins per day.
Navigate between day, month, and year history and edit a record's timestamp.
Confirm that statistics update and that image previews and audio playback stay
with their original record when rows move. Check both themes, English and
Chinese, and compact and large screen layouts.

Weekly badge calculation now groups distinct days by week once instead of
filtering the full record list for each week. Repository snapshot conversion
runs on `Dispatchers.Default`; unchanged query results and unrelated preference
changes do not trigger downstream Insights calculations. Snapshot flows still
query Room on collection and retain the existing day/minute refresh behavior.
