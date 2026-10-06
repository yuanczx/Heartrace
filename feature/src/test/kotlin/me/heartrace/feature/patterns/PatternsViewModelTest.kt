package me.heartrace.feature.patterns

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.heartrace.data.DailyNote
import me.heartrace.data.InsightsDataSnapshot
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.feature.testing.FakeDraftAttachmentStorage
import me.heartrace.feature.testing.FakeHeartraceDataSource
import me.heartrace.feature.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class PatternsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun insights_are_grouped_and_averaged_by_day() = runTest {
        val now = System.currentTimeMillis()
        val repository = FakeHeartraceDataSource().apply {
            insights.value = InsightsDataSnapshot(
                records = listOf(
                    MoodRecord(1, Mood.STEADY, 40, 1, now - 1_000),
                    MoodRecord(2, Mood.BRIGHT, 80, 3, now - 2_000),
                ),
                notes = listOf(DailyNote(3, "", "note", Mood.STEADY, now)),
                recentRecords = listOf(
                    MoodRecord(1, Mood.STEADY, 40, 1, now - 1_000),
                    MoodRecord(2, Mood.BRIGHT, 80, 3, now - 2_000),
                ),
            )
        }
        val viewModel = PatternsViewModel(repository, FakeDraftAttachmentStorage())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.hasHistory)
        assertEquals(60, viewModel.uiState.averageMood)
        assertEquals(2f, viewModel.uiState.averageEnergy)
        assertEquals(1, viewModel.uiState.activeDays)
        assertEquals(1, viewModel.uiState.dailySummaries.size)
        assertEquals(60, viewModel.uiState.dailySummaries.single().averageMood)
    }

    @Test
    fun daily_summaries_keep_mood_trend_when_energy_is_missing() = runTest {
        val now = System.currentTimeMillis()
        val repository = FakeHeartraceDataSource().apply {
            insights.value = InsightsDataSnapshot(
                records = listOf(
                    MoodRecord(1, Mood.QUIET, 40, null, now - 86_400_000L),
                    MoodRecord(2, Mood.BRIGHT, 72, null, now - 1_000L),
                ),
                recentRecords = listOf(
                    MoodRecord(1, Mood.QUIET, 40, null, now - 86_400_000L),
                    MoodRecord(2, Mood.BRIGHT, 72, null, now - 1_000L),
                ),
            )
        }
        val viewModel = PatternsViewModel(repository, FakeDraftAttachmentStorage())
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.dailySummaries.size)
        assertTrue(viewModel.uiState.dailySummaries.all { it.averageEnergy == null })
    }

    @Test
    fun date_and_range_navigation_is_bounded_to_today() = runTest {
        val now = System.currentTimeMillis()
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val repository = FakeHeartraceDataSource().apply {
            insights.value = InsightsDataSnapshot(
                records = listOf(MoodRecord(1, Mood.STEADY, 55, 2, yesterday)),
            )
        }
        val viewModel = PatternsViewModel(repository, FakeDraftAttachmentStorage())
        advanceUntilIdle()
        val initialDate = viewModel.uiState.selectedDateStart
        viewModel.showNextHistoryDate()
        assertTrue(viewModel.uiState.selectedDateStart >= initialDate)
        viewModel.showNextHistoryDate()
        assertFalse(viewModel.uiState.canGoToNextDate)
        viewModel.selectHistoryRange(HistoryRange.MONTH)
        assertEquals(HistoryRange.MONTH, viewModel.uiState.historyRange)
    }

    @Test
    fun history_periods_include_start_and_exclude_next_period() = runTest {
        val januaryStart = Calendar.getInstance().apply {
            clear()
            set(2025, Calendar.JANUARY, 1)
        }.timeInMillis
        val februaryStart = Calendar.getInstance().apply {
            clear()
            set(2025, Calendar.FEBRUARY, 1)
        }.timeInMillis
        val nextYearStart = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1)
        }.timeInMillis
        val records = listOf(januaryStart - 1, januaryStart, februaryStart - 1, februaryStart, nextYearStart)
            .mapIndexed { index, timestamp -> MoodRecord(index.toLong(), Mood.STEADY, 55, null, timestamp) }
        val repository = FakeHeartraceDataSource().apply {
            insights.value = InsightsDataSnapshot(records = records)
        }
        val viewModel = PatternsViewModel(repository, FakeDraftAttachmentStorage())
        advanceUntilIdle()
        viewModel.selectHistoryDate(januaryStart)

        assertEquals(listOf(1L), viewModel.uiState.records.map { it.id })
        viewModel.selectHistoryRange(HistoryRange.MONTH)
        assertEquals(listOf(1L, 2L), viewModel.uiState.records.map { it.id })
        assertEquals(2, viewModel.uiState.recordCount)
        assertEquals(2, viewModel.uiState.activeDays)
        viewModel.selectHistoryRange(HistoryRange.YEAR)
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.records.map { it.id })
        assertEquals(3, viewModel.uiState.recordCount)
    }

    @Test
    fun editing_and_delete_delegate_to_repository() = runTest {
        val repository = FakeHeartraceDataSource()
        val viewModel = PatternsViewModel(repository, FakeDraftAttachmentStorage())
        val record = MoodRecord(9, Mood.LOW, 20, 1, System.currentTimeMillis())
        viewModel.startEditRecord(record)
        assertEquals(record, viewModel.uiState.editingRecord)
        viewModel.updateRecord(9, 70, 3, "updated", 123L)
        advanceUntilIdle()
        assertEquals(9L, repository.updatedRecords.single().id)
        assertEquals(null, viewModel.uiState.editingRecord)
        viewModel.deleteRecord(9)
        advanceUntilIdle()
        assertEquals(listOf(9L), repository.deletedRecordIds)
    }

    @Test
    fun editing_failure_keeps_record_open_and_reports_error() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            updateCheckInError = IllegalStateException()
        }
        val viewModel = PatternsViewModel(repository, FakeDraftAttachmentStorage())
        val record = MoodRecord(9, Mood.LOW, 20, 1, System.currentTimeMillis())
        viewModel.startEditRecord(record)

        viewModel.updateRecord(9, 70, 3, "updated", 123L)
        advanceUntilIdle()

        assertEquals(record, viewModel.uiState.editingRecord)
        assertEquals("save_failed", viewModel.uiState.errorMessage)
    }

    @Test
    fun delete_failure_keeps_record_open_and_reports_error() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            deleteCheckInError = IllegalStateException()
        }
        val viewModel = PatternsViewModel(repository, FakeDraftAttachmentStorage())
        val record = MoodRecord(9, Mood.LOW, 20, 1, System.currentTimeMillis())
        viewModel.startEditRecord(record)

        viewModel.deleteRecord(9)
        advanceUntilIdle()

        assertEquals(record, viewModel.uiState.editingRecord)
        assertEquals("save_failed", viewModel.uiState.errorMessage)
    }
}
