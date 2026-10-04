package me.heartrace.feature.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import me.heartrace.data.AppLanguage
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.InsightsDataSnapshot
import me.heartrace.data.Mood
import me.heartrace.data.PreferencesSnapshot
import me.heartrace.data.TodayDataSnapshot

class FakeHeartraceDataSource : HeartraceDataSource {
    val preferences = MutableStateFlow(PreferencesSnapshot())
    val today = MutableStateFlow(TodayDataSnapshot())
    val insights = MutableStateFlow(InsightsDataSnapshot())
    val savedCheckIns = mutableListOf<CheckInCall>()
    val savedMoments = mutableListOf<MomentCall>()
    val savedNotes = mutableListOf<NoteCall>()
    val updatedRecords = mutableListOf<UpdateCall>()
    val deletedRecordIds = mutableListOf<Long>()
    var nextId = 1L
    var saveCheckInError: Throwable? = null
    var saveMomentError: Throwable? = null
    var saveNoteError: Throwable? = null
    var updateCheckInError: Throwable? = null
    var deleteCheckInError: Throwable? = null

    data class CheckInCall(val moodValue: Int, val energyLevel: Int?, val momentText: String? = null)

    data class MomentCall(
        val id: Long,
        val text: String,
        val imageUris: List<String>,
        val audioUri: String?,
        val audioDurationMs: Long,
    )

    data class NoteCall(
        val text: String,
        val mood: Mood,
        val recordedAt: Long,
        val imageUris: List<String>,
        val audioUri: String?,
        val audioDurationMs: Long,
    )

    data class UpdateCall(
        val id: Long,
        val moodValue: Int,
        val energyLevel: Int?,
        val momentText: String?,
        val recordedAt: Long,
    )

    override fun observePreferences(): Flow<PreferencesSnapshot> = preferences

    override fun observeToday(): Flow<TodayDataSnapshot> = today

    override suspend fun loadTodaySnapshot(): TodayDataSnapshot = today.value

    override fun observeInsights(): Flow<InsightsDataSnapshot> = insights

    override suspend fun ensureSeedData() = Unit

    override suspend fun saveNote(
        text: String,
        mood: Mood,
        recordedAt: Long,
        imageUris: List<String>,
        audioUri: String?,
        audioDurationMs: Long,
    ) {
        saveNoteError?.let { throw it }
        savedNotes += NoteCall(text, mood, recordedAt, imageUris, audioUri, audioDurationMs)
    }

    override suspend fun saveCheckIn(
        moodValue: Int,
        energyLevel: Int?,
        recordedAt: Long,
        momentText: String?,
        imageUris: List<String>,
        audioUri: String?,
        audioDurationMs: Long,
    ): Long {
        saveCheckInError?.let { throw it }
        savedCheckIns += CheckInCall(moodValue, energyLevel, momentText)
        return nextId++
    }

    override suspend fun saveMoment(
        checkInId: Long,
        text: String,
        recordedAt: Long,
        imageUris: List<String>,
        audioUri: String?,
        audioDurationMs: Long,
    ) {
        saveMomentError?.let { throw it }
        savedMoments += MomentCall(checkInId, text, imageUris, audioUri, audioDurationMs)
    }

    override suspend fun updateCheckIn(
        id: Long,
        moodValue: Int,
        energyLevel: Int?,
        momentText: String?,
        recordedAt: Long,
        imageUris: List<String>,
        audioUri: String?,
        audioDurationMs: Long,
    ) {
        updateCheckInError?.let { throw it }
        updatedRecords += UpdateCall(id, moodValue, energyLevel, momentText, recordedAt)
    }

    override suspend fun deleteCheckIn(id: Long) {
        deleteCheckInError?.let { throw it }
        deletedRecordIds += id
    }

    override suspend fun setLanguage(language: AppLanguage) {
        preferences.value = preferences.value.copy(language = language)
    }

    override suspend fun setLightTheme(light: Boolean) {
        preferences.value = preferences.value.copy(lightTheme = light)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        preferences.value = preferences.value.copy(remindersEnabled = enabled)
    }

    override suspend fun setWeeklyGoalDays(days: Int) {
        preferences.value = preferences.value.copy(weeklyGoalDays = days)
        insights.value = insights.value.copy(weeklyGoalDays = days)
    }

    override suspend fun setProfile(nickname: String, avatarUri: String?) {
        preferences.value = preferences.value.copy(nickname = nickname, avatarUri = avatarUri)
    }

    override suspend fun resetToday() = Unit
}
