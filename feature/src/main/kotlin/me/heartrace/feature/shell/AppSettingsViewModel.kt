package me.heartrace.feature.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.heartrace.data.AppLanguage
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.ReminderSchedulerPort
import javax.inject.Inject

data class AppSettingsUiState(
    val isLoaded: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val lightTheme: Boolean = false,
    val remindersEnabled: Boolean = false,
    val nickname: String = "",
    val avatarUri: String? = null,
)

@HiltViewModel
class AppSettingsViewModel
@Inject
constructor(
    private val repository: HeartraceDataSource,
    private val reminderScheduler: ReminderSchedulerPort,
) : ViewModel() {
    var uiState by mutableStateOf(AppSettingsUiState())
        private set

    private var lastReminderState: Boolean? = null

    init {
        viewModelScope.launch {
            repository.observePreferences().collect { snapshot ->
                uiState =
                    AppSettingsUiState(
                        isLoaded = true,
                        language = snapshot.language,
                        lightTheme = snapshot.lightTheme,
                        remindersEnabled = snapshot.remindersEnabled,
                        nickname = snapshot.nickname,
                        avatarUri = snapshot.avatarUri,
                    )
                if (lastReminderState != snapshot.remindersEnabled) {
                    lastReminderState = snapshot.remindersEnabled
                    reminderScheduler.setEnabled(snapshot.remindersEnabled)
                }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        uiState = uiState.copy(language = language)
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setLightTheme(light: Boolean) {
        uiState = uiState.copy(lightTheme = light)
        viewModelScope.launch { repository.setLightTheme(light) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        uiState = uiState.copy(remindersEnabled = enabled)
        if (lastReminderState != enabled) {
            lastReminderState = enabled
            reminderScheduler.setEnabled(enabled)
        }
        viewModelScope.launch { repository.setRemindersEnabled(enabled) }
    }
}
