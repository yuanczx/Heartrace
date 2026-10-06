package me.heartrace.ui.components

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val MAX_IMAGE_BYTES = 20L * 1024L * 1024L

/** Deletes the imported file if cancellation prevents its delivery to a draft. */
suspend fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
    var copiedUri: String? = null
    return try {
        withContext(Dispatchers.IO) {
            copyImageUriToInternalStorage(context, uri).also { copiedUri = it }
        }
    } catch (e: CancellationException) {
        deleteInternalAttachment(context, copiedUri)
        throw e
    }
}

fun copyImageUriToInternalStorage(context: Context, uri: Uri): String? {
    var file: File? = null
    return try {
        val declaredLength = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (declaredLength > MAX_IMAGE_BYTES) return null
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val dir = File(context.filesDir, "attachments/images").apply { mkdirs() }
        file = File(dir, "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
        input.use {
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_IMAGE_BYTES) throw IOException("Image exceeds the size limit")
                    output.write(buffer, 0, count)
                }
            }
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        file?.delete()
        null
    }
}

fun deleteInternalAttachment(context: Context, uriString: String?) {
    val uri = uriString?.let(Uri::parse) ?: return
    if (uri.scheme != "file") return
    val root = File(context.filesDir, "attachments").canonicalFile
    val target = runCatching { uri.path?.let(::File)?.canonicalFile }.getOrNull() ?: return
    if (target.path.startsWith(root.path + File.separator)) target.delete()
}
