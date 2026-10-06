package me.heartrace.feature.today

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.data.toMood
import me.heartrace.data.toMoodValue
import me.heartrace.ui.components.AttachmentSelectorBar
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Ink
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.PanelLight
import me.heartrace.ui.components.SectionLabel
import me.heartrace.uikit.R
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordSheet(
    draft: EditRecordDraft,
    onDraftChange: ((EditRecordDraft) -> EditRecordDraft) -> Unit,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        moodValue: Int,
        energyLevel: Int?,
        momentText: String,
        recordedAt: Long,
        imageUris: List<String>,
        audioUri: String?,
        audioDurationMs: Long,
    ) -> Unit,
    onDelete: () -> Unit,
    errorMessage: String? = null,
) {
    val record = draft.record
    val moodValue = draft.moodValue
    val energyLevel = draft.energyLevel
    val momentText = draft.momentText
    val recordedAt = draft.recordedAt
    val imageUris = draft.imageUris
    val audioUri = draft.audioUri
    val audioDurationMs = draft.audioDurationMs
    val context = LocalContext.current
    val localeTag = LocalConfiguration.current.locales.toLanguageTags()
    var showDatePicker by rememberSaveable(record.id) { mutableStateOf(false) }
    var showTimePicker by rememberSaveable(record.id) { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(record.id) { mutableStateOf(false) }
    var areAttachmentsBusy by remember(record.id) { mutableStateOf(false) }
    val currentIsSaving by rememberUpdatedState(isSaving)

    if (showDatePicker) {
        key(localeTag) {
            val todayUtcMillis = remember { localDateAsUtcMillis(System.currentTimeMillis()) }
            val selectableDates = remember(todayUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis
                }
            }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = localDateAsUtcMillis(recordedAt),
                selectableDates = selectableDates,
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                                onDraftChange { it.copy(recordedAt = it.recordedAt.withLocalDateFromUtc(selectedDateMillis)) }
                            }
                            showDatePicker = false
                        },
                    ) {
                        Text(
                            stringResource(R.string.save),
                            color = Coral,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel), color = Muted)
                    }
                },
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            stringResource(R.string.recorded_at),
                            color = Cream,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 12.dp),
                        )
                    },
                )
            }
        }
    }

    if (showTimePicker) {
        val selected = remember(recordedAt) {
            Calendar.getInstance().apply { timeInMillis = recordedAt }
        }
        key(localeTag) {
            val timePickerState = rememberTimePickerState(
                initialHour = selected.get(Calendar.HOUR_OF_DAY),
                initialMinute = selected.get(Calendar.MINUTE),
                is24Hour = DateFormat.is24HourFormat(context),
            )
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                title = {
                    Text(
                        stringResource(R.string.recorded_at),
                        color = Cream,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val updated = Calendar.getInstance().apply {
                                timeInMillis = recordedAt
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onDraftChange { it.copy(recordedAt = updated.timeInMillis) }
                            showTimePicker = false
                        },
                    ) {
                        Text(
                            stringResource(R.string.save),
                            color = Coral,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(R.string.cancel), color = Muted)
                    }
                },
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = PanelLight,
            title = { Text(stringResource(R.string.delete_record_confirm_title), color = Cream, fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.delete_record_confirm_message), color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) { Text(stringResource(R.string.delete_record), color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.cancel), color = Muted) }
            },
        )
    }

    val locale = LocalConfiguration.current.locales[0]
    val recordedDate = Date(recordedAt)
    val dateLabel = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale).format(recordedDate)
    val timeLabel = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale).format(recordedDate)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !currentIsSaving || it != SheetValue.Hidden }),
        containerColor = Panel,
        contentWindowInsets = { WindowInsets(0.dp) },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.edit_record),
                            color = Cream,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = { showDeleteConfirmation = true }, enabled = !isSaving) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = Coral,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.delete_record),
                                color = Coral,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SectionLabel(stringResource(R.string.recorded_at))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDatePicker = true }, enabled = !isSaving) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = Cream,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                dateLabel,
                                color = Cream,
                                fontSize = 13.sp,
                            )
                        }
                        Text("·", color = Muted, fontSize = 13.sp)
                        TextButton(onClick = { showTimePicker = true }, enabled = !isSaving) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Cream,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                timeLabel,
                                color = Cream,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SectionLabel(stringResource(R.string.mood_scale))
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = Panel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MoodStrip(
                            selected = moodValue.toMood(),
                            onSelect = if (isSaving) null else { mood -> onDraftChange { it.copy(moodValue = mood.toMoodValue()) } },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    SectionLabel(stringResource(R.string.energy_level))
                    Spacer(Modifier.height(6.dp))
                    EnergyChoices(
                        options = listOf(
                            EnergyChoiceOption(1, stringResource(R.string.energy_low)),
                            EnergyChoiceOption(2, stringResource(R.string.energy_medium)),
                            EnergyChoiceOption(3, stringResource(R.string.energy_high)),
                        ),
                        selectedLevel = energyLevel ?: 0,
                        onSelect = { level -> onDraftChange { it.copy(energyLevel = level) } },
                        enabled = !isSaving,
                    )
                    Spacer(Modifier.height(14.dp))
                    SectionLabel(stringResource(R.string.daily_note))
                    Spacer(Modifier.height(6.dp))
                    TextField(
                        value = momentText,
                        onValueChange = { text -> onDraftChange { it.copy(momentText = text) } },
                        enabled = !isSaving,
                        placeholder = { Text(stringResource(R.string.note_placeholder), color = Muted) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PanelLight,
                            unfocusedContainerColor = PanelLight,
                            focusedTextColor = Cream,
                            unfocusedTextColor = Cream,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    key(record.id) {
                        AttachmentSelectorBar(
                            imageUris = imageUris,
                            onAddImages = { uris -> onDraftChange { it.copy(imageUris = it.imageUris + uris) } },
                            onRemoveImage = { uri -> onDraftChange { it.copy(imageUris = it.imageUris - uri) } },
                            audioUri = audioUri,
                            audioDurationMs = audioDurationMs,
                            onAudioRecorded = { uri, duration -> onDraftChange { it.copy(audioUri = uri, audioDurationMs = duration) } },
                            onRemoveAudio = { onDraftChange { it.copy(audioUri = null, audioDurationMs = 0L) } },
                            enabled = !isSaving,
                            onBusyChanged = { areAttachmentsBusy = it },
                        )
                    }
                    errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.save_failed), color = Coral, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isSaving) {
                            Text(stringResource(R.string.cancel), color = Muted)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = !isSaving && !areAttachmentsBusy,
                            onClick = {
                                onSave(
                                    moodValue,
                                    energyLevel,
                                    momentText,
                                    recordedAt,
                                    imageUris,
                                    audioUri,
                                    audioDurationMs,
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Ink),
                        ) {
                            Text(
                                stringResource(R.string.save),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}
