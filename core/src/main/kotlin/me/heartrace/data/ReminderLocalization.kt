package me.heartrace.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** Applies persisted app language even when a receiver starts a fresh process. */
internal fun localizedReminderContext(context: Context, language: AppLanguage): Context {
    val configuration = Configuration(context.resources.configuration).apply {
        if (language == AppLanguage.SYSTEM) {
            setLocales(Resources.getSystem().configuration.locales)
        } else {
            setLocale(Locale.forLanguageTag(language.tag))
        }
    }
    return context.createConfigurationContext(configuration)
}
