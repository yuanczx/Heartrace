package me.heartrace.feature.today

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.data.TodayDataSnapshot
import me.heartrace.feature.testing.FakeHeartraceDataSource
import me.heartrace.feature.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sliders_are_bounded() = runTest {
        val repository = FakeHeartraceDataSource()
        val viewModel = TodayViewModel(repository)

        viewModel.setMoodValue(-20f)
        viewModel.setEnergyLevel(8)

        assertEquals(0, viewModel.uiState.moodValue)
        assertEquals(3, viewModel.uiState.energyLevel)
    }

    @Test
    fun saveCheckIn_sets_prompt_direction_and_pending_id() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            today.value = TodayDataSnapshot(
                moodValue = 40,
                todayMoodRecords = listOf(MoodRecord(7, Mood.QUIET, 40, 2, 1L)),
            )
        }
        val viewModel = TodayViewModel(repository)
        advanceUntilIdle()
        viewModel.selectMood(Mood.BRIGHT)
        viewModel.saveCheckIn(momentText = "  a note  ")
        advanceUntilIdle()

        assertEquals(listOf(FakeHeartraceDataSource.CheckInCall(72, null, "  a note  ")), repository.savedCheckIns)
        assertEquals(1, viewModel.uiState.savedCount)
        assertEquals(false, viewModel.uiState.isSaving)
    }

    @Test
    fun existing_today_energy_does_not_prefill_next_check_in() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            today.value = TodayDataSnapshot(
                moodValue = 72,
                energyLevel = 3,
                todayMoodRecords = listOf(MoodRecord(8, Mood.BRIGHT, 72, 3, 1L)),
            )
        }
        val viewModel = TodayViewModel(repository)
        advanceUntilIdle()

        viewModel.saveCheckIn()
        advanceUntilIdle()

        assertEquals(null, repository.savedCheckIns.single().energyLevel)
    }

    @Test
    fun clearing_today_records_resets_the_next_check_in_selection() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            today.value = TodayDataSnapshot(
                moodValue = 72,
                todayMoodRecords = listOf(MoodRecord(8, Mood.BRIGHT, 72, 3, 1L)),
            )
        }
        val viewModel = TodayViewModel(repository)
        advanceUntilIdle()
        viewModel.selectMood(Mood.ELECTRIC)

        repository.today.value = TodayDataSnapshot(moodValue = 25)
        advanceUntilIdle()

        assertEquals(Mood.LOW, viewModel.uiState.selectedMood)
        assertEquals(25, viewModel.uiState.moodValue)
        assertEquals(0, viewModel.uiState.savedCount)
    }

    @Test
    fun saveMoment_and_dismiss_clear_pending_prompt() = runTest {
        val repository = FakeHeartraceDataSource()
        val viewModel = TodayViewModel(repository)
        viewModel.saveCheckIn()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.savedCount)
    }

    @Test
    fun repository_failure_is_exposed_and_does_not_leave_saving_state() = runTest {
        val repository = FakeHeartraceDataSource().apply { saveCheckInError = IllegalStateException() }
        val viewModel = TodayViewModel(repository)
        viewModel.saveCheckIn()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.isSaving)
        assertEquals("save_failed", viewModel.uiState.errorMessage)
    }

    @Test
    fun snapshot_refresh_preserves_selected_mood_and_failed_draft() = runTest {
        val repository = FakeHeartraceDataSource().apply { saveCheckInError = IllegalStateException() }
        val viewModel = TodayViewModel(repository)
        advanceUntilIdle()
        viewModel.selectMood(Mood.RESTLESS)
        viewModel.setMomentDraft("keep me")
        viewModel.saveCheckIn()
        advanceUntilIdle()
        repository.today.value = TodayDataSnapshot(todayNotes = listOf(me.heartrace.data.DailyNote(1, "", "other", Mood.STEADY)))
        advanceUntilIdle()
        assertEquals(Mood.RESTLESS, viewModel.uiState.selectedMood)
        assertEquals("keep me", viewModel.uiState.momentDraft)
        assertEquals("save_failed", viewModel.uiState.errorMessage)
    }
}
