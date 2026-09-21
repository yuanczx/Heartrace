package me.heartrace.feature.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.Mood
import javax.inject.Inject

data class NoteComposerUiState(
    val draft: String = "",
    val mood: Mood = Mood.STEADY,
    val recordedAt: Long = System.currentTimeMillis(),
    val imageUris: List<String> = emptyList(),
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
)

@HiltViewModel
class NoteComposerViewModel
@Inject
constructor(
    private val repository: HeartraceDataSource,
) : ViewModel() {
    var uiState by mutableStateOf(NoteComposerUiState())
        private set

    init {
        viewModelScope.launch {
            uiState = uiState.copy(mood = repository.observeToday().first().mood)
        }
    }

    fun updateDraft(text: String) {
        uiState = uiState.copy(draft = text)
    }

    fun updateMood(mood: Mood) {
        uiState = uiState.copy(mood = mood)
    }

    fun updateRecordedAt(recordedAt: Long) {
        uiState = uiState.copy(recordedAt = recordedAt)
    }

    fun addImages(newUris: List<String>) {
        uiState = uiState.copy(imageUris = uiState.imageUris + newUris)
    }

    fun removeImage(uri: String) {
        uiState = uiState.copy(imageUris = uiState.imageUris.filter { it != uri })
    }

    fun setAudio(uri: String, durationMs: Long) {
        uiState = uiState.copy(audioUri = uri, audioDurationMs = durationMs)
    }

    fun removeAudio() {
        uiState = uiState.copy(audioUri = null, audioDurationMs = 0L)
    }

    fun save(onSuccess: () -> Unit) {
        if (uiState.isSaving) return
        val state = uiState
        uiState = state.copy(isSaving = true, saveError = false)
        viewModelScope.launch {
            runCatching {
                repository.saveNote(
                    text = state.draft.trim(),
                    mood = state.mood,
                    recordedAt = state.recordedAt.coerceAtMost(System.currentTimeMillis()),
                    imageUris = state.imageUris,
                    audioUri = state.audioUri,
                    audioDurationMs = state.audioDurationMs,
                )
            }.onSuccess {
                uiState = uiState.copy(isSaving = false)
                onSuccess()
            }.onFailure {
                uiState = uiState.copy(isSaving = false, saveError = true)
            }
        }
    }
}
