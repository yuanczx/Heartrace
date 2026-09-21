package me.heartrace.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyNoteEntity::class, AppStateEntity::class, MoodEntryEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class HeartraceDatabase : RoomDatabase() {
    abstract fun dailyNoteDao(): DailyNoteDao

    abstract fun appStateDao(): AppStateDao

    abstract fun moodEntryDao(): MoodEntryDao
}
