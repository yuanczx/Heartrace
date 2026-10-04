package me.heartrace.feature.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.ui.theme.HeartraceTheme
import me.heartrace.uikit.R

data class EnergyChoiceOption(
    val level: Int,
    val label: String,
)

@Composable
fun EnergyChoices(
    options: List<EnergyChoiceOption>,
    selectedLevel: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HeartraceTheme.colors
    val selectedIndex = options.indexOfFirst { it.level == selectedLevel }
    val visualIndex = selectedIndex.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.panelLight)
            .padding(5.dp),
    ) {
        if (options.isNotEmpty()) {
            val itemWidth = maxWidth / options.size
            val animatedOffset by animateDpAsState(
                targetValue = itemWidth * visualIndex,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = 0.8f,
                ),
                label = "energy_choice_offset",
            )
            val selectedBackground by animateColorAsState(
                targetValue = energyChoiceBackground(options[visualIndex].level, colors),
                label = "energy_choice_background",
            )
            if (selectedIndex >= 0) {
                Box(
                    Modifier
                        .offset { IntOffset(animatedOffset.roundToPx(), 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedBackground),
                )
            }
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { index, option ->
                    val isSelected = index == selectedIndex
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) colors.ink else colors.muted,
                        animationSpec = tween(durationMillis = 200),
                        label = "energy_choice_icon_$index",
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) colors.ink else colors.cream,
                        animationSpec = tween(durationMillis = 200),
                        label = "energy_choice_label_$index",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(option.level) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                option.label,
                                color = labelColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun energyChoiceBackground(level: Int, colors: me.heartrace.ui.theme.HeartracePalette) = when (level) {
    1 -> colors.muted
    3 -> colors.coral
    else -> colors.lemon
}

@Composable
fun energyLabel(level: Int?): String = when (level) {
    null -> stringResource(R.string.energy_not_recorded)
    1 -> stringResource(R.string.energy_low)
    3 -> stringResource(R.string.energy_high)
    else -> stringResource(R.string.energy_medium)
}
