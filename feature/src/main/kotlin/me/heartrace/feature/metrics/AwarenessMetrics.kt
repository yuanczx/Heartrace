package me.heartrace.feature.metrics

import me.heartrace.data.MoodRecord
import java.util.Calendar

data class WeeklyGoalProgress(
    val goalDays: Int,
    val completedDays: Int,
    val remainingDays: Int,
    val isComplete: Boolean,
)

enum class BadgeKind {
    FIRST_RECORD,
    THREE_DAY_STREAK,
    SEVEN_DAY_STREAK,
    FOURTEEN_DAY_STREAK,
    TEN_RECORDS,
    FIRST_WEEKLY_GOAL,
    FOUR_WEEKLY_GOALS,
    MOOD_EXPLORER,
    MOMENT_KEEPER,
    THIRTY_RECORDS,
}

data class BadgeStatus(
    val kind: BadgeKind,
    val earned: Boolean,
)

fun calculateWeeklyGoalProgress(
    records: List<MoodRecord>,
    now: Long,
    goalDays: Int,
): WeeklyGoalProgress {
    val start = weekStart(now)
    val end = shiftDays(start, 7)
    val completed = records.asSequence()
        .filter { it.recordedAt in start until end }
        .map { dayStart(it.recordedAt) }
        .toSet()
        .size
    val safeGoal = goalDays.coerceIn(3, 7)
    return WeeklyGoalProgress(
        goalDays = safeGoal,
        completedDays = completed,
        remainingDays = (safeGoal - completed).coerceAtLeast(0),
        isComplete = completed >= safeGoal,
    )
}

fun calculateCurrentStreak(records: List<MoodRecord>, now: Long): Int {
    val days = records.asSequence().map { dayStart(it.recordedAt) }.toSet()
    if (days.isEmpty()) return 0
    var cursor = dayStart(now)
    if (cursor !in days) cursor = shiftDays(cursor, -1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = shiftDays(cursor, -1)
    }
    return streak
}

fun calculateBadges(records: List<MoodRecord>, now: Long, goalDays: Int): List<BadgeStatus> {
    val streak = calculateLongestStreak(records)
    val weeklyGoals = completedWeeklyGoals(records, now, goalDays)
    val moodVariety = records.asSequence().map { it.mood }.toSet().size
    val moments = records.count {
        !it.momentText.isNullOrBlank() || it.imageUris.isNotEmpty() || !it.audioUri.isNullOrBlank()
    }
    return listOf(
        BadgeStatus(BadgeKind.FIRST_RECORD, records.isNotEmpty()),
        BadgeStatus(BadgeKind.THREE_DAY_STREAK, streak >= 3),
        BadgeStatus(BadgeKind.SEVEN_DAY_STREAK, streak >= 7),
        BadgeStatus(BadgeKind.FOURTEEN_DAY_STREAK, streak >= 14),
        BadgeStatus(BadgeKind.TEN_RECORDS, records.size >= 10),
        BadgeStatus(BadgeKind.FIRST_WEEKLY_GOAL, weeklyGoals >= 1),
        BadgeStatus(BadgeKind.FOUR_WEEKLY_GOALS, weeklyGoals >= 4),
        BadgeStatus(BadgeKind.MOOD_EXPLORER, moodVariety >= 5),
        BadgeStatus(BadgeKind.MOMENT_KEEPER, moments >= 3),
        BadgeStatus(BadgeKind.THIRTY_RECORDS, records.size >= 30),
    )
}

fun calculateLongestStreak(records: List<MoodRecord>): Int {
    val days = records.asSequence().map { dayStart(it.recordedAt) }.toSet().sorted()
    if (days.isEmpty()) return 0
    var longest = 1
    var current = 1
    for (index in 1 until days.size) {
        if (days[index] == shiftDays(days[index - 1], 1)) {
            current++
            longest = maxOf(longest, current)
        } else {
            current = 1
        }
    }
    return longest
}

fun dayStart(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun weekStart(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = dayStart(timestamp)
    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
    val daysFromMonday = (dayOfWeek + 5) % 7
    add(Calendar.DAY_OF_YEAR, -daysFromMonday)
}.timeInMillis

fun shiftDays(timestamp: Long, amount: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    add(Calendar.DAY_OF_YEAR, amount)
}.timeInMillis

private fun completedWeeklyGoals(records: List<MoodRecord>, now: Long, goalDays: Int): Int {
    if (records.isEmpty()) return 0
    val currentWeek = weekStart(now)
    val safeGoal = goalDays.coerceIn(3, 7)
    // A day can contain several check-ins. Group distinct days once instead
    // of scanning the entire history again for each recorded week.
    val daysByWeek = records.asSequence()
        .map { dayStart(it.recordedAt) }
        .toSet()
        .groupBy { weekStart(it) }
    val completed = daysByWeek.entries.asSequence()
        .filter { (start, days) -> start <= currentWeek && days.size >= safeGoal }
        .map { it.key }
        .sorted()
        .toList()
    var longest = 0
    var current = 0
    completed.forEachIndexed { index, start ->
        current = if (index > 0 && start == shiftDays(completed[index - 1], 7)) current + 1 else 1
        longest = maxOf(longest, current)
    }
    return longest
}
