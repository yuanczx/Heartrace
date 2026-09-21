package me.heartrace.feature.today

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.heartrace.data.MoodRecord
import me.heartrace.ui.components.Coral
import me.heartrace.ui.components.Cream
import me.heartrace.ui.components.ImageAttachmentsView
import me.heartrace.ui.components.Lemon
import me.heartrace.ui.components.Muted
import me.heartrace.ui.components.Panel
import me.heartrace.ui.components.VoicePlayerView
import me.heartrace.uikit.R
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodRecordRow(
    record: MoodRecord,
    showDate: Boolean = false,
    onLongClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val recordedDate = Date(record.recordedAt)

    val timeText = if (showDate) {
        val dateLabel = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale).format(recordedDate)
        val timeLabel = DateFormat.getTimeFormat(context).format(recordedDate)
        "$dateLabel $timeLabel"
    } else {
        DateFormat.getTimeFormat(context).format(recordedDate)
    }

    val shape = RoundedCornerShape(12.dp)
    Surface(
        color = Panel,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(shape)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = {},
            ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .background(
                                when (record.energyLevel) {
                                    3 -> Coral
                                    2 -> Lemon
                                    else -> Muted
                                },
                                RoundedCornerShape(50),
                            ),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        moodLabel(record.moodValue),
                        color = Cream,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    timeText,
                    color = Muted,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${stringResource(R.string.energy_label)} ${energyLabel(record.energyLevel)} · ${record.moodValue}/100",
                color = Muted,
                fontSize = 12.sp,
            )
            record.momentText?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"$it\"",
                    color = Cream.copy(alpha = .86f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            if (record.imageUris.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ImageAttachmentsView(imageUris = record.imageUris)
            }
            record.audioUri?.takeIf { it.isNotBlank() }?.let { uri ->
                Spacer(Modifier.height(8.dp))
                VoicePlayerView(audioUri = uri, durationMs = record.audioDurationMs)
            }
        }
    }
}
