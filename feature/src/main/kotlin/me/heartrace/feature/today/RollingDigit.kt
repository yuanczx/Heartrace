package me.heartrace.feature.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.ui.components.Ink

@Composable
internal fun RollingDigit(
    digit: Int,
    isDecreasing: Boolean,
    label: String,
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            if (isDecreasing) {
                (
                    slideInVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
                        fadeIn(animationSpec = tween(180))
                    ) togetherWith
                    (
                        slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } +
                            fadeOut(animationSpec = tween(180))
                        )
            } else {
                (
                    slideInVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } +
                        fadeIn(animationSpec = tween(180))
                    ) togetherWith
                    (
                        slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
                            fadeOut(animationSpec = tween(180))
                        )
            }
        },
        label = label,
        modifier = Modifier.clipToBounds(),
    ) { targetDigit ->
        Text(
            text = targetDigit.toString(),
            color = Ink,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
    }
}
