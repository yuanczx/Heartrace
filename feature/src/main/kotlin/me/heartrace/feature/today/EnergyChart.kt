package me.heartrace.feature.today

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
import me.heartrace.data.MoodRecord
import me.heartrace.ui.theme.HeartraceTheme

@Composable
internal fun EnergyChart(
    orderedRecords: List<MoodRecord>,
    chartStart: Long,
    chartEnd: Long,
) {
    val colors = HeartraceTheme.colors
    val coralColor = colors.coral
    val mintColor = colors.mint
    val inkColor = colors.ink
    val panelLightColor = colors.panelLight
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(110.dp),
    ) {
        val xPadding = 16.dp.toPx()
        val yPadding = 12.dp.toPx()
        val usableWidth = (size.width - 2 * xPadding).coerceAtLeast(1f)
        val usableHeight = (size.height - 2 * yPadding).coerceAtLeast(1f)

        for (line in 0..3) {
            val y = yPadding + usableHeight * line / 3f
            drawLine(panelLightColor.copy(alpha = .6f), Offset(0f, y), Offset(size.width, y), 1f)
        }
        if (orderedRecords.isNotEmpty()) {
            val chartLength = (chartEnd - chartStart).toFloat()

            val energyCoords = orderedRecords.map { record ->
                val timeRatio = if (chartLength <= 0f) 0.5f else ((record.recordedAt - chartStart).toFloat() / chartLength).coerceIn(0f, 1f)
                val energyRatio = when (record.energyLevel) {
                    1 -> 0.18f
                    2 -> 0.50f
                    else -> 0.85f
                }
                Offset(xPadding + usableWidth * timeRatio, yPadding + usableHeight * (1f - energyRatio))
            }

            val moodCoords = orderedRecords.map { record ->
                val timeRatio = if (chartLength <= 0f) 0.5f else ((record.recordedAt - chartStart).toFloat() / chartLength).coerceIn(0f, 1f)
                val moodRatio = (record.moodValue / 100f).coerceIn(0f, 1f)
                Offset(xPadding + usableWidth * timeRatio, yPadding + usableHeight * (1f - moodRatio))
            }

            if (orderedRecords.size == 1) {
                val ep = energyCoords.first()
                val mp = moodCoords.first()
                drawLine(mintColor.copy(alpha = 0.25f), Offset(xPadding, ep.y), Offset(size.width - xPadding, ep.y), 1.5f)
                drawLine(coralColor.copy(alpha = 0.25f), Offset(xPadding, mp.y), Offset(size.width - xPadding, mp.y), 1.5f)
            }

            if (energyCoords.size > 1) {
                val area = Path().apply {
                    moveTo(energyCoords.first().x, yPadding + usableHeight)
                    energyCoords.forEach { lineTo(it.x, it.y) }
                    lineTo(energyCoords.last().x, yPadding + usableHeight)
                    close()
                }
                drawPath(area, color = mintColor.copy(alpha = .08f))
                val linePath = Path().apply {
                    moveTo(energyCoords.first().x, energyCoords.first().y)
                    energyCoords.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    linePath,
                    color = mintColor,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )
            }
            energyCoords.forEachIndexed { index, point ->
                val isLatest = index == energyCoords.lastIndex
                drawCircle(
                    if (isLatest) mintColor else mintColor.copy(alpha = .8f),
                    if (isLatest) 5.5f else 3.5f,
                    point,
                )
                if (isLatest) drawCircle(inkColor, 2f, point)
            }

            if (moodCoords.size > 1) {
                val area = Path().apply {
                    moveTo(moodCoords.first().x, yPadding + usableHeight)
                    moodCoords.forEach { lineTo(it.x, it.y) }
                    lineTo(moodCoords.last().x, yPadding + usableHeight)
                    close()
                }
                drawPath(area, color = coralColor.copy(alpha = .10f))
                val linePath = Path().apply {
                    moveTo(moodCoords.first().x, moodCoords.first().y)
                    moodCoords.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    linePath,
                    color = coralColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
            }
            moodCoords.forEachIndexed { index, point ->
                val isLatest = index == moodCoords.lastIndex
                drawCircle(
                    if (isLatest) coralColor else coralColor.copy(alpha = .85f),
                    if (isLatest) 6f else 4f,
                    point,
                )
                if (isLatest) drawCircle(inkColor, 2f, point)
            }
        }
    }
}
