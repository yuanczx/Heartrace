package me.heartrace.feature.you

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.heartrace.data.InsightsDataSnapshot
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.feature.metrics.BadgeKind
import me.heartrace.feature.testing.FakeDraftAttachmentStorage
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
        val viewModel = YouViewModel(repository, FakeDraftAttachmentStorage())
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
    fun reset_dialog_and_reset_follow_user_actions() = runTest {
        val repository = FakeHeartraceDataSource()
        val viewModel = YouViewModel(repository, FakeDraftAttachmentStorage())
        viewModel.openResetDialog()
        assertTrue(viewModel.uiState.isResetDialogVisible)
        viewModel.closeResetDialog()
        assertFalse(viewModel.uiState.isResetDialogVisible)
        viewModel.resetToday()
        advanceUntilIdle()
        assertEquals(1, repository.resetCount)
    }

    @Test
    fun badges_are_derived_for_the_you_page() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            insights.value = InsightsDataSnapshot(
                records = listOf(MoodRecord(1, Mood.BRIGHT, 72, 3, System.currentTimeMillis())),
            )
        }
        val viewModel = YouViewModel(repository, FakeDraftAttachmentStorage())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.badges.first { it.kind == BadgeKind.FIRST_RECORD }.earned)
        viewModel.openAchievements()
        assertTrue(viewModel.uiState.isAchievementsVisible)
        viewModel.closeAchievements()
        assertFalse(viewModel.uiState.isAchievementsVisible)
    }

    @Test
    fun failed_avatar_import_resets_loading_and_keeps_previous_draft() = runTest {
        val storage = FakeDraftAttachmentStorage().apply { copyError = IllegalStateException("Unavailable") }
        val viewModel = YouViewModel(FakeHeartraceDataSource(), storage)
        advanceUntilIdle()
        viewModel.openProfileDialog()
        viewModel.setDraftAvatarUri("file://draft-avatar")
        viewModel.copyAvatar("content://image")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isCopyingAvatar)
        assertEquals("avatar_copy_failed", viewModel.uiState.profileErrorMessage)
        assertEquals("file://draft-avatar", viewModel.uiState.draftAvatarUri)
        assertTrue(storage.deletedUris.isEmpty())
        storage.copyError = null
        viewModel.copyAvatar("content://another-image")
        advanceUntilIdle()
        assertEquals("file://new-avatar", viewModel.uiState.draftAvatarUri)
        assertEquals(null, viewModel.uiState.profileErrorMessage)
        assertEquals(listOf("file://draft-avatar"), storage.deletedUris)
    }

    @Test
    fun closing_during_avatar_import_cancels_it_and_allows_reopening() = runTest {
        val gate = CompletableDeferred<Unit>()
        val storage = FakeDraftAttachmentStorage().apply { copyGate = gate }
        val viewModel = YouViewModel(FakeHeartraceDataSource(), storage)
        advanceUntilIdle()
        viewModel.openProfileDialog()
        viewModel.copyAvatar("content://image")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.isCopyingAvatar)
        viewModel.closeProfileDialog()
        viewModel.openProfileDialog()
        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isCopyingAvatar)
        assertEquals(null, viewModel.uiState.draftAvatarUri)
        viewModel.copyAvatar("content://another-image")
        advanceUntilIdle()
        assertEquals("file://new-avatar", viewModel.uiState.draftAvatarUri)
        assertFalse(viewModel.uiState.isCopyingAvatar)
    }

    @Test
    fun newer_avatar_import_wins_over_cancelled_import() = runTest {
        val gate = CompletableDeferred<Unit>()
        val storage = FakeDraftAttachmentStorage().apply {
            copyGate = gate
            copiedUri = "file://old-import"
        }
        val viewModel = YouViewModel(FakeHeartraceDataSource(), storage)
        advanceUntilIdle()
        viewModel.openProfileDialog()
        viewModel.copyAvatar("content://old-image")
        advanceUntilIdle()
        storage.copyGate = null
        storage.copiedUri = "file://new-import"
        viewModel.copyAvatar("content://new-image")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("file://new-import", viewModel.uiState.draftAvatarUri)
        assertFalse(viewModel.uiState.isCopyingAvatar)
    }

    @Test
    fun pending_profile_save_cannot_discard_or_replace_the_submitted_avatar() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeHeartraceDataSource().apply { setProfileGate = gate }
        val storage = FakeDraftAttachmentStorage()
        val viewModel = YouViewModel(repository, storage)
        advanceUntilIdle()
        viewModel.openProfileDialog()
        viewModel.setDraftNickname("Submitted name")
        viewModel.setDraftAvatarUri("file://submitted-avatar")
        viewModel.saveProfile()
        advanceUntilIdle()
        val submitted = viewModel.uiState
        viewModel.closeProfileDialog()
        viewModel.setDraftNickname("Changed name")
        viewModel.setDraftAvatarUri("file://late-avatar")
        viewModel.copyAvatar("content://late-image")
        viewModel.saveProfile()
        val pending = viewModel.uiState
        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(submitted.isSavingProfile)
        assertEquals(submitted, pending)
        assertEquals(listOf("file://late-avatar"), storage.deletedUris)
        assertEquals("file://submitted-avatar", repository.preferences.value.avatarUri)
        assertEquals("Submitted name", repository.preferences.value.nickname)
        assertFalse(viewModel.uiState.isSavingProfile)
        assertFalse(viewModel.uiState.isProfileDialogVisible)
    }
}
