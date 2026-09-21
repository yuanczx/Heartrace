package me.heartrace.feature.today

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.data.MoodRecord
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Mint
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.uikit.R
import java.util.Calendar
import java.util.Date

@Composable
fun EnergySection(records: List<MoodRecord>) {
    val context = LocalContext.current
    val orderedRecords = remember(records) { records.sortedBy { it.recordedAt } }
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }

    val dayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val (chartStart, chartEnd) = remember(orderedRecords, dayStart) {
        if (orderedRecords.isEmpty()) {
            Pair(dayStart + 6 * 3600 * 1000L, dayStart + 21 * 3600 * 1000L)
        } else if (orderedRecords.size == 1) {
            val t = orderedRecords.first().recordedAt
            Pair(t, t)
        } else {
            Pair(orderedRecords.first().recordedAt, orderedRecords.last().recordedAt)
        }
    }

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.energy_and_mood),
                    color = Muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    energyTrendSummary(records),
                    color = Cream,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Coral, RoundedCornerShape(50)))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.mood_title), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(8.dp).background(Mint, RoundedCornerShape(50)))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.energy_label), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            color = Panel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                EnergyChart(orderedRecords, chartStart, chartEnd)
                Spacer(Modifier.height(8.dp))
                if (orderedRecords.isEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(
                            stringResource(R.string.energy_chart_6am),
                            stringResource(R.string.energy_chart_12pm),
                            stringResource(R.string.energy_chart_9pm),
                        ).forEach {
                            Text(it, color = Muted.copy(alpha = .75f), fontSize = 9.sp)
                        }
                    }
                } else if (chartStart == chartEnd) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            timeFormat.format(Date(chartStart)),
                            color = Muted.copy(alpha = .85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            timeFormat.format(Date(chartStart)),
                            color = Muted.copy(alpha = .85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            timeFormat.format(Date(chartStart + (chartEnd - chartStart) / 2)),
                            color = Muted.copy(alpha = .75f),
                            fontSize = 10.sp,
                        )
                        Text(
                            timeFormat.format(Date(chartEnd)),
                            color = Muted.copy(alpha = .85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
