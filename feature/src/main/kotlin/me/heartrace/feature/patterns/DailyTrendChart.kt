package me.heartrace.feature.patterns

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.heartrace.feature.charts.smoothCubicPath
import me.heartrace.ui.theme.HeartraceTheme
import me.heartrace.uikit.R

@Composable
internal fun DailyTrendChart(summaries: List<DailyRecordSummary>) {
    val colors = HeartraceTheme.colors
    val coralColor = colors.coral
    val mintColor = colors.mint
    val inkColor = colors.ink
    val panelLightColor = colors.panelLight
    val chartDescription = stringResource(R.string.daily_trends_accessibility)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
            .semantics { contentDescription = chartDescription },
    ) {
        val xPadding = 16.dp.toPx()
        val yPadding = 12.dp.toPx()
        val usableWidth = (size.width - 2 * xPadding).coerceAtLeast(1f)
        val usableHeight = (size.height - 2 * yPadding).coerceAtLeast(1f)
        for (line in 0..3) {
            val y = yPadding + usableHeight * line / 3f
            drawLine(panelLightColor.copy(alpha = .6f), Offset(0f, y), Offset(size.width, y), 1f)
        }
        if (summaries.isNotEmpty()) {
            val count = summaries.size
            val energyCoords = summaries.mapIndexedNotNull { index, summary ->
                val energy = summary.averageEnergy ?: return@mapIndexedNotNull null
                val x = if (count == 1) xPadding + usableWidth / 2f else xPadding + usableWidth * (index.toFloat() / (count - 1))
                Offset(x, yPadding + usableHeight * (1f - (energy / 3f).coerceIn(0f, 1f)))
            }
            val moodCoords = summaries.mapIndexed { index, summary ->
                val x = if (count == 1) xPadding + usableWidth / 2f else xPadding + usableWidth * (index.toFloat() / (count - 1))
                Offset(x, yPadding + usableHeight * (1f - (summary.averageMood / 100f).coerceIn(0f, 1f)))
            }
            if (count == 1 && energyCoords.isNotEmpty()) {
                val ep = energyCoords.first()
                val mp = moodCoords.first()
                drawLine(mintColor.copy(alpha = 0.25f), Offset(xPadding, ep.y), Offset(size.width - xPadding, ep.y), 1.5f)
                drawLine(coralColor.copy(alpha = 0.25f), Offset(xPadding, mp.y), Offset(size.width - xPadding, mp.y), 1.5f)
            }
            if (count > 1 && energyCoords.size > 1) {
                val energyArea = smoothCubicPath(energyCoords).apply {
                    lineTo(energyCoords.last().x, yPadding + usableHeight)
                    lineTo(energyCoords.first().x, yPadding + usableHeight)
                    close()
                }
                drawPath(energyArea, color = mintColor.copy(alpha = .08f))
                val energyLine = smoothCubicPath(energyCoords)
                drawPath(energyLine, color = mintColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                val moodArea = smoothCubicPath(moodCoords).apply {
                    lineTo(moodCoords.last().x, yPadding + usableHeight)
                    lineTo(moodCoords.first().x, yPadding + usableHeight)
                    close()
                }
                drawPath(moodArea, color = coralColor.copy(alpha = .10f))
                val moodLine = smoothCubicPath(moodCoords)
                drawPath(moodLine, color = coralColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
            energyCoords.forEachIndexed { index, point ->
                val isLatest = index == energyCoords.lastIndex
                drawCircle(if (isLatest) mintColor else mintColor.copy(alpha = .8f), if (isLatest) 5.5f else 3.5f, point)
                if (isLatest) drawCircle(inkColor, 2f, point)
            }
            moodCoords.forEachIndexed { index, point ->
                val isLatest = index == moodCoords.lastIndex
                drawCircle(if (isLatest) coralColor else coralColor.copy(alpha = .85f), if (isLatest) 6f else 4f, point)
                if (isLatest) drawCircle(inkColor, 2f, point)
            }
        }
    }
}
