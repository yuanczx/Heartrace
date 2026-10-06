package me.heartrace.feature.patterns

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.heartrace.feature.metrics.WeeklyGoalProgress
import me.heartrace.feature.today.EditRecordSheet
import me.heartrace.feature.today.MoodRecordRow
import me.heartrace.ui.components.AdaptiveTwoPane
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.SectionLabel
import me.heartrace.uikit.R

@Composable
fun PatternsScreen(viewModel: PatternsViewModel = hiltViewModel()) {
    val state = viewModel.uiState
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.insights_title), color = Cream, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(stringResource(R.string.insights_subtitle), color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(23.dp))
        AdaptiveTwoPane(
            modifier = Modifier.weight(1f),
            first = {
                LazyColumn(Modifier.fillMaxSize()) {
                    item(key = "overview") { InsightsOverviewPane(state) }
                }
            },
            second = {
                LazyColumn(Modifier.fillMaxSize()) { historyItems(state, viewModel) }
            },
            compact = {
                LazyColumn(Modifier.fillMaxSize()) {
                    item(key = "overview") { InsightsOverviewPane(state) }
                    item(key = "spacing") { Spacer(Modifier.height(23.dp)) }
                    historyItems(state, viewModel)
                }
            },
        )
    }
    state.editingDraft?.let { draft ->
        val record = draft.record
        EditRecordSheet(
            draft = draft,
            onDraftChange = { transform -> viewModel.updateEditDraft(record, transform) },
            isSaving = state.isUpdatingRecord,
            onDismiss = viewModel::dismissEditRecord,
            onSave = { moodValue, energyLevel, momentText, recordedAt, imageUris, audioUri, audioDurationMs ->
                viewModel.updateRecord(record.id, moodValue, energyLevel, momentText, recordedAt, imageUris, audioUri, audioDurationMs)
            },
            onDelete = { viewModel.deleteRecord(record.id) },
            errorMessage = state.errorMessage,
        )
    }
}

@Composable
private fun InsightsOverviewPane(state: PatternsUiState) {
    SectionLabel(stringResource(R.string.insights_feedback))
    Spacer(Modifier.height(9.dp))
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (!state.hasHistory) {
                Text(stringResource(R.string.no_history), color = Cream, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text(stringResource(R.string.insights_ready), color = Cream, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(
                    state.averageEnergy?.let { stringResource(R.string.average_energy, it) }
                        ?: stringResource(R.string.average_energy_not_recorded),
                    color = Muted,
                    fontSize = 13.sp,
                )
                Text(
                    state.averageMood?.let { stringResource(R.string.average_mood, it) }
                        ?: stringResource(R.string.average_mood_no_data),
                    color = Muted,
                    fontSize = 13.sp,
                )
                Text(stringResource(R.string.insights_record_count, state.recordCount), color = Muted, fontSize = 13.sp)
                Text(stringResource(R.string.insights_active_days, state.activeDays), color = Muted, fontSize = 13.sp)
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    WeeklyGoalCard(state.weeklyGoal)
    Spacer(Modifier.height(18.dp))
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.streak_title, state.currentStreak),
            color = Cream,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(16.dp),
        )
    }
    Spacer(Modifier.height(23.dp))
    DailyTrendSection(summaries = state.dailySummaries)
}

@Composable
private fun WeeklyGoalCard(progress: WeeklyGoalProgress) {
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.weekly_goal_title), color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Text(stringResource(R.string.weekly_goal_progress, progress.completedDays, progress.goalDays), color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (progress.completedDays.toFloat() / progress.goalDays).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                when {
                    progress.isComplete -> stringResource(R.string.weekly_goal_complete)
                    progress.remainingDays == 1 -> stringResource(R.string.weekly_goal_remaining, progress.remainingDays)
                    else -> stringResource(R.string.weekly_goal_remaining_plural, progress.remainingDays)
                },
                color = Muted,
                fontSize = 12.sp,
            )
        }
    }
}

private fun LazyListScope.historyItems(state: PatternsUiState, viewModel: PatternsViewModel) {
    item(key = "history_header") {
        SectionLabel(stringResource(R.string.history))
        Spacer(Modifier.height(9.dp))
        HistoryDatePager(
            dateStart = state.selectedDateStart.takeIf { it > 0L } ?: System.currentTimeMillis(),
            range = state.historyRange,
            canGoToNextDate = state.canGoToNextDate,
            onPreviousDate = viewModel::showPreviousHistoryDate,
            onNextDate = viewModel::showNextHistoryDate,
            onDateSelected = viewModel::selectHistoryDate,
            onRangeSelected = viewModel::selectHistoryRange,
        )
        Spacer(Modifier.height(9.dp))
    }
    if (state.records.isEmpty()) {
        item(key = "empty_history") {
            Text(stringResource(R.string.no_history_for_date), color = Muted, fontSize = 13.sp)
        }
    } else {
        items(state.records, key = { it.id }, contentType = { "record" }) { record ->
            MoodRecordRow(record = record, showDate = true, onLongClick = { viewModel.startEditRecord(record) })
        }
    }
}
