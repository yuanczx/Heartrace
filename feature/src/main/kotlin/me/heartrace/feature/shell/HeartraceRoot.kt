package me.heartrace.feature.shell

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import kotlinx.coroutines.launch
import me.heartrace.data.AppTab
import me.heartrace.feature.patterns.PatternsScreen
import me.heartrace.feature.today.TodayScreen
import me.heartrace.feature.you.YouScreen
import me.heartrace.ui.components.BottomBar
import me.heartrace.ui.components.Ink
import me.heartrace.ui.components.SideNavigation
import kotlin.math.roundToInt

internal const val NAVIGATION_RAIL_BREAKPOINT_DP = 600

internal fun shouldUseNavigationRail(widthDp: Int): Boolean = widthDp >= NAVIGATION_RAIL_BREAKPOINT_DP

@Composable
internal fun HeartraceRoot(settingsViewModel: AppSettingsViewModel) {
    val tabs = remember { listOf(HeartraceDestination.Today, HeartraceDestination.Insights, HeartraceDestination.You) }
    val backStack = remember { mutableStateListOf<HeartraceDestination>(HeartraceDestination.Today) }
    val currentDestination = backStack.lastOrNull() ?: HeartraceDestination.Today
    val initialPage = remember { tabs.indexOf(currentDestination).coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(pagerState.settledPage) {
        val targetDestination = tabs[pagerState.settledPage]
        if (backStack.lastOrNull() != targetDestination) {
            backStack.clear()
            backStack.add(targetDestination)
        }
    }
    LaunchedEffect(currentDestination) {
        val targetPage = tabs.indexOf(currentDestination)
        if (targetPage != -1 && pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }
    fun navigateToTab(tab: AppTab) {
        val destination = tab.toDestination()
        val pageIndex = tabs.indexOf(destination)
        if (pageIndex != -1) {
            if (backStack.lastOrNull() != destination) {
                backStack.clear()
                backStack.add(destination)
            }
            if (pagerState.currentPage != pageIndex) {
                coroutineScope.launch { pagerState.animateScrollToPage(pageIndex) }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useNavigationRail = shouldUseNavigationRail(maxWidth.value.toInt())
        val contentMaxWidth = when {
            maxWidth >= 900.dp -> 1120.dp
            useNavigationRail -> 760.dp
            else -> 430.dp
        }
        val horizontalContentPadding = if (useNavigationRail) 32.dp else 22.dp
        val verticalContentPadding = if (useNavigationRail) 28.dp else 20.dp
        val entryProvider = remember(contentMaxWidth, horizontalContentPadding, verticalContentPadding) {
            entryProvider {
                entry<HeartraceDestination.Today> { TodayScreen() }
                entry<HeartraceDestination.Insights> { PatternsScreen() }
                entry<HeartraceDestination.You> {
                    YouScreen(
                        settingsViewModel = settingsViewModel,
                        isActive = backStack.lastOrNull() == HeartraceDestination.You,
                        contentMaxWidth = contentMaxWidth,
                        horizontalContentPadding = horizontalContentPadding,
                        verticalContentPadding = verticalContentPadding,
                    )
                }
            }
        }
        val entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator<HeartraceDestination>(),
        )
        val decoratedEntries = rememberDecoratedNavEntries(backStack = tabs, entryDecorators = entryDecorators, entryProvider = entryProvider)
        val indicatorPosition =
            @SuppressLint("FrequentlyChangingValue")
            (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, tabs.lastIndex.toFloat())
        val indicatedTab = tabs[indicatorPosition.roundToInt().coerceIn(tabs.indices)].toAppTab()

        Scaffold(
            containerColor = Ink,
            bottomBar = {
                if (!useNavigationRail) {
                    BottomBar(
                        selected = currentDestination.toAppTab(),
                        indicatorPosition = indicatorPosition,
                        onTabSelected = ::navigateToTab,
                    )
                }
            },
        ) { padding ->
            Row(
                Modifier
                    .fillMaxSize()
                    .background(Ink)
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                if (useNavigationRail) {
                    SideNavigation(
                        selected = indicatedTab,
                        onTabSelected = ::navigateToTab,
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    beyondViewportPageCount = 1,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    val entry = decoratedEntries.getOrNull(page)
                    val isYouPage = page == tabs.indexOf(HeartraceDestination.You)
                    Column(
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (isYouPage) {
                                    Modifier
                                } else {
                                    Modifier.verticalScroll(rememberScrollState())
                                },
                            ),
                    ) {
                        if (isYouPage) {
                            entry?.Content()
                        } else {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = contentMaxWidth)
                                    .align(Alignment.CenterHorizontally)
                                    .padding(
                                        horizontal = horizontalContentPadding,
                                        vertical = verticalContentPadding,
                                    ),
                            ) {
                                entry?.Content()
                            }
                        }
                    }
                }
            }
        }
    }
}
