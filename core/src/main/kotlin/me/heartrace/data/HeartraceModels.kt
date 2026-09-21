package me.heartrace.data

enum class AppTab { TODAY, INSIGHTS, YOU }

enum class Mood { DRAINED, QUIET, LOW, STEADY, BRIGHT, RESTLESS, ELECTRIC }

enum class AppLanguage(
    val tag: String,
) {
    SYSTEM("system"),
    ENGLISH("en"),
    CHINESE("zh"),
}

data class DailyNote(
    val id: Long,
    val date: String,
    val text: String,
    val mood: Mood,
    val createdAt: Long = 0L,
    val moodEntryId: Long? = null,
    val imageUris: List<String> = emptyList(),
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
)

data class MoodRecord(
    val id: Long,
    val mood: Mood,
    val moodValue: Int = 50,
    val energyLevel: Int = 2,
    val recordedAt: Long,
    val momentText: String? = null,
    val imageUris: List<String> = emptyList(),
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
)

data class PreferencesSnapshot(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val lightTheme: Boolean = false,
    val remindersEnabled: Boolean = false,
    val nickname: String = "",
    val avatarUri: String? = null,
)

data class TodayDataSnapshot(
    val mood: Mood = Mood.STEADY,
    val savedNote: String = "",
    val todayNotes: List<DailyNote> = emptyList(),
    val todayMoodRecords: List<MoodRecord> = emptyList(),
    val moodValue: Int = 50,
    val energyLevel: Int = 2,
)

data class InsightsDataSnapshot(
    val records: List<MoodRecord> = emptyList(),
    val notes: List<DailyNote> = emptyList(),
    val recentRecords: List<MoodRecord> = emptyList(),
)

fun Mood.energyPercent(): Int = when (this) {
    Mood.DRAINED -> 12
    Mood.QUIET -> 34
    Mood.LOW -> 22
    Mood.STEADY -> 62
    Mood.BRIGHT -> 78
    Mood.RESTLESS -> 70
    Mood.ELECTRIC -> 94
}
