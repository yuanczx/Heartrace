package me.heartrace.feature.patterns

import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AwarenessMetricsTest {
    @Test
    fun weekly_goal_counts_each_calendar_day_once() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 4, 12, 0, 0) }.timeInMillis
        val today = MoodRecord(1, Mood.STEADY, 55, 2, now)
        val secondToday = MoodRecord(2, Mood.BRIGHT, 75, 3, now - 10_000)
        val yesterday = MoodRecord(3, Mood.QUIET, 40, 2, dayOffset(now, -1))

        val progress = calculateWeeklyGoalProgress(
            records = listOf(today, secondToday, yesterday),
            now = now,
            goalDays = 3,
        )

        assertEquals(2, progress.completedDays)
        assertEquals(1, progress.remainingDays)
        assertTrue(!progress.isComplete)
    }

    @Test
    fun current_streak_ends_at_the_latest_recorded_day() {
        val now = System.currentTimeMillis()
        val records = (-2..0).mapIndexed { index, offset ->
            MoodRecord(index.toLong(), Mood.STEADY, 55, 2, dayOffset(now, offset))
        }

        assertEquals(3, calculateCurrentStreak(records, now))
    }

    @Test
    fun badges_use_record_count_and_streak_thresholds() {
        val now = System.currentTimeMillis()
        val records = (0..9).map { index ->
            MoodRecord(index.toLong(), Mood.STEADY, 55, 2, dayOffset(now, -index))
        }

        val badges = calculateBadges(records, now, goalDays = 5)

        assertTrue(badges.first { it.kind == BadgeKind.FIRST_RECORD }.earned)
        assertTrue(badges.first { it.kind == BadgeKind.TEN_RECORDS }.earned)
        assertTrue(badges.first { it.kind == BadgeKind.THREE_DAY_STREAK }.earned)
    }

    private fun dayOffset(timestamp: Long, amount: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        add(Calendar.DAY_OF_YEAR, amount)
    }.timeInMillis

    @Test
    fun interrupted_streak_keeps_historic_badge_and_deletion_recalculates_it() {
        val now = System.currentTimeMillis()
        val records = (4..6).map { MoodRecord(it.toLong(), Mood.STEADY, 55, 2, dayOffset(now, -it)) }
        assertEquals(0, calculateCurrentStreak(records, now))
        assertTrue(calculateBadges(records, now, 5).first { it.kind == BadgeKind.THREE_DAY_STREAK }.earned)
        assertTrue(!calculateBadges(records.drop(1), now, 5).first { it.kind == BadgeKind.THREE_DAY_STREAK }.earned)
    }

    @Test
    fun four_week_badge_requires_consecutive_weeks() {
        val now = System.currentTimeMillis()
        val monday = weekStart(now)
        val records = listOf(-1, -3, -5, -7).flatMap { week ->
            (0..4).map { day -> MoodRecord((week * 7 + day).toLong(), Mood.STEADY, 55, 2, shiftDays(monday, week * 7 + day)) }
        }
        assertTrue(!calculateBadges(records, now, 5).first { it.kind == BadgeKind.FOUR_WEEKLY_GOALS }.earned)
    }
}
