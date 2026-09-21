package me.heartrace.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.heartrace.data.Mood
import me.heartrace.ui.theme.HeartraceTheme

@Composable
fun moodAccent(mood: Mood): Color = when (mood) {
    Mood.DRAINED -> HeartraceTheme.colors.muted
    Mood.LOW -> HeartraceTheme.colors.sky
    Mood.QUIET -> HeartraceTheme.colors.mint
    Mood.STEADY -> HeartraceTheme.colors.lemon
    Mood.BRIGHT -> HeartraceTheme.colors.coral
    Mood.RESTLESS -> HeartraceTheme.colors.violet
    Mood.ELECTRIC -> HeartraceTheme.colors.amber
}
