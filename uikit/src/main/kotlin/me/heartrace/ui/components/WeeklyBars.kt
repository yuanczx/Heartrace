package me.heartrace.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.ui.theme.HeartraceTheme

@Composable
fun WeeklyBars(values: List<Int>) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxValue = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val colors = HeartraceTheme.colors
    Row(
        Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.take(7).forEachIndexed { index, value ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val barHeight = if (value == 0) 6 else (110f * value / maxValue).coerceAtLeast(8f).toInt()
                val animatedHeight by animateDpAsState(barHeight.dp, label = "weekly bar $index")
                Box(
                    Modifier.width(24.dp).height(animatedHeight)
                        .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                        .background(if (index == 6) colors.coral else colors.panelLight),
                )
                Spacer(Modifier.height(8.dp))
                Text(labels[index], color = colors.muted, fontSize = 10.sp)
            }
        }
    }
}
