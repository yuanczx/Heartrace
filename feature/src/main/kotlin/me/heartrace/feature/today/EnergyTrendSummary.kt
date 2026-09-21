package me.heartrace.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.heartrace.data.MoodRecord
import me.heartrace.uikit.R

@Composable
internal fun energyTrendSummary(records: List<MoodRecord>): String {
    if (records.isEmpty()) return stringResource(R.string.no_energy_data)
    if (records.size == 1) return stringResource(R.string.first_check_in)

    val sorted = records.sortedBy { it.recordedAt }
    val first = sorted.first()
    val last = sorted.last()
    val moodDelta = last.moodValue - first.moodValue
    val energyDelta = last.energyLevel - first.energyLevel

    if (sorted.size >= 3) {
        var directionChanges = 0
        for (i in 1 until sorted.size - 1) {
            val prevDelta = sorted[i].moodValue - sorted[i - 1].moodValue
            val nextDelta = sorted[i + 1].moodValue - sorted[i].moodValue
            if ((prevDelta > 3 && nextDelta < -3) || (prevDelta < -3 && nextDelta > 3)) {
                directionChanges++
            }
        }
        if (directionChanges >= 1) {
            return stringResource(R.string.variable_rhythm)
        }
    }

    return when {
        moodDelta > 6 || (moodDelta >= 0 && energyDelta > 0) -> stringResource(R.string.gentle_climb)
        moodDelta < -6 || (moodDelta <= 0 && energyDelta < 0) -> stringResource(R.string.gentle_dip)
        else -> stringResource(R.string.steady_flow)
    }
}
