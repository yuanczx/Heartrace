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

    @Test
    fun navigation_rail_is_used_from_compact_tablet_width() {
        assertEquals(false, shouldUseNavigationRail(NAVIGATION_RAIL_BREAKPOINT_DP - 1))
        assertEquals(true, shouldUseNavigationRail(NAVIGATION_RAIL_BREAKPOINT_DP))
        assertEquals(true, shouldUseNavigationRail(1024))
    }
}
