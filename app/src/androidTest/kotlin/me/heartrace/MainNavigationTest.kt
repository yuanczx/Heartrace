package me.heartrace

import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
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

    @Test
    fun achievements_only_handle_system_back_on_you_tab() {
        val activity = composeRule.activity
        val youTab = activity.getString(UIKitR.string.nav_you)
        val youSubtitle = activity.getString(UIKitR.string.you_subtitle)
        val achievementsTitle = activity.getString(UIKitR.string.badges_title)
        val achievementsSubtitle = activity.getString(UIKitR.string.achievements_page_subtitle)
        val insightsTab = activity.getString(UIKitR.string.nav_insights)
        val insightsTitle = activity.getString(UIKitR.string.insights_title)
        var fallbackBackCount = 0
        val fallbackBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                fallbackBackCount++
            }
        }
        // Keep the activity open when Insights delegates back to the host.
        composeRule.runOnUiThread {
            activity.onBackPressedDispatcher.addCallback(activity, fallbackBackCallback)
        }
        try {
            waitForScreen(activity.getString(UIKitR.string.today_check_in_title))
            composeRule.onNodeWithContentDescription(youTab).performClick()
            waitForScreen(youSubtitle)
            composeRule.onNodeWithText(achievementsTitle).performClick()
            waitForScreen(achievementsSubtitle)

            composeRule.onNodeWithContentDescription(insightsTab).performClick()
            waitForScreen(insightsTitle)
            composeRule.runOnUiThread {
                activity.onBackPressedDispatcher.onBackPressed()
            }
            composeRule.runOnIdle { assertEquals(1, fallbackBackCount) }
            waitForScreen(insightsTitle)

            composeRule.onNodeWithContentDescription(youTab).performClick()
            waitForScreen(achievementsSubtitle)
            composeRule.runOnUiThread {
                activity.onBackPressedDispatcher.onBackPressed()
            }
            waitForScreen(youSubtitle)
            composeRule.runOnIdle { assertEquals(1, fallbackBackCount) }
        } finally {
            composeRule.runOnUiThread { fallbackBackCallback.remove() }
        }
    }

    @Test
    fun unsaved_record_editor_survives_activity_recreation() {
        val activity = composeRule.activity
        val savedText = "Saved note ${System.nanoTime()}"
        val draftText = "Unsaved changes"
        waitForScreen(activity.getString(UIKitR.string.today_check_in_title))
        composeRule.onNodeWithText(activity.getString(UIKitR.string.add_details)).performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement(savedText)
        composeRule.onNodeWithText(activity.getString(UIKitR.string.save_check_in)).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("\"$savedText\"").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("\"$savedText\"").performScrollTo().performTouchInput { longClick() }
        waitForScreen(activity.getString(UIKitR.string.edit_record))
        composeRule.onNode(hasSetTextAction()).performTextReplacement(draftText)

        composeRule.activityRule.scenario.recreate()

        waitForScreen(composeRule.activity.getString(UIKitR.string.edit_record))
        composeRule.onNodeWithText(draftText).assertIsDisplayed()
    }

    private fun waitForScreen(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithText(text).isDisplayed()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
