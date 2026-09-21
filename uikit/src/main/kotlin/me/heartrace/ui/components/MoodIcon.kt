package me.heartrace.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import me.heartrace.data.Mood

fun moodIcon(mood: Mood): ImageVector = when (mood) {
    Mood.DRAINED -> Icons.Rounded.ArrowDownward
    Mood.LOW -> Icons.Rounded.Remove
    Mood.QUIET -> Icons.Rounded.Circle
    Mood.STEADY -> Icons.Rounded.Waves
    Mood.BRIGHT -> Icons.Rounded.WbSunny
    Mood.RESTLESS -> Icons.Rounded.PriorityHigh
    Mood.ELECTRIC -> Icons.Rounded.Bolt
}
