package me.heartrace.feature.today

import me.heartrace.data.MoodRecord

data class TodayUiState(
    val moodValue: Int = 50,
    val energyLevel: Int = 2,
    val todayMoodRecords: List<MoodRecord> = emptyList(),
    val todayNotesCount: Int = 0,
    val pendingMomentForId: Long? = null,
    val momentPromptDirection: Int = 0,
    val isSaving: Boolean = false,
    val isSavingMoment: Boolean = false,
    val errorMessage: String? = null,
    val editingRecord: MoodRecord? = null,
)
