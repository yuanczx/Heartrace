package me.heartrace.feature.patterns

import me.heartrace.data.DailyNote
import me.heartrace.data.MoodRecord
import me.heartrace.feature.metrics.WeeklyGoalProgress
import me.heartrace.feature.today.EditRecordDraft

data class DailyRecordSummary(
    val dayStart: Long,
    val dateLabel: String,
    val averageMood: Int,
    val averageEnergy: Float?,
)

enum class HistoryRange {
    DAY,
    MONTH,
    YEAR,
}

data class PatternsUiState(
    val records: List<MoodRecord> = emptyList(),
    val hasHistory: Boolean = false,
    val notes: List<DailyNote> = emptyList(),
    val dailySummaries: List<DailyRecordSummary> = emptyList(),
    val activeDays: Int = 0,
    val averageEnergy: Float? = null,
    val averageMood: Int? = null,
    val recordCount: Int = 0,
    val weeklyGoal: WeeklyGoalProgress = WeeklyGoalProgress(5, 0, 5, false),
    val currentStreak: Int = 0,
    val selectedDateStart: Long = 0L,
    val historyRange: HistoryRange = HistoryRange.DAY,
    val canGoToNextDate: Boolean = false,
    val editingRecord: MoodRecord? = null,
    val editingDraft: EditRecordDraft? = null,
    val isUpdatingRecord: Boolean = false,
    val errorMessage: String? = null,
)
