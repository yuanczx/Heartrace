package me.heartrace.feature.patterns

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.Mint
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.uikit.R

@Composable
fun DailyTrendSection(summaries: List<DailyRecordSummary>) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.daily_trends), color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.daily_trends_subtitle), color = Cream, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
        Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                DailyTrendChart(summaries)
                Spacer(Modifier.height(8.dp))
                if (summaries.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (summaries.size == 1) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(summaries.first().dateLabel, color = Muted.copy(alpha = .85f), fontSize = 10.sp)
                            }
                        } else {
                            val displayLabels = if (summaries.size <= 7) {
                                summaries.map { it.dateLabel }
                            } else {
                                listOf(
                                    summaries.first().dateLabel,
                                    summaries[summaries.size / 2].dateLabel,
                                    summaries.last().dateLabel,
                                )
                            }
                            displayLabels.forEach { label -> Text(label, color = Muted.copy(alpha = .85f), fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
    }
}
