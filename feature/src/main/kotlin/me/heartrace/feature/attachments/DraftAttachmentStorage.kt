package me.heartrace.feature.attachments

import android.content.Context
import androidx.core.net.toUri
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.heartrace.ui.components.copyImageToInternalStorage
import me.heartrace.ui.components.deleteInternalAttachment
import javax.inject.Inject

interface DraftAttachmentStorage {
    suspend fun copyImage(uri: String): String?

    fun delete(uri: String?)
}

class InternalDraftAttachmentStorage
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : DraftAttachmentStorage {
    override suspend fun copyImage(uri: String): String? = copyImageToInternalStorage(context, uri.toUri())

    override fun delete(uri: String?) = deleteInternalAttachment(context, uri)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DraftAttachmentModule {
    @Binds
    abstract fun bindDraftAttachmentStorage(storage: InternalDraftAttachmentStorage): DraftAttachmentStorage
}
