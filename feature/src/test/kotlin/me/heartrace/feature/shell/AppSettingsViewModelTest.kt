package me.heartrace.feature.shell

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.heartrace.data.AppLanguage
import me.heartrace.feature.testing.FakeHeartraceDataSource
import me.heartrace.feature.testing.FakeReminderScheduler
import me.heartrace.feature.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initial_preferences_are_loaded_and_reminder_state_is_synced() = runTest {
        val repository = FakeHeartraceDataSource().apply {
            preferences.value = preferences.value.copy(language = AppLanguage.CHINESE, lightTheme = true)
        }
        val scheduler = FakeReminderScheduler()
        val viewModel = AppSettingsViewModel(repository, scheduler)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.isLoaded)
        assertEquals(AppLanguage.CHINESE, viewModel.uiState.language)
        assertEquals(true, viewModel.uiState.lightTheme)
        assertEquals(listOf(false), scheduler.enabledValues)
    }

    @Test
    fun changing_reminders_updates_scheduler_and_preferences() = runTest {
        val repository = FakeHeartraceDataSource()
        val scheduler = FakeReminderScheduler()
        val viewModel = AppSettingsViewModel(repository, scheduler)
        advanceUntilIdle()

        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.setLightTheme(true)
        viewModel.setRemindersEnabled(true)
        advanceUntilIdle()

        assertEquals(AppLanguage.ENGLISH, repository.preferences.value.language)
        assertEquals(true, repository.preferences.value.lightTheme)
        assertEquals(true, repository.preferences.value.remindersEnabled)
        assertEquals(listOf(false, true), scheduler.enabledValues)
    }
}
