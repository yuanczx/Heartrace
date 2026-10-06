package me.heartrace.data

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.heartrace.data.db.AppStateDao
import me.heartrace.data.db.AppStateEntity
import me.heartrace.data.db.DailyNoteDao
import me.heartrace.data.db.DailyNoteEntity
import me.heartrace.data.db.MoodEntryDao
import me.heartrace.data.db.MoodEntryEntity
import java.io.File
import java.util.Calendar
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class HeartraceRepository
@Inject
constructor(
    private val noteDao: DailyNoteDao,
    private val stateDao: AppStateDao,
    private val moodEntryDao: MoodEntryDao,
    private val database: me.heartrace.data.db.HeartraceDatabase,
    @ApplicationContext private val context: Context,
) : HeartraceDataSource {
    private val stateMutex = Mutex()

    private companion object {
        const val RECENT_DAYS = 7
    }

    override fun observePreferences(): Flow<PreferencesSnapshot> = stateDao.observe().map { state ->
        val saved = state ?: AppStateEntity()
        PreferencesSnapshot(
            language =
            AppLanguage.entries.firstOrNull { it.tag == saved.languageTag }
                ?: AppLanguage.SYSTEM,
            themeMode = ThemeMode.entries.firstOrNull { it.tag == saved.themeMode }
                ?: ThemeMode.SYSTEM,
            remindersEnabled = saved.remindersEnabled,
            nickname = saved.nickname,
            avatarUri = saved.avatarUri,
            weeklyGoalDays = saved.weeklyGoalDays.coerceIn(3, 7),
        )
    }.distinctUntilChanged()

    override fun observeToday(): Flow<TodayDataSnapshot> = dayTicker().flatMapLatest { todayStart ->
        combine(
            noteDao.observeBetween(todayStart, Long.MAX_VALUE).distinctUntilChanged(),
            stateDao.observe(),
            moodEntryDao.observeBetween(todayStart, Long.MAX_VALUE).distinctUntilChanged(),
            minuteTicker(),
        ) { notes, state, moodEntries, _ ->
            // The ticker only triggers a refresh. Its emitted timestamp may be
            // older than a record written since the last tick, which would
            // make buildTodaySnapshot filter that record (and any other recent
            // records) out after an update. Read the clock at emission time so
            // database invalidations always build a current snapshot.
            buildTodaySnapshot(notes, state, moodEntries, System.currentTimeMillis())
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    override suspend fun loadTodaySnapshot(): TodayDataSnapshot = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)
        buildTodaySnapshot(
            notes = noteDao.observeBetween(todayStart, Long.MAX_VALUE).first(),
            state = stateDao.observe().first(),
            moodEntries = moodEntryDao.observeBetween(todayStart, Long.MAX_VALUE).first(),
            now = now,
        )
    }

    override fun observeInsights(): Flow<InsightsDataSnapshot> {
        val historyFlow = combine(
            noteDao.observeAll().distinctUntilChanged(),
            moodEntryDao.observeAll().distinctUntilChanged(),
        ) { notes, moodEntries ->
            val noteModels = notes.map { it.toModel() }
            val notesByEntryId = noteModels.mapNotNull { note ->
                note.moodEntryId?.let { it to note }
            }.toMap()
            val records = moodEntries.map { entry ->
                val record = entry.toModel()
                val note = notesByEntryId[record.id]
                record.copy(
                    momentText = note?.text,
                    imageUris = note?.imageUris ?: emptyList(),
                    audioUri = note?.audioUri,
                    audioDurationMs = note?.audioDurationMs ?: 0L,
                )
            }
            InsightsDataSnapshot(
                records = records,
                notes = noteModels,
            )
        }
        val weeklyGoalFlow = stateDao.observe()
            .map { it?.weeklyGoalDays?.coerceIn(3, 7) ?: 5 }
            .distinctUntilChanged()
        // A new day must reach collectors even when the stored history is
        // unchanged: current streaks and weekly progress depend on the clock.
        return combine(historyFlow, weeklyGoalFlow, dayTicker()) { history, weeklyGoalDays, _ ->
            val recentWindowStart = startOfRecentWindow()
            history.copy(
                recentRecords = history.records.filter { it.recordedAt >= recentWindowStart },
                weeklyGoalDays = weeklyGoalDays,
            )
        }.flowOn(Dispatchers.Default)
    }

    private suspend fun ensureState() {
        stateMutex.withLock {
            if (stateDao.get() == null) stateDao.upsert(AppStateEntity())
        }
    }

    override suspend fun saveCheckIn(
        moodValue: Int,
        energyLevel: Int?,
        recordedAt: Long,
        momentText: String?,
        imageUris: List<String>,
        audioUri: String?,
        audioDurationMs: Long,
    ): Long = run {
        ensureState()
        database.withTransaction {
            saveCheckInInternal(moodValue, energyLevel, recordedAt, momentText, imageUris, audioUri, audioDurationMs)
        }
    }

    private suspend fun saveCheckInInternal(
        moodValue: Int,
        energyLevel: Int?,
        recordedAt: Long,
        momentText: String? = null,
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    ): Long {
        val boundedMood = moodValue.coerceIn(0, 100)
        val boundedEnergy = energyLevel?.coerceIn(1, 3)
        val mood = boundedMood.toMood().name
        val safeRecordedAt = recordedAt.coerceAtMost(System.currentTimeMillis())
        val id = moodEntryDao.insert(
            MoodEntryEntity(
                mood = mood,
                moodValue = boundedMood,
                energyLevel = boundedEnergy,
                recordedAt = safeRecordedAt,
            ),
        )
        val trimmedText = momentText?.trim().orEmpty()
        if (trimmedText.isNotBlank() || imageUris.isNotEmpty() || !audioUri.isNullOrBlank()) {
            noteDao.insert(
                DailyNoteEntity(
                    dateLabel = "",
                    text = trimmedText,
                    mood = mood,
                    createdAt = safeRecordedAt,
                    moodEntryId = id,
                    imageUris = imageUris.toDbString(),
                    audioUri = audioUri,
                    audioDurationMs = audioDurationMs,
                ),
            )
        }
        syncCurrentMood()
        return id
    }

    override suspend fun saveMoment(
        checkInId: Long,
        text: String,
        recordedAt: Long,
        imageUris: List<String>,
        audioUri: String?,
        audioDurationMs: Long,
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank() && imageUris.isEmpty() && audioUri.isNullOrBlank()) return
        var obsoleteAttachments = emptyList<String>()
        database.withTransaction {
            val mood = moodEntryDao.findById(checkInId)?.mood ?: return@withTransaction
            val safeRecordedAt = recordedAt.coerceAtMost(System.currentTimeMillis())
            val existingNote = noteDao.findByMoodEntryId(checkInId)
            if (existingNote == null) {
                noteDao.insert(
                    DailyNoteEntity(
                        dateLabel = "",
                        text = trimmed,
                        mood = mood,
                        createdAt = safeRecordedAt,
                        moodEntryId = checkInId,
                        imageUris = imageUris.toDbString(),
                        audioUri = audioUri,
                        audioDurationMs = audioDurationMs,
                    ),
                )
            } else {
                obsoleteAttachments = existingNote.attachmentUris() - (imageUris + listOfNotNull(audioUri)).toSet()
                noteDao.update(
                    existingNote.copy(
                        text = trimmed,
                        mood = mood,
                        createdAt = safeRecordedAt,
                        imageUris = imageUris.toDbString(),
                        audioUri = audioUri,
                        audioDurationMs = audioDurationMs,
                    ),
                )
            }
        }
        obsoleteAttachments.forEach(::deleteAttachment)
    }

    override suspend fun updateCheckIn(
        id: Long,
        moodValue: Int,
        energyLevel: Int?,
        momentText: String?,
        recordedAt: Long,
        imageUris: List<String>,
        audioUri: String?,
        audioDurationMs: Long,
    ) {
        ensureState()
        var obsoleteAttachments = emptyList<String>()
        database.withTransaction {
            val boundedMood = moodValue.coerceIn(0, 100)
            val boundedEnergy = energyLevel?.coerceIn(1, 3)
            val moodName = boundedMood.toMood().name
            val existingEntry = moodEntryDao.findById(id) ?: return@withTransaction
            val safeRecordedAt = recordedAt.coerceAtMost(System.currentTimeMillis())
            moodEntryDao.update(
                existingEntry.copy(
                    mood = moodName,
                    moodValue = boundedMood,
                    energyLevel = boundedEnergy,
                    recordedAt = safeRecordedAt,
                ),
            )
            val trimmedText = momentText?.trim().orEmpty()
            val hasContent = trimmedText.isNotBlank() || imageUris.isNotEmpty() || !audioUri.isNullOrBlank()
            val existingNote = noteDao.findByMoodEntryId(id)
            if (existingNote != null) {
                if (!hasContent) {
                    obsoleteAttachments = existingNote.attachmentUris()
                    noteDao.deleteByMoodEntryId(id)
                } else {
                    obsoleteAttachments = existingNote.attachmentUris() - (imageUris + listOfNotNull(audioUri)).toSet()
                    noteDao.update(
                        existingNote.copy(
                            text = trimmedText,
                            mood = moodName,
                            createdAt = safeRecordedAt,
                            imageUris = imageUris.toDbString(),
                            audioUri = audioUri,
                            audioDurationMs = audioDurationMs,
                        ),
                    )
                }
            } else if (hasContent) {
                noteDao.insert(
                    DailyNoteEntity(
                        dateLabel = "",
                        text = trimmedText,
                        mood = moodName,
                        createdAt = safeRecordedAt,
                        moodEntryId = id,
                        imageUris = imageUris.toDbString(),
                        audioUri = audioUri,
                        audioDurationMs = audioDurationMs,
                    ),
                )
            }
            syncCurrentMood()
        }
        obsoleteAttachments.forEach(::deleteAttachment)
    }

    override suspend fun deleteCheckIn(id: Long) {
        ensureState()
        var attachments = emptyList<String>()
        database.withTransaction {
            attachments = noteDao.findByMoodEntryId(id)?.attachmentUris().orEmpty()
            moodEntryDao.deleteById(id)
            noteDao.deleteByMoodEntryId(id)
            syncCurrentMood()
        }
        attachments.forEach(::deleteAttachment)
    }

    override suspend fun setLanguage(language: AppLanguage) = updateState { copy(languageTag = language.tag) }

    override suspend fun setThemeMode(mode: ThemeMode) = updateState { copy(themeMode = mode.tag) }

    override suspend fun setRemindersEnabled(enabled: Boolean) = updateState { copy(remindersEnabled = enabled) }

    override suspend fun setWeeklyGoalDays(days: Int) = updateState { copy(weeklyGoalDays = days.coerceIn(3, 7)) }

    override suspend fun setProfile(nickname: String, avatarUri: String?) {
        val safeNickname = nickname.trim().ifBlank { AppStateEntity().nickname }
        var obsoleteAvatar: String? = null
        stateMutex.withLock {
            val current = stateDao.get() ?: AppStateEntity()
            if (current.avatarUri != null && current.avatarUri != avatarUri) {
                obsoleteAvatar = current.avatarUri
            }
            stateDao.upsert(
                current.copy(
                    nickname = safeNickname,
                    avatarUri = avatarUri,
                ),
            )
        }
        obsoleteAvatar?.let(::deleteAttachment)
    }

    override suspend fun resetToday() {
        ensureState()
        val todayStart = startOfDay(System.currentTimeMillis())
        val tomorrowStart = nextStartOfDay(todayStart)
        val attachments = mutableListOf<String>()
        database.withTransaction {
            attachments += noteDao.findBetween(todayStart, tomorrowStart).flatMap { it.attachmentUris() }
            moodEntryDao.deleteBetween(todayStart, tomorrowStart)
            noteDao.deleteBetween(todayStart, tomorrowStart)
            stateDao.resetToday()
        }
        attachments.forEach(::deleteAttachment)
    }

    private fun dayTicker(): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            val now = System.currentTimeMillis()
            val todayStart = startOfDay(now)
            emit(todayStart)
            delay((nextStartOfDay(todayStart) - now).coerceAtLeast(1_000L).milliseconds)
        }
    }

    private fun minuteTicker(): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(System.currentTimeMillis())
            delay(60.seconds)
        }
    }

    private fun startOfRecentWindow(): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(System.currentTimeMillis())
        add(Calendar.DAY_OF_YEAR, -(RECENT_DAYS - 1))
    }.timeInMillis

    private fun DailyNoteEntity.attachmentUris(): List<String> = imageUris.toUriList() + listOfNotNull(audioUri)

    private fun buildTodaySnapshot(
        notes: List<DailyNoteEntity>,
        state: AppStateEntity?,
        moodEntries: List<MoodEntryEntity>,
        now: Long,
    ): TodayDataSnapshot {
        val noteModels = notes
            .filter { it.createdAt <= now }
            .map { it.toModel() }
        val records = moodEntries
            .filter { it.recordedAt <= now }
            .map { it.toModel() }
        val notesByEntryId = noteModels.mapNotNull { note ->
            note.moodEntryId?.let { it to note }
        }.toMap()
        val saved = state ?: AppStateEntity()
        return TodayDataSnapshot(
            mood = records.firstOrNull()?.mood ?: saved.mood.toMood(),
            todayNotes = noteModels,
            todayMoodRecords = records.map { record ->
                val note = notesByEntryId[record.id]
                record.copy(
                    momentText = note?.text,
                    imageUris = note?.imageUris ?: emptyList(),
                    audioUri = note?.audioUri,
                    audioDurationMs = note?.audioDurationMs ?: 0L,
                )
            },
            moodValue = records.firstOrNull()?.moodValue ?: saved.mood.toMoodValue(),
            energyLevel = records.firstOrNull()?.energyLevel,
        )
    }

    private fun deleteAttachment(uriString: String) {
        val uri = uriString.toUri()
        if (uri.scheme != "file") return
        val file = uri.path?.let(::File) ?: return
        val root = File(context.filesDir, "attachments").canonicalFile
        val target = runCatching { file.canonicalFile }.getOrNull() ?: return
        if (target.path.startsWith(root.path + File.separator)) target.delete()
    }

    private suspend fun updateState(transform: AppStateEntity.() -> AppStateEntity) {
        stateMutex.withLock {
            stateDao.upsert((stateDao.get() ?: AppStateEntity()).transform())
        }
    }

    private suspend fun syncCurrentMood() {
        val now = System.currentTimeMillis()
        val latestToday = moodEntryDao.latestBetween(startOfDay(now), now)
        stateDao.updateMood(latestToday?.mood ?: Mood.STEADY.name)
    }

    private fun List<String>.toDbString(): String? = if (isEmpty()) null else joinToString("|")

    private fun String?.toUriList(): List<String> = this?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

    private fun DailyNoteEntity.toModel() = DailyNote(
        id = id,
        date = dateLabel,
        text = text,
        mood = mood.toMood(),
        createdAt = createdAt,
        moodEntryId = moodEntryId,
        imageUris = imageUris.toUriList(),
        audioUri = audioUri,
        audioDurationMs = audioDurationMs,
    )

    private fun MoodEntryEntity.toModel() = MoodRecord(id, mood.toMood(), moodValue, energyLevel, recordedAt)

    private fun String.toMood() = runCatching { Mood.valueOf(this) }.getOrDefault(Mood.STEADY)

    private fun String.toMoodValue() = toMood().toMoodValue()

    private fun startOfDay(time: Long): Long = Calendar
        .getInstance()
        .apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun nextStartOfDay(time: Long): Long = Calendar.getInstance().apply {
        timeInMillis = time
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}
