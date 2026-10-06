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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    enabled: Boolean = true,
    onBusyChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var isStopping by remember { mutableStateOf(false) }
    var isAwaitingPermission by remember { mutableStateOf(false) }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnBusyChanged by rememberUpdatedState(onBusyChanged)
    var recordingDurationSeconds by remember { mutableLongStateOf(0L) }
    val recorderManager = remember { VoiceRecorderManager(context) }
    val currentOnAddImages by rememberUpdatedState(onAddImages)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        coroutineScope.launch {
            val copiedPaths = mutableListOf<String>()
            var delivered = false
            try {
                uris.forEach { uri -> copyImageToInternalStorage(context, uri)?.let(copiedPaths::add) }
                if (isActive && currentEnabled) {
                    if (copiedPaths.isNotEmpty()) currentOnAddImages(copiedPaths)
                    delivered = true
                }
            } finally {
                if (!delivered) copiedPaths.forEach { deleteInternalAttachment(context, it) }
                isImporting = false
            }
        }
    }
    val audioPermissionDeniedText = stringResource(R.string.permission_audio_denied)
    val currentOnAudioRecorded by rememberUpdatedState(onAudioRecorded)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        isAwaitingPermission = false
        if (isGranted && currentEnabled) {
            val file = recorderManager.startRecording()
            if (file != null) {
                isRecording = true
                currentOnBusyChanged(true)
                recordingDurationSeconds = 0L
            }
        } else if (!isGranted) {
            Toast.makeText(context, audioPermissionDeniedText, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1.seconds)
            recordingDurationSeconds += 1
        }
    }
    LaunchedEffect(isRecording, isImporting, isStopping, isAwaitingPermission) {
        currentOnBusyChanged(isRecording || isImporting || isStopping || isAwaitingPermission)
    }
    DisposableEffect(recorderManager) {
        onDispose {
            recorderManager.cancelRecording()
            currentOnBusyChanged(false)
        }
    }
    val controlsEnabled = enabled && !isImporting && !isStopping && !isAwaitingPermission
    Column(modifier = modifier.fillMaxWidth()) {
        if (imageUris.isNotEmpty()) {
            ImageAttachmentsView(imageUris = imageUris, onRemoveImage = if (controlsEnabled) onRemoveImage else null)
            Spacer(Modifier.height(8.dp))
        }
        if (!audioUri.isNullOrBlank()) {
            VoicePlayerView(audioUri = audioUri, durationMs = audioDurationMs, onDelete = if (controlsEnabled) onRemoveAudio else null)
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
                    isStopping = true
                    isRecording = false
                    coroutineScope.launch {
                        var recordedUri: String? = null
                        var delivered = false
                        try {
                            val result = recorderManager.stopRecording()
                            recordedUri = result?.first
                            if (result != null && isActive && currentEnabled) {
                                currentOnAudioRecorded(result.first, result.second)
                                delivered = true
                            }
                        } finally {
                            if (!delivered) deleteInternalAttachment(context, recordedUri)
                            isStopping = false
                        }
                    }
                },
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        isImporting = true
                        currentOnBusyChanged(true)
                        imagePickerLauncher.launch("image/*")
                    },
                    enabled = controlsEnabled,
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
                        enabled = controlsEnabled,
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                val file = recorderManager.startRecording()
                                if (file != null) {
                                    isRecording = true
                                    currentOnBusyChanged(true)
                                    recordingDurationSeconds = 0L
                                }
                            } else {
                                isAwaitingPermission = true
                                currentOnBusyChanged(true)
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
