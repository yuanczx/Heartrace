package me.heartrace.feature.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.max
import kotlin.math.min

/**
 * Builds a cubic Bezier path through [points] using a Catmull-Rom style
 * tangent at each point. Control points are constrained to their segment's
 * horizontal bounds so unevenly spaced chart data cannot make the curve fold
 * back on itself.
 */
internal fun smoothCubicPath(points: List<Offset>): Path {
    return Path().apply {
        if (points.isEmpty()) return@apply

        moveTo(points.first().x, points.first().y)
        if (points.size == 1) return@apply

        points.zipWithNext().forEachIndexed { index, (start, end) ->
            val previous = points.getOrNull(index - 1) ?: start
            val next = points.getOrNull(index + 2) ?: end
            val segmentMinX = min(start.x, end.x)
            val segmentMaxX = max(start.x, end.x)

            val firstControl = Offset(
                x = (start.x + (end.x - previous.x) / 6f).coerceIn(segmentMinX, segmentMaxX),
                y = start.y + (end.y - previous.y) / 6f,
            )
            val secondControl = Offset(
                x = (end.x - (next.x - start.x) / 6f).coerceIn(segmentMinX, segmentMaxX),
                y = end.y - (next.y - start.y) / 6f,
            )
            cubicTo(
                firstControl.x,
                firstControl.y,
                secondControl.x,
                secondControl.y,
                end.x,
                end.y,
            )
        }
    }
}
