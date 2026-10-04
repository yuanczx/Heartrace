package me.heartrace.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_notes",
    indices = [Index(value = ["createdAt"]), Index(value = ["moodEntryId"], unique = true)],
)
data class DailyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateLabel: String,
    val text: String,
    val mood: String,
    val createdAt: Long = System.currentTimeMillis(),
    val moodEntryId: Long? = null,
    val imageUris: String? = null,
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
)

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 1,
    val mood: String = "STEADY",
    val savedNote: String = "",
    val languageTag: String = "system",
    val lightTheme: Boolean = false,
    val remindersEnabled: Boolean = false,
    val nickname: String = "",
    val avatarUri: String? = null,
    val weeklyGoalDays: Int = 5,
)

@Entity(
    tableName = "mood_entries",
    indices = [Index(value = ["recordedAt"])],
)
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mood: String,
    val moodValue: Int = 50,
    val energyLevel: Int? = null,
    val recordedAt: Long = System.currentTimeMillis(),
)
