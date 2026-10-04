package me.heartrace.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.heartrace.data.db.DailyNoteEntity
import me.heartrace.data.db.HeartraceDatabase
import me.heartrace.data.db.MoodEntryEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class HeartraceRepositoryTest {
    private lateinit var database: HeartraceDatabase
    private lateinit var repository: HeartraceRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, HeartraceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HeartraceRepository(
            noteDao = database.dailyNoteDao(),
            stateDao = database.appStateDao(),
            moodEntryDao = database.moodEntryDao(),
            database = database,
            context = context,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveCheckIn_bounds_values_and_syncs_current_mood() = runBlocking {
        val id = repository.saveCheckIn(moodValue = 150, energyLevel = -1, recordedAt = System.currentTimeMillis())

        val entry = database.moodEntryDao().findById(id)
        assertNotNull(entry)
        assertEquals(100, entry?.moodValue)
        assertEquals(1, entry?.energyLevel)
        assertEquals("ELECTRIC", entry?.mood)
        assertEquals("ELECTRIC", database.appStateDao().get()?.mood)
    }

    @Test
    fun saveNote_creates_joined_mood_record_and_preserves_attachments() = runBlocking {
        repository.saveNote(
            text = "  clear head  ",
            mood = Mood.BRIGHT,
            imageUris = listOf("file://one", "file://two"),
            audioUri = "file://voice",
            audioDurationMs = 2_500L,
        )

        val snapshot = repository.loadTodaySnapshot()
        val record = snapshot.todayMoodRecords.single()
        val note = snapshot.todayNotes.single()
        assertEquals("clear head", note.text)
        assertEquals(record.id, note.moodEntryId)
        assertEquals(listOf("file://one", "file://two"), note.imageUris)
        assertEquals("file://voice", note.audioUri)
        assertEquals(Mood.BRIGHT, record.mood)
        assertEquals("clear head", record.momentText)
        assertEquals(2_500L, record.audioDurationMs)
    }

    @Test
    fun updateCheckIn_removes_empty_note_and_updates_entry() = runBlocking {
        repository.saveNote("old", Mood.LOW, System.currentTimeMillis(), emptyList(), null, 0L)
        val id = repository.loadTodaySnapshot().todayMoodRecords.single().id

        repository.updateCheckIn(
            id = id,
            moodValue = 75,
            energyLevel = 3,
            momentText = "   ",
            recordedAt = System.currentTimeMillis(),
        )

        val snapshot = repository.loadTodaySnapshot()
        assertEquals(75, snapshot.todayMoodRecords.single().moodValue)
        assertEquals(3, snapshot.todayMoodRecords.single().energyLevel)
        assertTrue(snapshot.todayNotes.isEmpty())
        assertNull(database.dailyNoteDao().findByMoodEntryId(id))
    }

    @Test
    fun insights_join_notes_and_reset_only_today_records() = runBlocking {
        val now = System.currentTimeMillis()
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val oldId = database.moodEntryDao().insert(MoodEntryEntity(mood = "QUIET", moodValue = 40, energyLevel = 2, recordedAt = yesterday))
        database.dailyNoteDao().insert(
            DailyNoteEntity(
                dateLabel = "",
                text = "old note",
                mood = "QUIET",
                createdAt = yesterday,
                moodEntryId = oldId,
            ),
        )
        repository.saveNote("today note", Mood.STEADY, System.currentTimeMillis(), emptyList(), null, 0L)

        val insights = repository.observeInsights().first()
        assertEquals(2, insights.records.size)
        assertEquals("today note", insights.records.first { it.momentText == "today note" }.momentText)

        repository.resetToday()
        val afterReset = repository.observeInsights().first()
        assertEquals(1, afterReset.records.size)
        assertEquals("old note", afterReset.notes.single().text)
        assertFalse(afterReset.records.any { it.momentText == "today note" })
    }

    @Test
    fun preferences_are_persisted_with_safe_profile_name() = runBlocking {
        repository.setLanguage(AppLanguage.CHINESE)
        repository.setLightTheme(true)
        repository.setRemindersEnabled(true)
        repository.setProfile("   ", "file://avatar")

        val preferences = repository.observePreferences().first()
        assertEquals(AppLanguage.CHINESE, preferences.language)
        assertTrue(preferences.lightTheme)
        assertTrue(preferences.remindersEnabled)
        assertEquals("", preferences.nickname)
        assertEquals("file://avatar", preferences.avatarUri)
    }

    @Test
    fun saveCheckIn_persists_optional_moment_and_weekly_goal() = runBlocking {
        repository.setWeeklyGoalDays(7)
        repository.saveCheckIn(
            moodValue = 72,
            energyLevel = null,
            momentText = "  optional note  ",
            imageUris = listOf("file://image"),
        )

        val preferences = repository.observePreferences().first()
        val record = repository.loadTodaySnapshot().todayMoodRecords.single()
        assertEquals(7, preferences.weeklyGoalDays)
        assertEquals("optional note", record.momentText)
        assertEquals(listOf("file://image"), record.imageUris)
        assertNull(record.energyLevel)
    }
}
