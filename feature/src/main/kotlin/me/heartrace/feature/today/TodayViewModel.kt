package me.heartrace.feature.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.data.toMood
import me.heartrace.data.toMoodValue
import me.heartrace.feature.patterns.calculateWeeklyGoalProgress
import javax.inject.Inject

@HiltViewModel
class TodayViewModel
@Inject
constructor(
    private val repository: HeartraceDataSource,
) : ViewModel() {
    var uiState by mutableStateOf(TodayUiState())
        private set

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            repository.observeToday().collect { snapshot ->
                val current = uiState
                val shouldSyncSelection = current.errorMessage == null && !current.isSaving && current.savedCount == 0 && current.momentDraft.isBlank() && current.imageUris.isEmpty()
                uiState = current.copy(
                    todayMoodRecords = snapshot.todayMoodRecords,
                    todayNotesCount = snapshot.todayNotes.size,
                    selectedMood = if (shouldSyncSelection) snapshot.moodValue.toMood() else current.selectedMood,
                    moodValue = if (shouldSyncSelection) snapshot.moodValue else current.moodValue,
                    // Energy is optional per check-in; a previous record must not prefill the next one.
                    energyLevel = if (shouldSyncSelection) null else current.energyLevel,
                )
            }
        }
        viewModelScope.launch {
            repository.observeInsights().collect { snapshot ->
                uiState = uiState.copy(weeklyGoal = calculateWeeklyGoalProgress(snapshot.records, System.currentTimeMillis(), snapshot.weeklyGoalDays))
            }
        }
    }

    fun setMoodValue(value: Float) {
        val moodValue = value.toInt().coerceIn(0, 100)
        uiState = uiState.copy(moodValue = moodValue, selectedMood = moodValue.toMood())
    }

    fun selectMood(mood: Mood) {
        if (!uiState.isSaving) uiState = uiState.copy(selectedMood = mood, moodValue = mood.toMoodValue())
    }

    fun setEnergyLevel(value: Int?) {
        uiState = uiState.copy(energyLevel = value?.coerceIn(1, 3))
    }

    fun setDetailsExpanded(expanded: Boolean) {
        uiState = uiState.copy(isDetailsExpanded = expanded)
    }

    fun setMomentDraft(text: String) {
        uiState = uiState.copy(momentDraft = text)
    }

    fun addImages(uris: List<String>) {
        uiState = uiState.copy(imageUris = uiState.imageUris + uris)
    }

    fun removeImage(uri: String) {
        uiState = uiState.copy(imageUris = uiState.imageUris - uri)
    }

    fun setAudio(uri: String?, durationMs: Long = 0L) {
        uiState = uiState.copy(audioUri = uri, audioDurationMs = durationMs)
    }

    fun setRecordedAt(timestamp: Long?) {
        uiState = uiState.copy(recordedAt = timestamp)
    }

    fun saveCheckIn(momentText: String? = null) {
        if (uiState.isSaving) return
        val state = uiState
        val savedMomentText = momentText ?: state.momentDraft.takeIf { it.isNotBlank() }
        uiState = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                repository.saveCheckIn(
                    moodValue = state.moodValue,
                    energyLevel = state.energyLevel,
                    recordedAt = state.recordedAt ?: System.currentTimeMillis(),
                    momentText = savedMomentText,
                    imageUris = state.imageUris,
                    audioUri = state.audioUri,
                    audioDurationMs = state.audioDurationMs,
                )
                uiState = uiState.copy(
                    isSaving = false,
                    momentDraft = "",
                    energyLevel = null,
                    imageUris = emptyList(),
                    audioUri = null,
                    audioDurationMs = 0L,
                    recordedAt = null,
                    isDetailsExpanded = false,
                    savedCount = uiState.savedCount + 1,
                )
                refreshTodaySnapshot()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(isSaving = false, errorMessage = "save_failed")
            }
        }
    }

    fun startEditRecord(record: MoodRecord) {
        uiState = uiState.copy(editingRecord = record)
    }

    fun dismissEditRecord() {
        if (uiState.isUpdatingRecord) return
        uiState = uiState.copy(editingRecord = null)
    }

    fun updateRecord(
        id: Long,
        moodValue: Int,
        energyLevel: Int?,
        momentText: String?,
        recordedAt: Long,
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    ) {
        if (uiState.isUpdatingRecord) return
        uiState = uiState.copy(isUpdatingRecord = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                repository.updateCheckIn(
                    id = id,
                    moodValue = moodValue,
                    energyLevel = energyLevel,
                    momentText = momentText,
                    recordedAt = recordedAt,
                    imageUris = imageUris,
                    audioUri = audioUri,
                    audioDurationMs = audioDurationMs,
                )
            }
                .onSuccess { uiState = uiState.copy(isUpdatingRecord = false, editingRecord = null) }
                .onFailure { uiState = uiState.copy(isUpdatingRecord = false, errorMessage = "save_failed") }
        }
    }

    fun deleteRecord(id: Long) {
        if (uiState.isUpdatingRecord) return
        uiState = uiState.copy(isUpdatingRecord = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { repository.deleteCheckIn(id) }
                .onSuccess { uiState = uiState.copy(isUpdatingRecord = false, editingRecord = null) }
                .onFailure { uiState = uiState.copy(isUpdatingRecord = false, errorMessage = "save_failed") }
        }
    }

    private fun refreshTodaySnapshot() {
        viewModelScope.launch {
            runCatching { repository.loadTodaySnapshot() }
                .onSuccess { snapshot ->
                    uiState = uiState.copy(
                        todayMoodRecords = snapshot.todayMoodRecords,
                        todayNotesCount = snapshot.todayNotes.size,
                    )
                }
        }
    }
}
