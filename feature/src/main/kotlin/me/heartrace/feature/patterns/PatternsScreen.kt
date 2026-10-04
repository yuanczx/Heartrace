package me.heartrace.feature.patterns

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.heartrace.feature.today.EditRecordSheet
import me.heartrace.feature.today.MoodRecordRow
import me.heartrace.ui.components.AdaptiveTwoPane
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Header
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.SectionLabel
import me.heartrace.uikit.R

@Composable
fun PatternsScreen(viewModel: PatternsViewModel = hiltViewModel()) {
    val state = viewModel.uiState
    Column {
        Header(stringResource(R.string.nav_insights))
        Spacer(Modifier.height(25.dp))
        Text(stringResource(R.string.insights_title), color = Cream, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(stringResource(R.string.insights_subtitle), color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(23.dp))
        AdaptiveTwoPane(
            compactSpacing = 23.dp,
            first = {
                InsightsOverviewPane(state)
            },
            second = {
                HistoryPane(
                    state = state,
                    onPreviousDate = viewModel::showPreviousHistoryDate,
                    onNextDate = viewModel::showNextHistoryDate,
                    onDateSelected = viewModel::selectHistoryDate,
                    onRangeSelected = viewModel::selectHistoryRange,
                    onEditRecord = viewModel::startEditRecord,
                )
            },
        )
    }
    state.editingRecord?.let { record ->
        EditRecordSheet(
            record = record,
            onDismiss = viewModel::dismissEditRecord,
            onSave = { moodValue, energyLevel, momentText, recordedAt, imageUris, audioUri, audioDurationMs ->
                viewModel.updateRecord(record.id, moodValue, energyLevel, momentText, recordedAt, imageUris, audioUri, audioDurationMs)
            },
            onDelete = { viewModel.deleteRecord(record.id) },
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
                Text(stringResource(R.string.average_energy, state.averageEnergy ?: 0f), color = Muted, fontSize = 13.sp)
                Text(stringResource(R.string.average_mood, state.averageMood ?: 0), color = Muted, fontSize = 13.sp)
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
    Spacer(Modifier.height(23.dp))
    BadgesCard(state.badges)
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

@Composable
private fun BadgesCard(badges: List<BadgeStatus>) {
    Column {
        SectionLabel(stringResource(R.string.badges_title))
        Spacer(Modifier.height(9.dp))
        Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                badges.forEach { badge ->
                    val title = when (badge.kind) {
                        BadgeKind.FIRST_RECORD -> stringResource(R.string.badge_first_record)
                        BadgeKind.THREE_DAY_STREAK -> stringResource(R.string.badge_three_day_streak)
                        BadgeKind.SEVEN_DAY_STREAK -> stringResource(R.string.badge_seven_day_streak)
                        BadgeKind.FOURTEEN_DAY_STREAK -> stringResource(R.string.badge_fourteen_day_streak)
                        BadgeKind.TEN_RECORDS -> stringResource(R.string.badge_ten_records)
                        BadgeKind.FIRST_WEEKLY_GOAL -> stringResource(R.string.badge_first_weekly_goal)
                        BadgeKind.FOUR_WEEKLY_GOALS -> stringResource(R.string.badge_four_weekly_goals)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (badge.earned) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                            contentDescription = title,
                            tint = if (badge.earned) me.heartrace.ui.components.Coral else Muted,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (badge.earned) title else stringResource(R.string.badge_locked, title),
                            color = if (badge.earned) Cream else Muted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPane(
    state: PatternsUiState,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onRangeSelected: (HistoryRange) -> Unit,
    onEditRecord: (me.heartrace.data.MoodRecord) -> Unit,
) {
    SectionLabel(stringResource(R.string.history))
    Spacer(Modifier.height(9.dp))
    HistoryDatePager(
        dateStart = state.selectedDateStart.takeIf { it > 0L } ?: System.currentTimeMillis(),
        range = state.historyRange,
        canGoToNextDate = state.canGoToNextDate,
        onPreviousDate = onPreviousDate,
        onNextDate = onNextDate,
        onDateSelected = onDateSelected,
        onRangeSelected = onRangeSelected,
    )
    Spacer(Modifier.height(9.dp))
    if (state.records.isEmpty()) {
        Text(stringResource(R.string.no_history_for_date), color = Muted, fontSize = 13.sp)
    } else {
        state.records.forEach { record ->
            MoodRecordRow(record = record, showDate = true, onLongClick = { onEditRecord(record) })
        }
    }
}
