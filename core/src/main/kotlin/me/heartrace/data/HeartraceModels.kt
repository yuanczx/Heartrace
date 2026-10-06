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

enum class ThemeMode(
    val tag: String,
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
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
    val energyLevel: Int? = null,
    val recordedAt: Long,
    val momentText: String? = null,
    val imageUris: List<String> = emptyList(),
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
)

data class PreferencesSnapshot(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val remindersEnabled: Boolean = false,
    val nickname: String = "",
    val avatarUri: String? = null,
    val weeklyGoalDays: Int = 5,
)

data class TodayDataSnapshot(
    val mood: Mood = Mood.STEADY,
    val todayNotes: List<DailyNote> = emptyList(),
    val todayMoodRecords: List<MoodRecord> = emptyList(),
    val moodValue: Int = 50,
    val energyLevel: Int? = null,
)

data class InsightsDataSnapshot(
    val records: List<MoodRecord> = emptyList(),
    val notes: List<DailyNote> = emptyList(),
    val recentRecords: List<MoodRecord> = emptyList(),
    val weeklyGoalDays: Int = 5,
)

fun Mood.toMoodValue(): Int = when (this) {
    Mood.DRAINED -> 10
    Mood.LOW -> 25
    Mood.QUIET -> 40
    Mood.STEADY -> 55
    Mood.BRIGHT -> 72
    Mood.RESTLESS -> 84
    Mood.ELECTRIC -> 90
}

fun Int.toMood(): Mood = when {
    this < 18 -> Mood.DRAINED
    this < 33 -> Mood.LOW
    this < 48 -> Mood.QUIET
    this < 64 -> Mood.STEADY
    this < 80 -> Mood.BRIGHT
    this < 88 -> Mood.RESTLESS
    else -> Mood.ELECTRIC
}
