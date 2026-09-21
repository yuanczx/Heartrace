package me.heartrace.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.heartrace.uikit.R
import kotlin.time.Duration.Companion.seconds

@Composable
fun AttachmentSelectorBar(
    imageUris: List<String>,
    onAddImages: (List<String>) -> Unit,
    onRemoveImage: (String) -> Unit,
    audioUri: String?,
    audioDurationMs: Long,
    onAudioRecorded: (String, Long) -> Unit,
    onRemoveAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableLongStateOf(0L) }
    val recorderManager = remember { VoiceRecorderManager(context) }
    val currentOnAddImages by rememberUpdatedState(onAddImages)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        coroutineScope.launch(Dispatchers.IO) {
            val copiedPaths = uris.mapNotNull { copyImageUriToInternalStorage(context, it) }
            if (copiedPaths.isNotEmpty()) {
                if (isActive) {
                    withContext(Dispatchers.Main.immediate) { currentOnAddImages(copiedPaths) }
                } else {
                    copiedPaths.forEach { deleteInternalAttachment(context, it) }
                }
            }
        }
    }
    val audioPermissionDeniedText = stringResource(R.string.permission_audio_denied)
    val currentOnAudioRecorded by rememberUpdatedState(onAudioRecorded)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val file = recorderManager.startRecording()
            if (file != null) {
                isRecording = true
                recordingDurationSeconds = 0L
            }
        } else {
            Toast.makeText(context, audioPermissionDeniedText, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1.seconds)
            recordingDurationSeconds += 1
        }
    }
    DisposableEffect(recorderManager) {
        onDispose { recorderManager.cancelRecording() }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (imageUris.isNotEmpty()) {
            ImageAttachmentsView(imageUris = imageUris, onRemoveImage = onRemoveImage)
            Spacer(Modifier.height(8.dp))
        }
        if (!audioUri.isNullOrBlank()) {
            VoicePlayerView(audioUri = audioUri, durationMs = audioDurationMs, onDelete = onRemoveAudio)
            Spacer(Modifier.height(8.dp))
        }
        if (isRecording) {
            RecordingControls(
                durationSeconds = recordingDurationSeconds,
                onCancel = {
                    recorderManager.cancelRecording()
                    isRecording = false
                },
                onStop = {
                    isRecording = false
                    coroutineScope.launch {
                        val result = recorderManager.stopRecording()
                        if (result != null) {
                            if (isActive) {
                                currentOnAudioRecorded(result.first, result.second)
                            } else {
                                deleteInternalAttachment(context, result.first)
                            }
                        }
                    }
                },
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cream),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_image), fontSize = 12.sp)
                }
                if (audioUri.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                val file = recorderManager.startRecording()
                                if (file != null) {
                                    isRecording = true
                                    recordingDurationSeconds = 0L
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cream),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.record_voice), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
