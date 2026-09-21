# Heartrace testing

The test suite is split by ownership and execution environment:

- `core/src/androidTest` runs repository and Room tests against an in-memory SQLite database. It covers value bounds, note/check-in joins, attachment metadata, preferences, reset behavior, and recent history.
- `feature/src/test` runs ViewModel and navigation tests on the JVM with a deterministic coroutine main dispatcher and an in-memory data-source fake. It covers success, failure, loading, editing, and boundary states for Today, note composition, Patterns, You, settings, and shell navigation.
- `app/src/androidTest` contains a Compose behavior smoke test for the three primary tabs and back-to-today navigation.

Run the JVM suite with:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Compile all production and test sources with:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Run the Room and Compose tests on a connected emulator or device with:

```powershell
.\gradlew.bat :core:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Jacoco is opt-in because the agent is not needed for normal test execution. Enable it in an environment with the Jacoco artifact available:

```powershell
.\gradlew.bat :app:testDebugUnitTest -PenableCoverage
```
