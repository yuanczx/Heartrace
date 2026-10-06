package me.heartrace.feature.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.data.toMood
import me.heartrace.data.toMoodValue
import me.heartrace.feature.attachments.DraftAttachmentStorage
import me.heartrace.feature.metrics.calculateWeeklyGoalProgress
import javax.inject.Inject

@HiltViewModel
class TodayViewModel
@Inject
constructor(
    private val repository: HeartraceDataSource,
    private val attachments: DraftAttachmentStorage,
) : ViewModel() {
    var uiState by mutableStateOf(TodayUiState())
        private set

    private var hasBeenCleared = false

    private var hasObservedTodayRecords = false
    private var hasDraftChanges = false

    init {
        viewModelScope.launch {
            repository.observeToday().collect { snapshot ->
                val current = uiState
                val recordsCleared = hasObservedTodayRecords && snapshot.todayMoodRecords.isEmpty()
                hasObservedTodayRecords = snapshot.todayMoodRecords.isNotEmpty()
                val shouldSyncSelection = !current.isSaving && !hasDraftChanges &&
                    current.errorMessage == null && (recordsCleared || current.savedCount == 0)
                uiState = current.copy(
                    todayMoodRecords = snapshot.todayMoodRecords,
                    todayNotesCount = snapshot.todayNotes.size,
                    selectedMood = if (shouldSyncSelection) snapshot.moodValue.toMood() else current.selectedMood,
                    moodValue = if (shouldSyncSelection) snapshot.moodValue else current.moodValue,
                    // Energy is optional per check-in; a previous record must not prefill the next one.
                    energyLevel = if (shouldSyncSelection) null else current.energyLevel,
                    savedCount = if (recordsCleared) 0 else current.savedCount,
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
        if (uiState.isSaving) return
        hasDraftChanges = true
        val moodValue = value.toInt().coerceIn(0, 100)
        uiState = uiState.copy(moodValue = moodValue, selectedMood = moodValue.toMood())
    }

    fun selectMood(mood: Mood) {
        if (uiState.isSaving) return
        hasDraftChanges = true
        uiState = uiState.copy(selectedMood = mood, moodValue = mood.toMoodValue())
    }

    fun setEnergyLevel(value: Int?) {
        if (uiState.isSaving) return
        hasDraftChanges = true
        uiState = uiState.copy(energyLevel = value?.coerceIn(1, 3))
    }

    fun setDetailsExpanded(expanded: Boolean) {
        if (!uiState.isSaving) uiState = uiState.copy(isDetailsExpanded = expanded)
    }

    fun setMomentDraft(text: String) {
        if (uiState.isSaving) return
        hasDraftChanges = true
        uiState = uiState.copy(momentDraft = text)
    }

    fun addImages(uris: List<String>) {
        if (uiState.isSaving) {
            (uris - uiState.imageUris.toSet()).forEach(attachments::delete)
            return
        }
        hasDraftChanges = true
        uiState = uiState.copy(imageUris = uiState.imageUris + uris)
    }

    fun removeImage(uri: String) {
        if (uiState.isSaving || uri !in uiState.imageUris) return
        attachments.delete(uri)
        hasDraftChanges = true
        uiState = uiState.copy(imageUris = uiState.imageUris - uri)
    }

    fun setAudio(uri: String?, durationMs: Long = 0L) {
        if (uiState.isSaving) {
            if (uri != uiState.audioUri) attachments.delete(uri)
            return
        }
        if (uri != uiState.audioUri) attachments.delete(uiState.audioUri)
        hasDraftChanges = true
        uiState = uiState.copy(audioUri = uri, audioDurationMs = durationMs)
    }

    fun setRecordedAt(timestamp: Long?) {
        if (uiState.isSaving) return
        hasDraftChanges = true
        uiState = uiState.copy(recordedAt = timestamp)
    }

    fun saveCheckIn(momentText: String? = null) {
        if (uiState.isSaving) return
        val state = uiState
        val savedMomentText = momentText ?: state.momentDraft.takeIf { it.isNotBlank() }
        uiState = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
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
                    hasDraftChanges = false
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (hasBeenCleared) {
                        state.imageUris.forEach(attachments::delete)
                        attachments.delete(state.audioUri)
                    }
                    uiState = uiState.copy(isSaving = false, errorMessage = "save_failed")
                }
            }
        }
    }

    fun startEditRecord(record: MoodRecord) {
        if (uiState.isUpdatingRecord) return
        uiState.editingDraft?.discardAttachments(attachments)
        uiState = uiState.copy(editingRecord = record, editingDraft = EditRecordDraft(record), recordErrorMessage = null)
    }

    fun updateEditDraft(record: MoodRecord, transform: (EditRecordDraft) -> EditRecordDraft) {
        val current = uiState.editingDraft
        val draft = transform(current?.takeIf { it.record.id == record.id } ?: EditRecordDraft(record))
        if (uiState.isUpdatingRecord || current?.record?.id != record.id) {
            (draft.newAttachments - current?.newAttachments.orEmpty()).forEach(attachments::delete)
            return
        }
        current.discardRemovedAttachments(draft, attachments)
        uiState = uiState.copy(editingDraft = draft)
    }

    fun dismissEditRecord() {
        if (uiState.isUpdatingRecord) return
        uiState.editingDraft?.discardAttachments(attachments)
        clearRecordEditor()
    }

    private fun clearRecordEditor() {
        uiState = uiState.copy(isUpdatingRecord = false, editingRecord = null, editingDraft = null, recordErrorMessage = null)
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
        uiState = uiState.copy(isUpdatingRecord = true, recordErrorMessage = null)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                try {
                    repository.updateCheckIn(id, moodValue, energyLevel, momentText, recordedAt, imageUris, audioUri, audioDurationMs)
                    clearRecordEditor()
                    refreshTodaySnapshot()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (hasBeenCleared) uiState.editingDraft?.discardAttachments(attachments)
                    uiState = uiState.copy(isUpdatingRecord = false, recordErrorMessage = "save_failed")
                }
            }
        }
    }

    fun deleteRecord(id: Long) {
        if (uiState.isUpdatingRecord) return
        uiState = uiState.copy(isUpdatingRecord = true, recordErrorMessage = null)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                try {
                    repository.deleteCheckIn(id)
                    uiState.editingDraft?.discardAttachments(attachments)
                    clearRecordEditor()
                    refreshTodaySnapshot()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (hasBeenCleared) uiState.editingDraft?.discardAttachments(attachments)
                    uiState = uiState.copy(isUpdatingRecord = false, recordErrorMessage = "save_failed")
                }
            }
        }
    }

    override fun onCleared() {
        hasBeenCleared = true
        if (!uiState.isSaving) {
            uiState.imageUris.forEach(attachments::delete)
            attachments.delete(uiState.audioUri)
        }
        if (!uiState.isUpdatingRecord) uiState.editingDraft?.discardAttachments(attachments)
        super.onCleared()
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
