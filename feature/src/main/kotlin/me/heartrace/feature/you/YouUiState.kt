package me.heartrace.feature.you

import me.heartrace.feature.metrics.BadgeKind
import me.heartrace.feature.metrics.BadgeStatus

data class YouUiState(
    val isResetDialogVisible: Boolean = false,
    val isProfileDialogVisible: Boolean = false,
    val nickname: String = "",
    val avatarUri: String? = null,
    val draftNickname: String = "",
    val draftAvatarUri: String? = null,
    val isCopyingAvatar: Boolean = false,
    val badges: List<BadgeStatus> = emptyList(),
    val badgeCelebration: List<BadgeKind> = emptyList(),
    val isAchievementsVisible: Boolean = false,
)
