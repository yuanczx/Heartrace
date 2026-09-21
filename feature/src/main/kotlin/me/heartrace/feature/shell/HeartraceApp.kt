package me.heartrace.feature.shell

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.heartrace.data.AppLanguage
import me.heartrace.ui.theme.HeartraceMaterialTheme

private fun AppLanguage.toLocaleListCompat(): LocaleListCompat = when (this) {
    AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
    AppLanguage.ENGLISH, AppLanguage.CHINESE -> LocaleListCompat.forLanguageTags(tag)
}

@Composable
fun HeartraceApp() {
    val settingsViewModel: AppSettingsViewModel = hiltViewModel()
    val settings = settingsViewModel.uiState
    LaunchedEffect(settings.isLoaded, settings.language) {
        if (settings.isLoaded) {
            val desiredLocales = settings.language.toLocaleListCompat()
            if (AppCompatDelegate.getApplicationLocales() != desiredLocales) {
                AppCompatDelegate.setApplicationLocales(desiredLocales)
            }
        }
    }
    HeartraceMaterialTheme(darkTheme = !settings.lightTheme, dynamicColor = false) {
        if (settings.isLoaded) HeartraceRoot(settingsViewModel)
    }
}
