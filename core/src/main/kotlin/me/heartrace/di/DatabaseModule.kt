package me.heartrace.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.heartrace.data.db.AppStateDao
import me.heartrace.data.db.DailyNoteDao
import me.heartrace.data.db.HeartraceDatabase
import me.heartrace.data.db.MoodEntryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_notes_createdAt ON daily_notes(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_notes_moodEntryId ON daily_notes(moodEntryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mood_entries_recordedAt ON mood_entries(recordedAt)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_state ADD COLUMN nickname TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE app_state ADD COLUMN avatarUri TEXT")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Keep the newest row when an older database contains duplicate moments.
            db.execSQL(
                "DELETE FROM daily_notes " +
                    "WHERE moodEntryId IS NOT NULL AND id NOT IN (" +
                    "SELECT MAX(id) FROM daily_notes " +
                    "WHERE moodEntryId IS NOT NULL GROUP BY moodEntryId)",
            )
            db.execSQL("DROP INDEX IF EXISTS index_daily_notes_moodEntryId")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_daily_notes_moodEntryId " +
                    "ON daily_notes(moodEntryId)",
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): HeartraceDatabase = Room
        .databaseBuilder(context, HeartraceDatabase::class.java, "heartrace.db")
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .build()

    @Provides fun provideNoteDao(database: HeartraceDatabase): DailyNoteDao = database.dailyNoteDao()

    @Provides fun provideAppStateDao(database: HeartraceDatabase): AppStateDao = database.appStateDao()

    @Provides fun provideMoodEntryDao(database: HeartraceDatabase): MoodEntryDao = database.moodEntryDao()
}
