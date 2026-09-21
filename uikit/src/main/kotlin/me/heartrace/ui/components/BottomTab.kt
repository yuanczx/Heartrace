package me.heartrace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BottomTab(
    label: String,
    unselectedIcon: ImageVector,
    selectedIcon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        if (active) Cream else Muted.copy(alpha = .55f),
        label = "bottom tab color $label",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val iconScale by animateFloatAsState(
        targetValue = when {
            isPressed -> .94f
            active -> 1.06f
            else -> 1f
        },
        label = "bottom tab scale $label",
    )
    Box(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(52.dp).height(28.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (active) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp).graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(label, color = color, fontSize = 9.sp)
        }
    }
}
