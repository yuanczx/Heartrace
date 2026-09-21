package me.heartrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.heartrace.data.AppTab
import me.heartrace.uikit.R
import kotlin.math.roundToInt

@Composable
fun BottomBar(
    selected: AppTab,
    indicatorPosition: Float = selected.index.toFloat(),
    onTabSelected: (AppTab) -> Unit,
) {
    val tabs = listOf(
        Triple(stringResource(R.string.nav_today), Icons.Outlined.Today, Icons.Filled.Today),
        Triple(
            stringResource(R.string.nav_insights),
            Icons.AutoMirrored.Outlined.ShowChart,
            Icons.AutoMirrored.Filled.ShowChart,
        ),
        Triple(stringResource(R.string.nav_you), Icons.Outlined.Person, Icons.Filled.Person),
    )
    val pagePosition = indicatorPosition.coerceIn(0f, tabs.lastIndex.toFloat())
    val activeIndex = pagePosition.roundToInt().coerceIn(0, tabs.lastIndex)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .background(Ink)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorWidth = 52.dp
        val indicatorOffset = (
            tabWidth * pagePosition + (tabWidth - indicatorWidth) / 2
            ).coerceIn(0.dp, maxWidth - indicatorWidth)
        Surface(
            color = PanelLight,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .offset(x = indicatorOffset, y = 2.dp)
                .width(indicatorWidth)
                .height(28.dp),
        ) {}
        Row(verticalAlignment = Alignment.CenterVertically) {
            BottomTab(tabs[0].first, tabs[0].second, tabs[0].third, activeIndex == 0, Modifier.weight(1f)) {
                onTabSelected(AppTab.TODAY)
            }
            BottomTab(tabs[1].first, tabs[1].second, tabs[1].third, activeIndex == 1, Modifier.weight(1f)) {
                onTabSelected(AppTab.INSIGHTS)
            }
            BottomTab(tabs[2].first, tabs[2].second, tabs[2].third, activeIndex == 2, Modifier.weight(1f)) {
                onTabSelected(AppTab.YOU)
            }
        }
    }
}

private val AppTab.index: Int
    get() = when (this) {
        AppTab.TODAY -> 0
        AppTab.INSIGHTS -> 1
        AppTab.YOU -> 2
    }
