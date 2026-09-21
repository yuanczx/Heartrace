package me.heartrace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T> SlidingSegmentedControl(
    options: List<SegmentedOption<T>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = remember(options, selectedOption) {
        options.indexOfFirst { it.value == selectedOption }.coerceAtLeast(0)
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(10.dp))
            .background(PanelLight).padding(5.dp),
    ) {
        val count = options.size
        if (count > 0) {
            val itemWidth = maxWidth / count
            val targetOffsetX = itemWidth * selectedIndex
            val animatedOffsetX by animateDpAsState(
                targetValue = targetOffsetX,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
                label = "pill_offset",
            )
            Box(
                Modifier.offset { IntOffset(animatedOffsetX.roundToPx(), 0) }.width(itemWidth)
                    .fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Coral),
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { index, option ->
                    val isSelected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Ink else Cream,
                        animationSpec = tween(durationMillis = 200),
                        label = "text_color_$index",
                    )
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onOptionSelected(option.value) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option.label, color = textColor, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
