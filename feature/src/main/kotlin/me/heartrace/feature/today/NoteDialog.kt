package me.heartrace.feature.today

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.data.Mood
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.PanelLight
import me.heartrace.uikit.R
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDialog(
    draft: String,
    onDraftChange: (String) -> Unit,
    mood: Mood,
    onMoodChange: (Mood) -> Unit,
    recordedAt: Long,
    onRecordedAtChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    saveError: String? = null,
) {
    val context = LocalContext.current
    val localeTag = LocalConfiguration.current.locales.toLanguageTags()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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
                                onRecordedAtChange(recordedAt.withLocalDateFromUtc(selectedDateMillis))
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
                            onRecordedAtChange(updated.timeInMillis)
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

    val locale = LocalConfiguration.current.locales[0]
    val recordedDate = Date(recordedAt)
    val dateLabel = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale).format(recordedDate)
    val timeLabel = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale).format(recordedDate)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelLight,
        title = { Text(stringResource(R.string.capture_moment), color = Cream, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MoodStrip(mood, onMoodChange)
                Spacer(Modifier.height(15.dp))
                Text(stringResource(R.string.recorded_at), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDatePicker = true }) {
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
                    TextButton(onClick = { showTimePicker = true }) {
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
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text(stringResource(R.string.note_placeholder), color = Muted) },
                    minLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Panel,
                        unfocusedContainerColor = Panel,
                        focusedTextColor = Cream,
                        unfocusedTextColor = Cream,
                        cursorColor = Coral,
                        focusedIndicatorColor = Coral,
                        unfocusedIndicatorColor = Muted.copy(alpha = .25f),
                    ),
                )
                saveError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Coral, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = saveEnabled) {
                Text(
                    stringResource(R.string.save),
                    color = Coral,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Muted) } },
    )
}

internal fun localDateAsUtcMillis(timestamp: Long): Long {
    val localDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localDate.get(Calendar.YEAR),
            localDate.get(Calendar.MONTH),
            localDate.get(Calendar.DAY_OF_MONTH),
        )
    }.timeInMillis
}

internal fun Long.withLocalDateFromUtc(utcDateMillis: Long): Long {
    val selectedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    return Calendar.getInstance().apply {
        timeInMillis = this@withLocalDateFromUtc
        set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
        set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
