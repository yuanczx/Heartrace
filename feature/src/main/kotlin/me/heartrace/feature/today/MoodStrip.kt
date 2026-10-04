package me.heartrace.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.heartrace.data.Mood
import me.heartrace.uikit.R

@Composable
fun MoodStrip(selected: Mood, onSelect: ((Mood) -> Unit)? = null) {
    val moods = listOf(
        Mood.DRAINED to stringResource(R.string.mood_drained),
        Mood.LOW to stringResource(R.string.mood_low),
        Mood.QUIET to stringResource(R.string.mood_quiet),
        Mood.STEADY to stringResource(R.string.mood_steady),
        Mood.BRIGHT to stringResource(R.string.mood_bright),
        Mood.RESTLESS to stringResource(R.string.mood_restless),
        Mood.ELECTRIC to stringResource(R.string.mood_electric),
    )
    val row1 = moods.take(4)
    val row2 = moods.drop(4)

    Column(Modifier.padding(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row1.forEach { (mood, label) ->
                    MoodCard(
                        mood = mood,
                        label = label,
                        selected = selected == mood,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row2.forEach { (mood, label) ->
                    MoodCard(
                        mood = mood,
                        label = label,
                        selected = selected == mood,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep the second row aligned with the four-column first row.
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
