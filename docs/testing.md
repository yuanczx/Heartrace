# Heartrace testing

The test suite is split by ownership and execution environment:

- `core/src/androidTest` runs repository and Room tests against an in-memory SQLite database. It covers value bounds, note/check-in joins, attachment metadata, preferences, reset behavior, recent history, and Insights updates when the weekly goal changes without emitting again for unrelated preferences. Reminder resource tests verify that persisted English/Chinese overrides a fresh receiver context and that System clears the previous app language.
- `feature/src/test` runs ViewModel and navigation tests on the JVM with a deterministic coroutine main dispatcher and an in-memory data-source fake. It covers success, failure, loading, editing, and boundary states for Today, note composition, Patterns, You, settings, and shell navigation. Gated saves exercise locked inputs, attachment ownership, failure retries, and commits after a destination is cleared. Both record editors preserve drafts on repository refresh, discard only newly imported files on cancel, and merge late imports into the latest text. Avatar tests cover failed, cancelled, and superseded copies. Metrics tests also cover duplicate daily check-ins, consecutive weekly goals, future weeks, and daylight-saving transitions. History tests cover inclusive starts and exclusive ends for day, month, and year ranges.
- `app/src/androidTest` contains Compose behavior tests for the three primary tabs, back-to-today navigation, and achievement back handling. It waits for the destination to become visible after each animated tab transition. The achievement regression test opens achievements, switches to Insights, dispatches system back, and checks that the host receives it. Returning to You preserves achievements, where system back still closes them.
- `DraftAndInsightsTest` checks editor content recreation, disabled actions and visible errors during save, missing statistics, the accessible image-preview close action, and bounded composition of a 400-record history in compact and two-pane layouts.
- The Activity recreation regression saves a real Room check-in, opens its editor, changes the note without saving, recreates `MainActivity`, and verifies that the open editor and unsaved text survive.

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

When multiple devices are connected, target one explicitly on Linux/macOS:

```bash
ANDROID_SERIAL=emulator-5556 ./gradlew :core:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

For performance checks, use a large history with several check-ins per day.
Navigate between day, month, and year history and edit a record's timestamp.
Confirm that statistics update and that image previews and audio playback stay
with their original record when rows move. Check both themes, English and
Chinese, and compact and large screen layouts.

For draft and attachment smoke tests, start a Today check-in and trigger a
preference or database refresh before saving. The selected mood and optional
energy must remain intact. Edit an existing record, change its note, date,
images, and audio, then rotate the activity: the same ViewModel draft must be
shown. Cancel should remove only new private attachments; originals stay usable.
During a pending save, input, delete, cancel, drag dismissal, and system-back
dismissal must not discard submitted files. A failed save keeps the draft and
shows its error inside the editor so it can be retried.

Import an avatar and rotate while copying. The copy should finish and re-enable
Save. Cancel or replace an import and verify that its result cannot overwrite a
newer draft. Copying images and stopping voice recording must finish before the
form can submit; cancelling either operation must clean undelivered private
files.

Check reminders on Android 30-32 as well as Android 33+: select English or
Chinese, terminate the process, and deliver the daily alarm. The notification
and channel name should use the persisted app language. Selecting System should
use the device language. Boot and time-change rescheduling still reads the saved
reminder setting, and Android 33+ notification permission remains required.

Weekly badge calculation now groups distinct days by week once instead of
filtering the full record list for each week. Repository snapshot conversion
runs on `Dispatchers.Default`; unchanged query results and unrelated preference
changes do not trigger downstream Insights calculations. Snapshot flows still
query Room on collection and retain the existing day/minute refresh behavior.
