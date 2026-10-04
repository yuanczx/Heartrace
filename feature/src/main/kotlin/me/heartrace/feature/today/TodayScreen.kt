package me.heartrace.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    var momentDraft by remember(state.pendingMomentForId) { mutableStateOf("") }
    var momentImageUris by remember(state.pendingMomentForId) { mutableStateOf<List<String>>(emptyList()) }
    var momentAudioUri by remember(state.pendingMomentForId) { mutableStateOf<String?>(null) }
    var momentAudioDurationMs by remember(state.pendingMomentForId) { mutableLongStateOf(0L) }
    fun discardMomentAttachments() {
        if (state.isSavingMoment) return
        momentImageUris.forEach { deleteInternalAttachment(context, it) }
        deleteInternalAttachment(context, momentAudioUri)
        momentImageUris = emptyList()
        momentAudioUri = null
        momentAudioDurationMs = 0L
    }
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
                    onMoodValueChange = viewModel::setMoodValue,
                    onEnergyLevelChange = viewModel::setEnergyLevel,
                    onSave = viewModel::saveCheckIn,
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

    if (state.pendingMomentForId != null) {
        ModalBottomSheet(
            onDismissRequest = {
                discardMomentAttachments()
                viewModel.dismissMoment()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Panel,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                Text(stringResource(R.string.moment_title), color = Cream, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    when {
                        state.momentPromptDirection > 0 -> stringResource(R.string.moment_prompt_up)
                        state.momentPromptDirection < 0 -> stringResource(R.string.moment_prompt_down)
                        else -> stringResource(R.string.moment_prompt_same)
                    },
                    color = Muted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                TextField(value = momentDraft, onValueChange = { momentDraft = it }, placeholder = { Text(stringResource(R.string.moment_placeholder), color = Muted) }, modifier = Modifier.fillMaxWidth(), minLines = 3, colors = TextFieldDefaults.colors(focusedContainerColor = PanelLight, unfocusedContainerColor = PanelLight, focusedTextColor = Cream, unfocusedTextColor = Cream, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                Spacer(Modifier.height(12.dp))
                AttachmentSelectorBar(
                    imageUris = momentImageUris,
                    onAddImages = { momentImageUris = momentImageUris + it },
                    onRemoveImage = { uri ->
                        deleteInternalAttachment(context, uri)
                        momentImageUris = momentImageUris.filter { it != uri }
                    },
                    audioUri = momentAudioUri,
                    audioDurationMs = momentAudioDurationMs,
                    onAudioRecorded = { uri, duration ->
                        momentAudioUri = uri
                        momentAudioDurationMs = duration
                    },
                    onRemoveAudio = {
                        deleteInternalAttachment(context, momentAudioUri)
                        momentAudioUri = null
                        momentAudioDurationMs = 0L
                    },
                )
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        discardMomentAttachments()
                        viewModel.dismissMoment()
                    }) { Text(stringResource(R.string.skip), color = Muted) }
                    Button(
                        onClick = { viewModel.saveMoment(momentDraft, momentImageUris, momentAudioUri, momentAudioDurationMs) },
                        enabled = !state.isSavingMoment,
                        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Ink),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.save_moment))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
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
    onMoodValueChange: (Float) -> Unit,
    onEnergyLevelChange: (Int) -> Unit,
    onSave: () -> Unit,
) {
    SectionLabel(stringResource(R.string.mood_scale))
    Spacer(Modifier.height(6.dp))
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(moodLabel(state.moodValue), color = Cream, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text("${state.moodValue}/100", color = Muted, fontSize = 12.sp)
            }
            Slider(
                value = state.moodValue.toFloat(),
                onValueChange = onMoodValueChange,
                valueRange = 0f..100f,
                colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Coral, activeTrackColor = Coral, inactiveTrackColor = PanelLight),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.mood_low_end), color = Muted, fontSize = 11.sp)
                Text(stringResource(R.string.mood_high_end), color = Muted, fontSize = 11.sp)
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    SectionLabel(stringResource(R.string.energy_level))
    Spacer(Modifier.height(6.dp))
    EnergyChoices(
        options = listOf(
            EnergyChoiceOption(1, stringResource(R.string.energy_low)),
            EnergyChoiceOption(2, stringResource(R.string.energy_medium)),
            EnergyChoiceOption(3, stringResource(R.string.energy_high)),
        ),
        selectedLevel = state.energyLevel,
        onSelect = onEnergyLevelChange,
    )
    Spacer(Modifier.height(17.dp))
    Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(11.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Ink)) {
        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.save_check_in), fontWeight = FontWeight.Bold)
    }
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
