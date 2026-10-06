package me.heartrace.feature.today

import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.feature.metrics.WeeklyGoalProgress

data class TodayUiState(
    val selectedMood: Mood = Mood.STEADY,
    val moodValue: Int = 55,
    val energyLevel: Int? = null,
    val todayMoodRecords: List<MoodRecord> = emptyList(),
    val todayNotesCount: Int = 0,
    val isSaving: Boolean = false,
    val isUpdatingRecord: Boolean = false,
    val errorMessage: String? = null,
    val editingRecord: MoodRecord? = null,
    val editingDraft: EditRecordDraft? = null,
    val recordErrorMessage: String? = null,
    val isDetailsExpanded: Boolean = false,
    val momentDraft: String = "",
    val imageUris: List<String> = emptyList(),
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
    val recordedAt: Long? = null,
    val savedCount: Int = 0,
    val weeklyGoal: WeeklyGoalProgress = WeeklyGoalProgress(5, 0, 5, false),
)
