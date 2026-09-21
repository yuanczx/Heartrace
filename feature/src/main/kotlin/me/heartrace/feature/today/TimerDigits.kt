package me.heartrace.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

@Suppress("Unused")
@Composable
private fun TimerDigits(value: Int, label: String) {
    var prevValue by remember { mutableIntStateOf(value) }
    var isDecreasing by remember { mutableStateOf(true) }

    if (value != prevValue) {
        isDecreasing = if ((prevValue == 0) && (value >= 50)) {
            true
        } else if ((prevValue >= 50) && (value == 0)) {
            false
        } else {
            value < prevValue
        }
        prevValue = value
    }

    val tens = (value / 10) % 10
    val units = value % 10

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        RollingDigit(digit = tens, isDecreasing = isDecreasing, label = "$label-tens")
        RollingDigit(digit = units, isDecreasing = isDecreasing, label = "$label-units")
    }
}
