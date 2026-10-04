package me.heartrace.feature.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.ui.components.AdaptiveTwoPane
import me.heartrace.ui.components.AttachmentSelectorBar
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Header
import me.heartrace.ui.components.Ink
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.PanelLight
import me.heartrace.ui.components.SectionLabel
import me.heartrace.ui.components.deleteInternalAttachment
import me.heartrace.uikit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state = viewModel.uiState
    val context = LocalContext.current
    Column {
        Header(stringResource(R.string.nav_today))
        Spacer(Modifier.height(25.dp))
        Text(stringResource(R.string.today_check_in_title), color = Cream, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(
            if (state.todayMoodRecords.isEmpty()) {
                stringResource(R.string.today_check_in_subtitle)
            } else {
                stringResource(R.string.today_checked_in, state.todayMoodRecords.size)
            },
            color = Muted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(23.dp))
        AdaptiveTwoPane(
            compactSpacing = 25.dp,
            first = {
                TodayCheckInPane(
                    state = state,
                    onMoodSelected = viewModel::selectMood,
                    onEnergyLevelChange = viewModel::setEnergyLevel,
                    onSave = viewModel::saveCheckIn,
                    detailText = state.momentDraft,
                    onDetailTextChange = viewModel::setMomentDraft,
                    detailImageUris = state.imageUris,
                    onAddImages = viewModel::addImages,
                    onRemoveImage = { uri ->
                        deleteInternalAttachment(context, uri)
                        viewModel.removeImage(uri)
                    },
                    detailAudioUri = state.audioUri,
                    detailAudioDurationMs = state.audioDurationMs,
                    onAudioRecorded = { uri, duration ->
                        deleteInternalAttachment(context, state.audioUri)
                        viewModel.setAudio(uri, duration)
                    },
                    onRemoveAudio = {
                        deleteInternalAttachment(context, state.audioUri)
                        viewModel.setAudio(null)
                    },
                    onDetailsExpanded = viewModel::setDetailsExpanded,
                )
            },
            second = {
                TodayTimelinePane(
                    records = state.todayMoodRecords,
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
                viewModel.updateRecord(
                    record.id,
                    moodValue,
                    energyLevel,
                    momentText,
                    recordedAt,
                    imageUris,
                    audioUri,
                    audioDurationMs,
                )
            },
            onDelete = {
                viewModel.deleteRecord(record.id)
            },
        )
    }
}

@Composable
private fun TodayCheckInPane(
    state: TodayUiState,
    onMoodSelected: (Mood) -> Unit,
    onEnergyLevelChange: (Int) -> Unit,
    onSave: () -> Unit,
    detailText: String,
    onDetailTextChange: (String) -> Unit,
    detailImageUris: List<String>,
    onAddImages: (List<String>) -> Unit,
    onRemoveImage: (String) -> Unit,
    detailAudioUri: String?,
    detailAudioDurationMs: Long,
    onAudioRecorded: (String, Long) -> Unit,
    onRemoveAudio: () -> Unit,
    onDetailsExpanded: (Boolean) -> Unit,
) {
    SectionLabel(stringResource(R.string.arriving))
    Spacer(Modifier.height(6.dp))
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        MoodStrip(selected = state.selectedMood, onSelect = onMoodSelected)
    }
    Spacer(Modifier.height(10.dp))
    TextButton(onClick = { onDetailsExpanded(!state.isDetailsExpanded) }) {
        Text(
            if (state.isDetailsExpanded) stringResource(R.string.hide_details) else stringResource(R.string.add_details),
            color = Coral,
            fontWeight = FontWeight.SemiBold,
        )
    }
    if (state.isDetailsExpanded) {
        TextField(
            value = detailText,
            onValueChange = onDetailTextChange,
            placeholder = { Text(stringResource(R.string.moment_placeholder), color = Muted) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PanelLight,
                unfocusedContainerColor = PanelLight,
                focusedTextColor = Cream,
                unfocusedTextColor = Cream,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.height(10.dp))
        EnergyChoices(
            options = listOf(
                EnergyChoiceOption(1, stringResource(R.string.energy_low)),
                EnergyChoiceOption(2, stringResource(R.string.energy_medium)),
                EnergyChoiceOption(3, stringResource(R.string.energy_high)),
            ),
            selectedLevel = state.energyLevel,
            onSelect = onEnergyLevelChange,
        )
        Spacer(Modifier.height(8.dp))
        AttachmentSelectorBar(
            imageUris = detailImageUris,
            onAddImages = onAddImages,
            onRemoveImage = onRemoveImage,
            audioUri = detailAudioUri,
            audioDurationMs = detailAudioDurationMs,
            onAudioRecorded = onAudioRecorded,
            onRemoveAudio = onRemoveAudio,
        )
    }
    Spacer(Modifier.height(10.dp))
    Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(11.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Ink)) {
        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.save_check_in), fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.weekly_goal_today, state.weeklyGoal.completedDays, state.weeklyGoal.goalDays),
        color = Muted,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth(),
    )
    state.errorMessage?.let {
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.save_failed), color = Coral, fontSize = 12.sp)
    }
}

@Composable
private fun TodayTimelinePane(
    records: List<MoodRecord>,
    onEditRecord: (MoodRecord) -> Unit,
) {
    EnergySection(records = records)
    Spacer(Modifier.height(25.dp))
    SectionLabel(stringResource(R.string.today_timeline))
    Spacer(Modifier.height(9.dp))
    if (records.isEmpty()) {
        Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.empty_timeline), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
    } else {
        records.sortedByDescending { it.recordedAt }.forEach { record ->
            MoodRecordRow(record = record, showDate = false, onLongClick = { onEditRecord(record) })
        }
    }
}
