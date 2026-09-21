package me.heartrace.feature.you

data class YouUiState(
    val isResetDialogVisible: Boolean = false,
    val isProfileDialogVisible: Boolean = false,
    val nickname: String = "",
    val avatarUri: String? = null,
    val draftNickname: String = "",
    val draftAvatarUri: String? = null,
    val isCopyingAvatar: Boolean = false,
)
