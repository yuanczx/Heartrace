package me.heartrace.feature.you

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.heartrace.data.HeartraceDataSource
import javax.inject.Inject

@HiltViewModel
class YouViewModel
@Inject
constructor(
    private val repository: HeartraceDataSource,
) : ViewModel() {
    var uiState by mutableStateOf(YouUiState())
        private set

    init {
        viewModelScope.launch {
            repository.observePreferences().collect { snapshot ->
                uiState = uiState.copy(
                    nickname = snapshot.nickname,
                    avatarUri = snapshot.avatarUri,
                    draftNickname = if (uiState.isProfileDialogVisible) uiState.draftNickname else snapshot.nickname,
                    draftAvatarUri = if (uiState.isProfileDialogVisible) uiState.draftAvatarUri else snapshot.avatarUri,
                )
            }
        }
    }

    fun openProfileDialog() {
        uiState = uiState.copy(
            isProfileDialogVisible = true,
            draftNickname = uiState.nickname,
            draftAvatarUri = uiState.avatarUri,
        )
    }

    fun closeProfileDialog() {
        uiState = uiState.copy(isProfileDialogVisible = false)
    }

    fun setDraftNickname(nickname: String) {
        uiState = uiState.copy(draftNickname = nickname.take(30))
    }

    fun setDraftAvatarUri(uri: String?) {
        uiState = uiState.copy(draftAvatarUri = uri)
    }

    fun setCopyingAvatar(copying: Boolean) {
        uiState = uiState.copy(isCopyingAvatar = copying)
    }

    fun saveProfile() {
        val current = uiState
        viewModelScope.launch {
            repository.setProfile(current.draftNickname, current.draftAvatarUri)
            uiState = uiState.copy(isProfileDialogVisible = false)
        }
    }

    fun openResetDialog() {
        uiState = uiState.copy(isResetDialogVisible = true)
    }

    fun closeResetDialog() {
        uiState = uiState.copy(isResetDialogVisible = false)
    }

    fun resetToday() {
        viewModelScope.launch { repository.resetToday() }
    }
}
