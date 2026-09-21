package me.heartrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.uikit.R

@Composable
internal fun RecordingControls(
    durationSeconds: Long,
    onCancel: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(color = PanelLight, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(Coral, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.recording_voice, durationSeconds),
                    color = Cream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted),
                    modifier = Modifier.height(34.dp),
                ) { Text(stringResource(R.string.cancel_recording), fontSize = 12.sp) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Ink),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.stop_recording), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
