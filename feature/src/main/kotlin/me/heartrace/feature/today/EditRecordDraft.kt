package me.heartrace.feature.today

import me.heartrace.data.MoodRecord
import me.heartrace.feature.attachments.DraftAttachmentStorage

data class EditRecordDraft(
    val record: MoodRecord,
    val moodValue: Int = record.moodValue,
    val energyLevel: Int? = record.energyLevel,
    val momentText: String = record.momentText.orEmpty(),
    val recordedAt: Long = record.recordedAt,
    val imageUris: List<String> = record.imageUris,
    val audioUri: String? = record.audioUri,
    val audioDurationMs: Long = record.audioDurationMs,
) {
    private val originalAttachments: Set<String>
        get() = (record.imageUris + listOfNotNull(record.audioUri)).toSet()

    val newAttachments: Set<String>
        get() = (imageUris + listOfNotNull(audioUri)).toSet() - originalAttachments

    fun discardAttachments(storage: DraftAttachmentStorage) {
        newAttachments.forEach(storage::delete)
    }

    fun discardRemovedAttachments(next: EditRecordDraft, storage: DraftAttachmentStorage) {
        (newAttachments - next.newAttachments).forEach(storage::delete)
    }
}
