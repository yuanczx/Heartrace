package me.heartrace.feature.you

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.heartrace.data.InsightsDataSnapshot
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.feature.metrics.BadgeKind
import me.heartrace.feature.testing.FakeHeartraceDataSource
import me.heartrace.feature.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YouViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun profile_dialog_limits_name_and_persists_profile() = runTest {
        val repository = FakeHeartraceDataSource()
        val viewModel = YouViewModel(repository)
        advanceUntilIdle()
        viewModel.openProfileDialog()
        viewModel.setDraftNickname("x".repeat(40))
        viewModel.setDraftAvatarUri("file://avatar")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(30, repository.preferences.value.nickname.length)
        assertEquals("file://avatar", repository.preferences.value.avatarUri)
        assertFalse(viewModel.uiState.isProfileDialogVisible)
    }

    @Test
    fun reset_and_copying_flags_follow_user_actions() = runTest {
        val repository = FakeHeartraceDataSource()
        val viewModel = YouViewModel(repository)
        viewModel.openResetDialog()
        assertTrue(viewModel.uiState.isResetDialogVisible)
        viewModel.closeResetDialog()
        viewModel.setCopyingAvatar(true)
        assertFalse(viewModel.uiState.isResetDialogVisible)
        assertTrue(viewModel.uiState.isCopyingAvatar)
        viewModel.resetToday()
        advanceUntilIdle()
    }

    @Test
    fun badges_are_derived_for_the_you_page() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            insights.value = InsightsDataSnapshot(
                records = listOf(MoodRecord(1, Mood.BRIGHT, 72, 3, System.currentTimeMillis())),
            )
        }
        val viewModel = YouViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.badges.first { it.kind == BadgeKind.FIRST_RECORD }.earned)
        viewModel.openAchievements()
        assertTrue(viewModel.uiState.isAchievementsVisible)
        viewModel.closeAchievements()
        assertFalse(viewModel.uiState.isAchievementsVisible)
    }
}
