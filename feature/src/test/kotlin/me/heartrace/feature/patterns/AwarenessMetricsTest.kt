package me.heartrace.feature.patterns

import me.heartrace.data.Mood
import me.heartrace.data.MoodRecord
import me.heartrace.data.toMoodValue
import me.heartrace.feature.metrics.BadgeKind
import me.heartrace.feature.metrics.calculateBadges
import me.heartrace.feature.metrics.calculateCurrentStreak
import me.heartrace.feature.metrics.calculateWeeklyGoalProgress
import me.heartrace.feature.metrics.shiftDays
import me.heartrace.feature.metrics.weekStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

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

    @Test
    fun weekly_badges_count_distinct_days_in_unsorted_history() {
        val now = System.currentTimeMillis()
        val monday = weekStart(now)
        val duplicateDays = (-4..-1).flatMap { week ->
            (0..4).map { checkIn ->
                MoodRecord((week * 10 + checkIn).toLong(), Mood.STEADY, 55, 2, shiftDays(monday, week * 7))
            }
        }
        assertTrue(!calculateBadges(duplicateDays.reversed(), now, 5).first { it.kind == BadgeKind.FIRST_WEEKLY_GOAL }.earned)

        val completedWeeks = (-4..-1).flatMap { week ->
            (0..4).map { day ->
                MoodRecord((week * 10 + day).toLong(), Mood.STEADY, 55, 2, shiftDays(monday, week * 7 + day))
            }
        }
        assertTrue(calculateBadges(completedWeeks.reversed(), now, 5).first { it.kind == BadgeKind.FOUR_WEEKLY_GOALS }.earned)
    }

    @Test
    fun future_weeks_do_not_earn_weekly_badges() {
        val now = System.currentTimeMillis()
        val nextMonday = shiftDays(weekStart(now), 7)
        val records = (0..4).map { day ->
            MoodRecord(day.toLong(), Mood.STEADY, 55, 2, shiftDays(nextMonday, day))
        }

        assertTrue(!calculateBadges(records, now, 5).first { it.kind == BadgeKind.FIRST_WEEKLY_GOAL }.earned)
    }

    @Test
    fun weekly_badges_preserve_calendar_days_across_daylight_saving() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val monday = Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.MARCH, 2, 0, 0, 0)
            }.timeInMillis
            val records = (0..27).map { day ->
                MoodRecord(day.toLong(), Mood.STEADY, 55, 2, shiftDays(monday, day))
            }

            val badges = calculateBadges(records, shiftDays(monday, 27), 7)
            assertTrue(badges.first { it.kind == BadgeKind.FOUR_WEEKLY_GOALS }.earned)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun badges_reward_mood_variety_and_saved_moments() {
        val now = System.currentTimeMillis()
        val moods = listOf(Mood.DRAINED, Mood.QUIET, Mood.STEADY, Mood.BRIGHT, Mood.ELECTRIC)
        val records = moods.mapIndexed { index, mood ->
            MoodRecord(index.toLong(), mood, mood.toMoodValue(), 2, dayOffset(now, -index), momentText = if (index < 3) "A small note" else null)
        }

        val badges = calculateBadges(records, now, goalDays = 5)

        assertTrue(badges.first { it.kind == BadgeKind.MOOD_EXPLORER }.earned)
        assertTrue(badges.first { it.kind == BadgeKind.MOMENT_KEEPER }.earned)
        assertTrue(!badges.first { it.kind == BadgeKind.THIRTY_RECORDS }.earned)
    }

    @Test
    fun moment_keeper_counts_attachment_only_moments() {
        val now = System.currentTimeMillis()
        val records = (0..2).map { index ->
            MoodRecord(
                id = index.toLong(),
                mood = Mood.STEADY,
                moodValue = Mood.STEADY.toMoodValue(),
                energyLevel = 2,
                recordedAt = dayOffset(now, -index),
                imageUris = if (index == 0) listOf("file://image") else emptyList(),
                audioUri = if (index == 1) "file://audio" else null,
                momentText = if (index == 2) "Saved moment" else null,
            )
        }

        val badges = calculateBadges(records, now, goalDays = 5)

        assertTrue(badges.first { it.kind == BadgeKind.MOMENT_KEEPER }.earned)
    }
}
