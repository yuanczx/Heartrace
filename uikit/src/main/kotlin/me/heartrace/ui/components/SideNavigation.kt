package me.heartrace.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.data.AppTab
import me.heartrace.uikit.R

@Composable
fun SideNavigation(
    selected: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        NavigationItem(AppTab.TODAY, stringResource(R.string.nav_today), Icons.Outlined.Today, Icons.Filled.Today),
        NavigationItem(
            AppTab.INSIGHTS,
            stringResource(R.string.nav_insights),
            Icons.AutoMirrored.Outlined.ShowChart,
            Icons.AutoMirrored.Filled.ShowChart,
        ),
        NavigationItem(AppTab.YOU, stringResource(R.string.nav_you), Icons.Outlined.Person, Icons.Filled.Person),
    )
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = Ink,
        header = {
            Box(Modifier.padding(top = 8.dp, bottom = 20.dp)) {
                Surface(color = Coral, shape = CircleShape, modifier = Modifier.size(14.dp)) {}
            }
        },
    ) {
        Spacer(Modifier.weight(1f))
        items.forEach { item ->
            val isSelected = selected == item.tab
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(21.dp),
                    )
                },
                label = { Text(item.label, fontSize = 10.sp) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Cream,
                    selectedTextColor = Cream,
                    indicatorColor = PanelLight,
                    unselectedIconColor = Muted.copy(alpha = .58f),
                    unselectedTextColor = Muted.copy(alpha = .58f),
                ),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

private data class NavigationItem(
    val tab: AppTab,
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
)
