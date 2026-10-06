package me.heartrace.feature.you

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.heartrace.data.HeartraceDataSource
import me.heartrace.feature.metrics.BadgeKind
import me.heartrace.feature.metrics.calculateBadges
import javax.inject.Inject

@HiltViewModel
class YouViewModel
@Inject
constructor(
    private val repository: HeartraceDataSource,
) : ViewModel() {
    var uiState by mutableStateOf(YouUiState())
        private set

    private var hasObservedInitialBadges = false
    private var previouslyEarnedBadges: Set<BadgeKind> = emptySet()

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
        viewModelScope.launch {
            repository.observeInsights().collect { snapshot ->
                val badges = calculateBadges(snapshot.records, System.currentTimeMillis(), snapshot.weeklyGoalDays)
                val earnedBadges = badges.filter { it.earned }.map { it.kind }.toSet()
                val newlyEarnedBadges = if (hasObservedInitialBadges) {
                    (earnedBadges - previouslyEarnedBadges).toList()
                } else {
                    emptyList()
                }
                previouslyEarnedBadges = earnedBadges
                hasObservedInitialBadges = true
                uiState = uiState.copy(
                    badges = badges,
                    badgeCelebration = newlyEarnedBadges.ifEmpty { uiState.badgeCelebration },
                )
            }
        }
    }

    fun dismissBadgeCelebration() {
        uiState = uiState.copy(badgeCelebration = emptyList())
    }

    fun openAchievements() {
        uiState = uiState.copy(isAchievementsVisible = true)
    }

    fun closeAchievements() {
        uiState = uiState.copy(isAchievementsVisible = false)
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
