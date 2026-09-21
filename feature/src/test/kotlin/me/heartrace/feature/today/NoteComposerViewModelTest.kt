package me.heartrace.feature.today

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.heartrace.data.Mood
import me.heartrace.data.TodayDataSnapshot
import me.heartrace.feature.testing.FakeHeartraceDataSource
import me.heartrace.feature.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteComposerViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun edits_are_kept_and_save_trims_text() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            today.value = TodayDataSnapshot(mood = Mood.BRIGHT)
        }
        val viewModel = NoteComposerViewModel(repository)
        advanceUntilIdle()
        viewModel.updateDraft("  a note  ")
        viewModel.updateMood(Mood.ELECTRIC)
        viewModel.addImages(listOf("one", "two"))
        viewModel.removeImage("one")
        viewModel.setAudio("audio", 900L)
        var succeeded = false

        viewModel.save { succeeded = true }
        advanceUntilIdle()

        assertEquals(Mood.ELECTRIC, repository.savedNotes.single().mood)
        assertEquals("a note", repository.savedNotes.single().text)
        assertEquals(listOf("two"), repository.savedNotes.single().imageUris)
        assertTrue(succeeded)
        assertEquals(false, viewModel.uiState.isSaving)
    }

    @Test
    fun save_failure_is_visible_and_can_be_retried() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            saveNoteError = IllegalStateException()
        }
        val viewModel = NoteComposerViewModel(repository)
        viewModel.updateDraft("note")
        viewModel.save {}
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.saveError)
        assertEquals(false, viewModel.uiState.isSaving)
    }
}
