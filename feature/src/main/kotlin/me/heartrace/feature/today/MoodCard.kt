package me.heartrace.feature.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.data.Mood
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.moodAccent
import me.heartrace.ui.components.moodIcon
import me.heartrace.ui.theme.HeartracePalette
import me.heartrace.ui.theme.HeartraceTheme
import me.heartrace.uikit.R

@Composable
internal fun MoodCard(
    mood: Mood,
    label: String,
    selected: Boolean,
    onSelect: ((Mood) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val accent = moodAccent(mood)
    val contentColor = moodContentColor(mood, HeartraceTheme.colors)
    val background by animateColorAsState(
        if (selected) accent else Panel,
        label = "mood background $label",
    )
    val iconColor by animateColorAsState(
        if (selected) contentColor else accent,
        label = "mood icon $label",
    )
    val labelColor by animateColorAsState(
        if (selected) contentColor.copy(alpha = .88f) else Muted,
        label = "mood label $label",
    )
    val scale by animateFloatAsState(
        if (selected) 1.04f else 1f,
        label = "mood scale $label",
    )
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(enabled = onSelect != null) { onSelect?.invoke(mood) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = moodIcon(mood),
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun moodContentColor(mood: Mood, palette: HeartracePalette): Color = when (mood) {
    Mood.LOW, Mood.RESTLESS -> palette.cream
    else -> palette.ink
}

@Composable
fun moodLabel(value: Int): String = when {
    value < 18 -> stringResource(R.string.mood_drained)
    value < 33 -> stringResource(R.string.mood_low)
    value < 48 -> stringResource(R.string.mood_quiet)
    value < 64 -> stringResource(R.string.mood_steady)
    value < 80 -> stringResource(R.string.mood_bright)
    value < 88 -> stringResource(R.string.mood_restless)
    else -> stringResource(R.string.mood_electric)
}
