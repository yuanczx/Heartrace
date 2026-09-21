package me.heartrace.feature.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.feature.today.localDateAsUtcMillis
import me.heartrace.feature.today.withLocalDateFromUtc
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.SegmentedOption
import me.heartrace.ui.components.SlidingSegmentedControl
import me.heartrace.uikit.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDatePager(
    dateStart: Long,
    range: HistoryRange,
    canGoToNextDate: Boolean,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onRangeSelected: (HistoryRange) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val localeTag = LocalConfiguration.current.locales.toLanguageTags()
    val dateLabel = when (range) {
        HistoryRange.DAY -> DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(dateStart))
        HistoryRange.MONTH -> SimpleDateFormat("MMMM yyyy", locale).format(Date(dateStart))
        HistoryRange.YEAR -> SimpleDateFormat("yyyy", locale).format(Date(dateStart))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    Surface(
        color = Panel,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            SlidingSegmentedControl(
                options = listOf(
                    SegmentedOption(HistoryRange.DAY, stringResource(R.string.history_range_day)),
                    SegmentedOption(HistoryRange.MONTH, stringResource(R.string.history_range_month)),
                    SegmentedOption(HistoryRange.YEAR, stringResource(R.string.history_range_year)),
                ),
                selectedOption = range,
                onOptionSelected = onRangeSelected,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPreviousDate,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = stringResource(R.string.history_previous_period),
                        tint = Cream,
                    )
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = stringResource(R.string.history_select_date),
                        tint = Coral,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(dateLabel, color = Cream, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(
                    onClick = onNextDate,
                    enabled = canGoToNextDate,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.history_next_period),
                        tint = if (canGoToNextDate) Cream else Muted.copy(alpha = .35f),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        key(localeTag, dateStart) {
            val todayUtcMillis = remember { localDateAsUtcMillis(System.currentTimeMillis()) }
            val selectableDates = remember(todayUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis
                }
            }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = localDateAsUtcMillis(dateStart),
                selectableDates = selectableDates,
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                                onDateSelected(dateStart.withLocalDateFromUtc(selectedDateMillis))
                            }
                            showDatePicker = false
                        },
                    ) {
                        Text(stringResource(R.string.save), color = Coral, fontWeight = FontWeight.Bold)
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
                            stringResource(R.string.history_select_date),
                            color = Cream,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 12.dp),
                        )
                    },
                )
            }
        }
    }
}
