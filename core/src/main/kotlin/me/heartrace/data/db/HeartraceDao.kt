package me.heartrace.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNoteDao {
    @Query("SELECT * FROM daily_notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DailyNoteEntity>>

    @Query("SELECT * FROM daily_notes WHERE createdAt >= :start AND createdAt <= :end ORDER BY createdAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<DailyNoteEntity>>

    @Query("SELECT * FROM daily_notes WHERE createdAt >= :start AND createdAt < :end")
    suspend fun findBetween(start: Long, end: Long): List<DailyNoteEntity>

    @Insert
    suspend fun insert(note: DailyNoteEntity): Long

    @Query("SELECT * FROM daily_notes WHERE moodEntryId = :moodEntryId LIMIT 1")
    suspend fun findByMoodEntryId(moodEntryId: Long): DailyNoteEntity?

    @Update
    suspend fun update(note: DailyNoteEntity)

    @Query("DELETE FROM daily_notes WHERE moodEntryId = :moodEntryId")
    suspend fun deleteByMoodEntryId(moodEntryId: Long)

    @Query("DELETE FROM daily_notes WHERE createdAt >= :start AND createdAt < :end")
    suspend fun deleteBetween(start: Long, end: Long)
}

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 1 LIMIT 1")
    fun observe(): Flow<AppStateEntity?>

    @Query("SELECT * FROM app_state WHERE id = 1 LIMIT 1")
    suspend fun get(): AppStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: AppStateEntity)

    @Query("UPDATE app_state SET mood = :mood WHERE id = 1")
    suspend fun updateMood(mood: String)

    @Query("UPDATE app_state SET mood = 'STEADY' WHERE id = 1")
    suspend fun resetToday()
}

@Dao
interface MoodEntryDao {
    @Query("SELECT * FROM mood_entries ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE recordedAt >= :start AND recordedAt <= :end ORDER BY recordedAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MoodEntryEntity?

    @Insert
    suspend fun insert(entry: MoodEntryEntity): Long

    @Update
    suspend fun update(entry: MoodEntryEntity)

    @Query("DELETE FROM mood_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM mood_entries WHERE recordedAt >= :start AND recordedAt < :end")
    suspend fun deleteBetween(start: Long, end: Long)

    @Query("SELECT * FROM mood_entries WHERE recordedAt >= :start AND recordedAt <= :end ORDER BY recordedAt DESC LIMIT 1")
    suspend fun latestBetween(start: Long, end: Long): MoodEntryEntity?
}
