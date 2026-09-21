package me.heartrace.ui.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.heartrace.uikit.R

@Composable
fun VoicePlayerView(
    audioUri: String,
    durationMs: Long,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember(durationMs) { mutableLongStateOf(durationMs) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    val currentMediaPlayer by rememberUpdatedState(mediaPlayer)

    DisposableEffect(audioUri) {
        onDispose {
            currentMediaPlayer?.let { player ->
                runCatching { player.stop() }
                runCatching { player.release() }
            }
            mediaPlayer = null
            isPreparing = false
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPositionMs = player.currentPosition.toLong()
                } else {
                    isPlaying = false
                    currentPositionMs = 0L
                }
            }
            delay(100)
        }
    }

    Surface(color = PanelLight, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Coral,
                shape = CircleShape,
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else if (!isPreparing) {
                        try {
                            if (mediaPlayer == null) {
                                val player = MediaPlayer()
                                player.setDataSource(context, Uri.parse(audioUri))
                                player.setOnPreparedListener { prepared ->
                                    isPreparing = false
                                    if (totalDurationMs <= 0) totalDurationMs = prepared.duration.toLong()
                                    prepared.start()
                                    isPlaying = true
                                }
                                player.setOnCompletionListener {
                                    isPlaying = false
                                    currentPositionMs = 0L
                                }
                                player.setOnErrorListener { failedPlayer, _, _ ->
                                    isPreparing = false
                                    isPlaying = false
                                    mediaPlayer = null
                                    runCatching { failedPlayer.release() }
                                    true
                                }
                                mediaPlayer = player
                                isPreparing = true
                                player.prepareAsync()
                            } else {
                                mediaPlayer?.start()
                                isPlaying = true
                            }
                        } catch (e: Exception) {
                            runCatching { mediaPlayer?.release() }
                            mediaPlayer = null
                            isPreparing = false
                            isPlaying = false
                        }
                    }
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        formatDuration(if (isPlaying) currentPositionMs else totalDurationMs),
                        color = Cream,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(formatDuration(totalDurationMs), color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
                val progress = if (totalDurationMs > 0) {
                    (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Coral,
                    trackColor = Panel,
                )
            }
            if (onDelete != null) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val player = mediaPlayer
                        mediaPlayer = null
                        isPreparing = false
                        isPlaying = false
                        runCatching { player?.stop() }
                        runCatching { player?.release() }
                        onDelete()
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete_voice_note),
                        tint = Coral,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
