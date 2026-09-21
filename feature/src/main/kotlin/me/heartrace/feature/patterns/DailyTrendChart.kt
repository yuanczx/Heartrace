package me.heartrace.feature.patterns

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import me.heartrace.ui.theme.HeartraceTheme

@Composable
internal fun DailyTrendChart(summaries: List<DailyRecordSummary>) {
    val colors = HeartraceTheme.colors
    val coralColor = colors.coral
    val mintColor = colors.mint
    val inkColor = colors.ink
    val panelLightColor = colors.panelLight
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
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
            val energyCoords = summaries.mapIndexed { index, summary ->
                val x = if (count == 1) xPadding + usableWidth / 2f else xPadding + usableWidth * (index.toFloat() / (count - 1))
                Offset(x, yPadding + usableHeight * (1f - (summary.averageEnergy / 3f).coerceIn(0f, 1f)))
            }
            val moodCoords = summaries.mapIndexed { index, summary ->
                val x = if (count == 1) xPadding + usableWidth / 2f else xPadding + usableWidth * (index.toFloat() / (count - 1))
                Offset(x, yPadding + usableHeight * (1f - (summary.averageMood / 100f).coerceIn(0f, 1f)))
            }
            if (count == 1) {
                val ep = energyCoords.first()
                val mp = moodCoords.first()
                drawLine(mintColor.copy(alpha = 0.25f), Offset(xPadding, ep.y), Offset(size.width - xPadding, ep.y), 1.5f)
                drawLine(coralColor.copy(alpha = 0.25f), Offset(xPadding, mp.y), Offset(size.width - xPadding, mp.y), 1.5f)
            }
            if (count > 1) {
                val energyArea = Path().apply {
                    moveTo(energyCoords.first().x, yPadding + usableHeight)
                    energyCoords.forEach { lineTo(it.x, it.y) }
                    lineTo(energyCoords.last().x, yPadding + usableHeight)
                    close()
                }
                drawPath(energyArea, color = mintColor.copy(alpha = .08f))
                val energyLine = Path().apply {
                    moveTo(energyCoords.first().x, energyCoords.first().y)
                    energyCoords.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(energyLine, color = mintColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                val moodArea = Path().apply {
                    moveTo(moodCoords.first().x, yPadding + usableHeight)
                    moodCoords.forEach { lineTo(it.x, it.y) }
                    lineTo(moodCoords.last().x, yPadding + usableHeight)
                    close()
                }
                drawPath(moodArea, color = coralColor.copy(alpha = .10f))
                val moodLine = Path().apply {
                    moveTo(moodCoords.first().x, moodCoords.first().y)
                    moodCoords.drop(1).forEach { lineTo(it.x, it.y) }
                }
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
