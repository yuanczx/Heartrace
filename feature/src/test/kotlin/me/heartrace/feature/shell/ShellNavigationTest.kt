package me.heartrace.feature.shell

import me.heartrace.data.AppTab
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellNavigationTest {
    @Test
    fun tabs_and_destinations_are_bijective() {
        AppTab.entries.forEach { tab ->
            assertEquals(tab, tab.toDestination().toAppTab())
        }
        assertEquals(AppTab.TODAY, HeartraceDestination.Today.toAppTab())
        assertEquals(AppTab.INSIGHTS, HeartraceDestination.Insights.toAppTab())
        assertEquals(AppTab.YOU, HeartraceDestination.You.toAppTab())
    }
}
