package me.heartrace.data

import kotlinx.coroutines.flow.Flow

/** The persistence contract consumed by feature ViewModels. */
interface HeartraceDataSource {
    fun observePreferences(): Flow<PreferencesSnapshot>

    fun observeToday(): Flow<TodayDataSnapshot>

    suspend fun loadTodaySnapshot(): TodayDataSnapshot

    fun observeInsights(): Flow<InsightsDataSnapshot>

    suspend fun ensureSeedData()

    suspend fun saveNote(
        text: String,
        mood: Mood,
        recordedAt: Long = System.currentTimeMillis(),
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    )

    suspend fun saveCheckIn(
        moodValue: Int,
        energyLevel: Int?,
        recordedAt: Long = System.currentTimeMillis(),
        momentText: String? = null,
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    ): Long

    suspend fun saveMoment(
        checkInId: Long,
        text: String,
        recordedAt: Long = System.currentTimeMillis(),
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    )

    suspend fun updateCheckIn(
        id: Long,
        moodValue: Int,
        energyLevel: Int?,
        momentText: String?,
        recordedAt: Long = System.currentTimeMillis(),
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    )

    suspend fun deleteCheckIn(id: Long)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setLightTheme(light: Boolean)

    suspend fun setRemindersEnabled(enabled: Boolean)

    suspend fun setWeeklyGoalDays(days: Int)

    suspend fun setProfile(nickname: String, avatarUri: String?)

    suspend fun resetToday()
}
