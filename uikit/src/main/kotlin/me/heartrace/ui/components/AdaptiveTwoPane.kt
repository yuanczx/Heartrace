package me.heartrace.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveTwoPane(
    modifier: Modifier = Modifier,
    minTwoPaneWidth: Dp = 760.dp,
    paneSpacing: Dp = 28.dp,
    compactSpacing: Dp = 24.dp,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth >= minTwoPaneWidth) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), content = first)
                Spacer(Modifier.width(paneSpacing))
                Column(Modifier.weight(1f), content = second)
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                first()
                Spacer(Modifier.height(compactSpacing))
                second()
            }
        }
    }
}
