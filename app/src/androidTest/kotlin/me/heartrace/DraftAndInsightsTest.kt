package me.heartrace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.HeartraceRepository
import me.heartrace.data.InsightsDataSnapshot
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.data.db.HeartraceDatabase
import me.heartrace.feature.attachments.InternalDraftAttachmentStorage
import me.heartrace.feature.patterns.HistoryRange
import me.heartrace.feature.patterns.PatternsScreen
import me.heartrace.feature.patterns.PatternsViewModel
import me.heartrace.feature.today.EditRecordSheet
import me.heartrace.feature.today.TodayViewModel
import me.heartrace.ui.components.FullScreenImageDialog
import me.heartrace.ui.theme.HeartraceMaterialTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import me.heartrace.uikit.R as UIKitR

class DraftAndInsightsTest {
    @get:Rule val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val viewModels = ViewModelStore()
    private lateinit var database: HeartraceDatabase
    private lateinit var repository: HeartraceRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, HeartraceDatabase::class.java).build()
        repository = HeartraceRepository(database.dailyNoteDao(), database.appStateDao(), database.moodEntryDao(), database, context)
    }

    @After
    fun tearDown() {
        composeRule.runOnIdle { viewModels.clear() }
        database.close()
    }

    @Test
    fun edit_draft_survives_recreating_the_sheet_content() {
        lateinit var viewModel: TodayViewModel
        val generation = mutableIntStateOf(0)
        composeRule.runOnUiThread {
            viewModel = track(TodayViewModel(repository, InternalDraftAttachmentStorage(context)))
            viewModel.startEditRecord(MoodRecord(1, Mood.BRIGHT, 72, null, System.currentTimeMillis()))
        }
        composeRule.setContent {
            HeartraceMaterialTheme(darkTheme = false) {
                key(generation.intValue) {
                    EditRecordSheet(
                        draft = viewModel.uiState.editingDraft!!,
                        onDraftChange = { viewModel.updateEditDraft(viewModel.uiState.editingRecord!!, it) },
                        isSaving = false,
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _, _ -> },
                        onDelete = {},
                    )
                }
            }
        }
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Unsaved note")
        composeRule.runOnIdle { generation.intValue++ }
        composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
        composeRule.onNodeWithText("Unsaved note").assertIsDisplayed()
    }

    @Test
    fun pending_save_sheet_stays_visible_and_disables_actions() {
        val record = MoodRecord(1, Mood.BRIGHT, 72, null, System.currentTimeMillis())
        composeRule.setContent {
            HeartraceMaterialTheme(darkTheme = true) {
                EditRecordSheet(
                    draft = me.heartrace.feature.today.EditRecordDraft(record),
                    onDraftChange = {},
                    isSaving = true,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _ -> },
                    onDelete = {},
                    errorMessage = "save_failed",
                )
            }
        }
        composeRule.onNodeWithText(context.getString(UIKitR.string.edit_record)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(UIKitR.string.save)).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(UIKitR.string.cancel)).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(UIKitR.string.delete_record)).assertIsNotEnabled()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(UIKitR.string.save_failed)).assertExists()
        Espresso.pressBack()
        composeRule.onNodeWithText(context.getString(UIKitR.string.edit_record)).assertIsDisplayed()
    }

    @Test
    fun annual_history_composes_a_bounded_number_of_rows_on_compact_screens() = checkLazyHistory(wide = false)

    @Test
    fun annual_history_composes_a_bounded_number_of_rows_on_wide_screens() = checkLazyHistory(wide = true)

    @Test
    fun missing_energy_is_shown_as_not_recorded() {
        val record = MoodRecord(1, Mood.BRIGHT, 72, null, System.currentTimeMillis())
        val viewModel = patternsViewModel(listOf(record))
        composeRule.setContent { HeartraceMaterialTheme(darkTheme = false) { PatternsScreen(viewModel) } }

        composeRule.onNodeWithText(context.getString(UIKitR.string.average_energy_not_recorded)).assertIsDisplayed()
        composeRule.runOnIdle {
            viewModel.selectHistoryRange(HistoryRange.MONTH)
            viewModel.showPreviousHistoryDate()
        }
        composeRule.onNodeWithText(context.getString(UIKitR.string.average_mood_no_data)).assertIsDisplayed()
    }

    @Test
    fun image_preview_exposes_a_close_action_in_the_light_theme() {
        val open = androidx.compose.runtime.mutableStateOf(true)
        composeRule.setContent {
            HeartraceMaterialTheme(darkTheme = false) {
                if (open.value) FullScreenImageDialog("file:///missing-image", onDismiss = { open.value = false })
            }
        }
        composeRule.onNodeWithContentDescription(context.getString(UIKitR.string.close_image_preview)).assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(!open.value) }
    }

    private fun checkLazyHistory(wide: Boolean) {
        val records = List(400) { index ->
            MoodRecord(index.toLong() + 1, Mood.BRIGHT, 72, null, System.currentTimeMillis() - index * 60_000L, momentText = "Record $index")
        }
        val viewModel = patternsViewModel(records)
        composeRule.runOnUiThread { viewModel.selectHistoryRange(HistoryRange.YEAR) }
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides if (wide) Density(density.density / 3f) else density) {
                HeartraceMaterialTheme(darkTheme = !wide) {
                    Box(Modifier.fillMaxSize()) { PatternsScreen(viewModel) }
                }
            }
        }
        val lists = composeRule.onAllNodes(hasScrollAction())
        lists[if (wide) 1 else 0].performScrollToNode(hasText("\"Record 399\""))
        composeRule.onNodeWithText("\"Record 399\"").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("\"Record ", substring = true).fetchSemanticsNodes().size < 40)
        composeRule.onNodeWithText("\"Record 0\"").assertDoesNotExist()
    }

    private fun patternsViewModel(records: List<MoodRecord>): PatternsViewModel {
        val insights = MutableStateFlow(InsightsDataSnapshot(records = records, recentRecords = records))
        val source = object : HeartraceDataSource by repository {
            override fun observeInsights() = insights
        }
        lateinit var viewModel: PatternsViewModel
        composeRule.runOnUiThread { viewModel = track(PatternsViewModel(source, InternalDraftAttachmentStorage(context))) }
        return viewModel
    }

    private fun <T : ViewModel> track(viewModel: T): T = viewModel.also { viewModels.put(it.javaClass.name, it) }
}
