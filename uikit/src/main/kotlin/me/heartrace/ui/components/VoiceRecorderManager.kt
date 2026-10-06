package me.heartrace.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VoiceRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    var currentOutputFile: File? = null
        private set

    fun startRecording(): File? = try {
        val dir = File(context.filesDir, "attachments/audio").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder = recorder
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioEncodingBitRate(128000)
        recorder.setAudioSamplingRate(44100)
        recorder.setOutputFile(file.absolutePath)
        recorder.prepare()
        recorder.start()
        file
    } catch (e: Exception) {
        cancelRecording()
        null
    }

    suspend fun stopRecording(): Pair<String, Long>? {
        val recorder = mediaRecorder ?: return null
        val file = currentOutputFile
        return try {
            recorder.stop()
            recorder.release()
            mediaRecorder = null
            currentOutputFile = null
            if (file != null && file.exists() && file.length() > 0) {
                val duration = withContext(Dispatchers.IO) {
                    val mp = MediaPlayer.create(context, Uri.fromFile(file))
                    try {
                        mp?.duration?.toLong() ?: 0L
                    } finally {
                        mp?.release()
                    }
                }
                Pair(Uri.fromFile(file).toString(), duration)
            } else {
                file?.delete()
                null
            }
        } catch (e: Exception) {
            runCatching { recorder.release() }
            mediaRecorder = null
            file?.delete()
            currentOutputFile = null
            if (e is CancellationException) throw e
            null
        }
    }

    fun cancelRecording() {
        runCatching { mediaRecorder?.stop() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        currentOutputFile?.delete()
        currentOutputFile = null
    }
}
