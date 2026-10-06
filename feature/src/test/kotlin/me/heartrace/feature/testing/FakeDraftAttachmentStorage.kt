package me.heartrace.feature.testing

import kotlinx.coroutines.CompletableDeferred
import me.heartrace.feature.attachments.DraftAttachmentStorage

class FakeDraftAttachmentStorage : DraftAttachmentStorage {
    val deletedUris = mutableListOf<String>()
    var copiedUri: String? = "file://new-avatar"
    var copyGate: CompletableDeferred<Unit>? = null
    var copyError: Throwable? = null

    override suspend fun copyImage(uri: String): String? {
        val result = copiedUri
        copyGate?.await()
        copyError?.let { throw it }
        return result
    }

    override fun delete(uri: String?) {
        uri?.let(deletedUris::add)
    }
}
