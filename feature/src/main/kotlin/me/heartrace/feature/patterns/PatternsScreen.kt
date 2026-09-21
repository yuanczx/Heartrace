package me.heartrace.feature.patterns

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import me.heartrace.feature.today.EditRecordSheet
import me.heartrace.feature.today.MoodRecordRow
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
                }
            }
        }
        Spacer(Modifier.height(23.dp))
        DailyTrendSection(summaries = state.dailySummaries)
        Spacer(Modifier.height(23.dp))
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
        if (state.records.isEmpty()) {
            Text(stringResource(R.string.no_history_for_date), color = Muted, fontSize = 13.sp)
        } else {
            state.records.forEach { record ->
                MoodRecordRow(record = record, showDate = true, onLongClick = { viewModel.startEditRecord(record) })
            }
        }
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
