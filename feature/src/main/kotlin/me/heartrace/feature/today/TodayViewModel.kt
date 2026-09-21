package me.heartrace.feature.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.MoodRecord
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
                uiState = current.copy(
                    moodValue = if (current.isSaving) current.moodValue else snapshot.moodValue,
                    energyLevel = if (current.isSaving) current.energyLevel else snapshot.energyLevel,
                    todayMoodRecords = snapshot.todayMoodRecords,
                    todayNotesCount = snapshot.todayNotes.size,
                    errorMessage = null,
                )
            }
        }
    }

    fun setMoodValue(value: Float) {
        uiState = uiState.copy(moodValue = value.toInt().coerceIn(0, 100))
    }

    fun setEnergyLevel(value: Int) {
        uiState = uiState.copy(energyLevel = value.coerceIn(1, 3))
    }

    fun saveCheckIn() {
        if (uiState.isSaving) return
        val state = uiState
        val previous = state.todayMoodRecords.firstOrNull()?.moodValue ?: state.moodValue
        uiState = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { repository.saveCheckIn(state.moodValue, state.energyLevel) }
                .onSuccess { id ->
                    val moodDelta = state.moodValue - previous
                    uiState = uiState.copy(
                        pendingMomentForId = id,
                        momentPromptDirection = when {
                            moodDelta > 8 -> 1
                            moodDelta < -8 -> -1
                            else -> 0
                        },
                        isSaving = false,
                    )
                    refreshTodaySnapshot()
                }
                .onFailure {
                    uiState = uiState.copy(isSaving = false, errorMessage = "save_failed")
                }
        }
    }

    fun saveMoment(
        text: String,
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    ) {
        val id = uiState.pendingMomentForId ?: return
        if (uiState.isSavingMoment) return
        uiState = uiState.copy(isSavingMoment = true)
        viewModelScope.launch {
            runCatching {
                repository.saveMoment(
                    checkInId = id,
                    text = text,
                    imageUris = imageUris,
                    audioUri = audioUri,
                    audioDurationMs = audioDurationMs,
                )
            }
                .onSuccess {
                    uiState = uiState.copy(pendingMomentForId = null, isSavingMoment = false)
                    refreshTodaySnapshot()
                }
                .onFailure { uiState = uiState.copy(isSavingMoment = false, errorMessage = "save_failed") }
        }
    }

    fun dismissMoment() {
        if (!uiState.isSavingMoment) {
            uiState = uiState.copy(pendingMomentForId = null, errorMessage = null)
        }
    }

    fun startEditRecord(record: MoodRecord) {
        uiState = uiState.copy(editingRecord = record)
    }

    fun dismissEditRecord() {
        uiState = uiState.copy(editingRecord = null)
    }

    fun updateRecord(
        id: Long,
        moodValue: Int,
        energyLevel: Int,
        momentText: String?,
        recordedAt: Long,
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    ) {
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
                .onSuccess { dismissEditRecord() }
                .onFailure { uiState = uiState.copy(errorMessage = "save_failed") }
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteCheckIn(id) }
                .onSuccess { dismissEditRecord() }
                .onFailure { uiState = uiState.copy(errorMessage = "save_failed") }
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
