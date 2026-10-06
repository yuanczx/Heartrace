package me.heartrace

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import me.heartrace.uikit.R as UIKitR

class MainNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottom_tabs_navigate_between_primary_screens() {
        val activity = composeRule.activity
        val todayTitle = activity.getString(UIKitR.string.today_check_in_title)
        val insightsTab = activity.getString(UIKitR.string.nav_insights)
        val insightsTitle = activity.getString(UIKitR.string.insights_title)
        val youTab = activity.getString(UIKitR.string.nav_you)
        val youSubtitle = activity.getString(UIKitR.string.you_subtitle)
        val todayTab = activity.getString(UIKitR.string.nav_today)

        waitForScreen(todayTitle)

        composeRule.onNodeWithContentDescription(insightsTab).performClick()
        waitForScreen(insightsTitle)

        composeRule.onNodeWithContentDescription(youTab).performClick()
        waitForScreen(youSubtitle)

        composeRule.onNodeWithContentDescription(todayTab).performClick()
        waitForScreen(todayTitle)
    }

    private fun waitForScreen(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithText(text).isDisplayed()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
