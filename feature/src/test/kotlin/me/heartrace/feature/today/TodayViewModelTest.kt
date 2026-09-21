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
import org.junit.Assert.assertNull
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
        viewModel.setMoodValue(80f)
        viewModel.saveCheckIn()
        advanceUntilIdle()

        assertEquals(listOf(FakeHeartraceDataSource.CheckInCall(80, 2)), repository.savedCheckIns)
        assertEquals(1L, viewModel.uiState.pendingMomentForId)
        assertEquals(1, viewModel.uiState.momentPromptDirection)
        assertEquals(false, viewModel.uiState.isSaving)
    }

    @Test
    fun saveMoment_and_dismiss_clear_pending_prompt() = runTest {
        val repository = FakeHeartraceDataSource()
        val viewModel = TodayViewModel(repository)
        viewModel.saveCheckIn()
        advanceUntilIdle()

        viewModel.saveMoment("  a useful note  ", listOf("file://image"), "file://audio", 1200L)
        advanceUntilIdle()

        assertEquals("  a useful note  ", repository.savedMoments.single().text)
        assertNull(viewModel.uiState.pendingMomentForId)
        viewModel.saveCheckIn()
        advanceUntilIdle()
        viewModel.dismissMoment()
        assertNull(viewModel.uiState.pendingMomentForId)
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
}
