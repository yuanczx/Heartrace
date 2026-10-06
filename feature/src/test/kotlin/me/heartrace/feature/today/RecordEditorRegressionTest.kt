package me.heartrace.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.feature.patterns.PatternsViewModel
import me.heartrace.feature.testing.FakeDraftAttachmentStorage
import me.heartrace.feature.testing.FakeHeartraceDataSource
import me.heartrace.feature.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class RecordEditorRegressionTest(private val patterns: Boolean) {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val record = MoodRecord(
        1,
        Mood.BRIGHT,
        72,
        2,
        System.currentTimeMillis(),
        imageUris = listOf("file://original-image"),
        audioUri = "file://original-audio",
    )

    @Test
    fun repository_refresh_preserves_draft_and_cancel_deletes_only_new_files() = runTest {
        val repository = FakeHeartraceDataSource()
        val storage = FakeDraftAttachmentStorage()
        val editor = editor(repository, storage)
        editor.open(record)
        editor.update(record) {
            it.copy(momentText = "Unsaved text", recordedAt = record.recordedAt - 1_000, imageUris = it.imageUris + "file://new-image", audioUri = "file://new-audio")
        }
        repository.insights.value = repository.insights.value.copy(weeklyGoalDays = 7)
        advanceUntilIdle()

        assertEquals("Unsaved text", editor.draft()!!.momentText)
        assertEquals(record.recordedAt - 1_000, editor.draft()!!.recordedAt)
        editor.close()
        assertEquals(null, editor.draft())
        assertEquals(setOf("file://new-image", "file://new-audio"), storage.deletedUris.toSet())
    }

    @Test
    fun pending_save_rejects_close_delete_and_attachment_changes() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeHeartraceDataSource().apply { updateCheckInGate = gate }
        val storage = FakeDraftAttachmentStorage()
        val editor = editor(repository, storage)
        editor.open(record)
        editor.update(record) { it.copy(imageUris = it.imageUris + "file://new-image") }
        val submitted = editor.draft()
        editor.save()
        advanceUntilIdle()
        val wasSaving = editor.isSaving()
        editor.close()
        editor.delete()
        editor.update(record) { it.copy(imageUris = emptyList(), momentText = "Changed while saving") }
        repository.insights.value = repository.insights.value.copy(weeklyGoalDays = 3)
        advanceUntilIdle()
        val pending = editor.draft()
        val remainedSaving = editor.isSaving()
        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(wasSaving)
        assertTrue(remainedSaving)
        assertEquals(submitted, pending)
        assertTrue(storage.deletedUris.isEmpty())
        assertTrue(repository.deletedRecordIds.isEmpty())
        assertEquals(submitted!!.imageUris, repository.updatedRecords.single().imageUris)
        assertEquals(null, editor.draft())
        assertFalse(editor.isSaving())
    }

    @Test
    fun failed_save_retains_draft_error_and_attachments_for_retry() = runTest {
        val repository = FakeHeartraceDataSource().apply { updateCheckInError = IllegalStateException("Unavailable") }
        val storage = FakeDraftAttachmentStorage()
        val editor = editor(repository, storage)
        editor.open(record)
        editor.update(record) { it.copy(momentText = "Keep me", imageUris = it.imageUris + "file://new-image") }
        editor.save()
        advanceUntilIdle()

        assertEquals("save_failed", editor.error())
        assertFalse(editor.isSaving())
        assertEquals("Keep me", editor.draft()!!.momentText)
        assertTrue(storage.deletedUris.isEmpty())
        repository.updateCheckInError = null
        editor.save()
        advanceUntilIdle()
        assertEquals(null, editor.draft())
        assertEquals(null, editor.error())
        assertEquals(listOf("file://original-image", "file://new-image"), repository.updatedRecords.single().imageUris)
        assertTrue(storage.deletedUris.isEmpty())
    }

    @Test
    fun failed_delete_keeps_new_attachments_until_user_cancels() = runTest {
        val repository = FakeHeartraceDataSource().apply { deleteCheckInError = IllegalStateException("Unavailable") }
        val storage = FakeDraftAttachmentStorage()
        val editor = editor(repository, storage)
        editor.open(record)
        editor.update(record) { it.copy(imageUris = it.imageUris + "file://new-image") }
        editor.delete()
        advanceUntilIdle()

        assertEquals("save_failed", editor.error())
        assertEquals(listOf("file://original-image", "file://new-image"), editor.draft()!!.imageUris)
        assertTrue(storage.deletedUris.isEmpty())
        editor.close()
        assertEquals(listOf("file://new-image"), storage.deletedUris)
    }

    @Test
    fun removing_original_files_preserves_them_when_edit_is_cancelled() = runTest {
        val storage = FakeDraftAttachmentStorage()
        val editor = editor(FakeHeartraceDataSource(), storage)
        editor.open(record)
        editor.update(record) { it.copy(imageUris = emptyList(), audioUri = "file://new-audio") }
        editor.update(record) { it.copy(audioUri = "file://replacement-audio") }
        editor.close()

        assertEquals(listOf("file://new-audio", "file://replacement-audio"), storage.deletedUris)
    }

    @Test
    fun late_attachment_result_does_not_overwrite_another_record() = runTest {
        val storage = FakeDraftAttachmentStorage()
        val editor = editor(FakeHeartraceDataSource(), storage)
        editor.open(record)
        editor.close()
        val other = record.copy(id = 2)
        editor.open(other)
        editor.update(record) { it.copy(imageUris = it.imageUris + "file://late-image") }

        assertEquals(other, editor.draft()!!.record)
        assertEquals(other.imageUris, editor.draft()!!.imageUris)
        assertEquals(listOf("file://late-image"), storage.deletedUris)
    }

    @Test
    fun attachment_completion_merges_with_the_latest_text() = runTest {
        val editor = editor(FakeHeartraceDataSource(), FakeDraftAttachmentStorage())
        editor.open(record)
        val importCompletion = { editor.update(record) { it.copy(imageUris = it.imageUris + "file://new-image") } }
        editor.update(record) { it.copy(momentText = "Typed during import") }
        importCompletion()

        assertEquals("Typed during import", editor.draft()!!.momentText)
        assertEquals(listOf("file://original-image", "file://new-image"), editor.draft()!!.imageUris)
    }

    @Test
    fun leaving_the_destination_during_save_keeps_files_until_the_commit_finishes() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeHeartraceDataSource().apply { updateCheckInGate = gate }
        val storage = FakeDraftAttachmentStorage()
        val editor = editor(repository, storage)
        editor.open(record)
        editor.update(record) { it.copy(imageUris = it.imageUris + "file://new-image") }
        editor.save()
        editor.dispose()
        assertTrue(storage.deletedUris.isEmpty())
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("file://original-image", "file://new-image"), repository.updatedRecords.single().imageUris)
        assertTrue(storage.deletedUris.isEmpty())
    }

    @Test
    fun failed_commit_after_leaving_the_destination_cleans_only_new_files() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeHeartraceDataSource().apply {
            updateCheckInGate = gate
            updateCheckInError = IllegalStateException("Unavailable")
        }
        val storage = FakeDraftAttachmentStorage()
        val editor = editor(repository, storage)
        editor.open(record)
        editor.update(record) { it.copy(imageUris = it.imageUris + "file://new-image") }
        editor.save()
        editor.dispose()
        assertTrue(storage.deletedUris.isEmpty())
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("file://new-image"), storage.deletedUris)
        assertTrue(repository.updatedRecords.isEmpty())
    }

    private fun editor(repository: FakeHeartraceDataSource, storage: FakeDraftAttachmentStorage): Editor = if (patterns) {
        val viewModel = PatternsViewModel(repository, storage)
        Editor(
            draft = { viewModel.uiState.editingDraft },
            isSaving = { viewModel.uiState.isUpdatingRecord },
            error = { viewModel.uiState.errorMessage },
            open = viewModel::startEditRecord,
            update = viewModel::updateEditDraft,
            close = viewModel::dismissEditRecord,
            dispose = { dispose(viewModel) },
            delete = { viewModel.deleteRecord(viewModel.uiState.editingDraft!!.record.id) },
            save = {
                viewModel.uiState.editingDraft!!.let {
                    viewModel.updateRecord(it.record.id, it.moodValue, it.energyLevel, it.momentText, it.recordedAt, it.imageUris, it.audioUri, it.audioDurationMs)
                }
            },
        )
    } else {
        val viewModel = TodayViewModel(repository, storage)
        Editor(
            draft = { viewModel.uiState.editingDraft },
            isSaving = { viewModel.uiState.isUpdatingRecord },
            error = { viewModel.uiState.recordErrorMessage },
            open = viewModel::startEditRecord,
            update = viewModel::updateEditDraft,
            close = viewModel::dismissEditRecord,
            dispose = { dispose(viewModel) },
            delete = { viewModel.deleteRecord(viewModel.uiState.editingDraft!!.record.id) },
            save = {
                viewModel.uiState.editingDraft!!.let {
                    viewModel.updateRecord(it.record.id, it.moodValue, it.energyLevel, it.momentText, it.recordedAt, it.imageUris, it.audioUri, it.audioDurationMs)
                }
            },
        )
    }

    private data class Editor(
        val draft: () -> EditRecordDraft?,
        val isSaving: () -> Boolean,
        val error: () -> String?,
        val open: (MoodRecord) -> Unit,
        val update: (MoodRecord, (EditRecordDraft) -> EditRecordDraft) -> Unit,
        val close: () -> Unit,
        val dispose: () -> Unit,
        val delete: () -> Unit,
        val save: () -> Unit,
    )

    private fun dispose(viewModel: ViewModel) {
        ViewModelStore().apply {
            put("editor", viewModel)
            clear()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "patterns={0}")
        fun screens(): List<Array<Boolean>> = listOf(arrayOf(false), arrayOf(true))
    }
}
