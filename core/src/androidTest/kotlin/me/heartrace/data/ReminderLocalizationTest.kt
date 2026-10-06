package me.heartrace.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.platform.app.InstrumentationRegistry
import me.heartrace.core.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Locale

class ReminderLocalizationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun persisted_language_overrides_a_fresh_receiver_context() {
        val english = localizedReminderContext(contextWithLocale(Locale.ENGLISH), AppLanguage.ENGLISH)
        val chinese = localizedReminderContext(contextWithLocale(Locale.ENGLISH), AppLanguage.CHINESE)
        val receiverAfterRestart = localizedReminderContext(contextWithLocale(Locale.CHINESE), AppLanguage.ENGLISH)

        listOf(R.string.reminder_channel_name, R.string.reminder_notification_title, R.string.reminder_notification_text).forEach { id ->
            assertNotEquals(english.getString(id), chinese.getString(id))
            assertEquals(english.getString(id), receiverAfterRestart.getString(id))
        }
        assertEquals("zh", chinese.resources.configuration.locales[0].language)
    }

    @Test
    fun system_language_clears_the_previous_app_language() {
        val localized = localizedReminderContext(contextWithLocale(Locale.CHINESE), AppLanguage.SYSTEM)

        assertEquals(Resources.getSystem().configuration.locales, localized.resources.configuration.locales)
    }

    private fun contextWithLocale(locale: Locale): Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(locale) },
    )
}
