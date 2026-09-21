package me.heartrace.feature.shell

import androidx.navigation3.runtime.NavKey
import me.heartrace.data.AppTab

internal sealed interface HeartraceDestination : NavKey {
    data object Today : HeartraceDestination
    data object Insights : HeartraceDestination
    data object You : HeartraceDestination
}

internal fun HeartraceDestination.toAppTab(): AppTab = when (this) {
    HeartraceDestination.Today -> AppTab.TODAY
    HeartraceDestination.Insights -> AppTab.INSIGHTS
    HeartraceDestination.You -> AppTab.YOU
}

internal fun AppTab.toDestination(): HeartraceDestination = when (this) {
    AppTab.TODAY -> HeartraceDestination.Today
    AppTab.INSIGHTS -> HeartraceDestination.Insights
    AppTab.YOU -> HeartraceDestination.You
}
