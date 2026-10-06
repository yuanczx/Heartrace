package me.heartrace.feature.patterns

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.InsightsDataSnapshot
import me.heartrace.data.MoodRecord
import me.heartrace.feature.attachments.DraftAttachmentStorage
import me.heartrace.feature.metrics.calculateCurrentStreak
import me.heartrace.feature.metrics.calculateWeeklyGoalProgress
import me.heartrace.feature.metrics.dayStart
import me.heartrace.feature.metrics.shiftDays
import me.heartrace.feature.today.EditRecordDraft
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PatternsViewModel
@Inject
constructor(
    private val repository: HeartraceDataSource,
    private val attachments: DraftAttachmentStorage,
) : ViewModel() {
    var uiState by mutableStateOf(PatternsUiState())
        private set

    private var hasBeenCleared = false

    private var latestSnapshot: InsightsDataSnapshot? = null
    private var selectedDateStart: Long? = null
    private var historyRange = HistoryRange.DAY

    init {
        viewModelScope.launch {
            repository.observeInsights().collect { snapshot ->
                latestSnapshot = snapshot
                refreshUiState()
            }
        }
    }

    fun selectHistoryDate(timestamp: Long) {
        selectedDateStart = dayStart(timestamp).coerceAtMost(dayStart(System.currentTimeMillis()))
        refreshUiState()
    }

    fun selectHistoryRange(range: HistoryRange) {
        historyRange = range
        refreshUiState()
    }

    fun showPreviousHistoryDate() {
        val current = selectedDateStart ?: dayStart(System.currentTimeMillis())
        selectedDateStart = shiftPeriod(current, -1)
        refreshUiState()
    }

    fun showNextHistoryDate() {
        val todayStart = dayStart(System.currentTimeMillis())
        val current = selectedDateStart ?: todayStart
        selectedDateStart = shiftPeriod(current, 1).coerceAtMost(todayStart)
        refreshUiState()
    }

    private fun refreshUiState() {
        val snapshot = latestSnapshot ?: return
        val now = System.currentTimeMillis()
        val todayStart = dayStart(now)
        if (selectedDateStart == null) {
            selectedDateStart = snapshot.records.firstOrNull()?.let { dayStart(it.recordedAt) } ?: todayStart
        }
        val selectedDay = selectedDateStart!!.coerceAtMost(todayStart)
        selectedDateStart = selectedDay

        val selectedPeriodStart = periodStart(selectedDay)
        val selectedPeriodEnd = shiftPeriod(selectedDay, 1)
        val selectedRecords = snapshot.records.filter {
            it.recordedAt >= selectedPeriodStart && it.recordedAt < selectedPeriodEnd
        }
        val windowStart = shiftDays(todayStart, -6)
        val periodRecords = when (historyRange) {
            HistoryRange.DAY -> snapshot.recentRecords.filter {
                it.recordedAt >= windowStart && it.recordedAt <= now
            }

            HistoryRange.MONTH, HistoryRange.YEAR -> selectedRecords
        }
        val recordsByDay = periodRecords.groupBy { dayStart(it.recordedAt) }
        val summaries = recordsByDay
            .map { (dayTimestamp, dayRecords) ->
                val avgMood = dayRecords.map { it.moodValue }.average().toInt()
                val avgEnergy = dayRecords.mapNotNull { it.energyLevel }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
                val calendar = Calendar.getInstance().apply { timeInMillis = dayTimestamp }
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                DailyRecordSummary(
                    dayStart = dayTimestamp,
                    dateLabel = "$month/$day",
                    averageMood = avgMood,
                    averageEnergy = avgEnergy,
                )
            }
            .sortedBy { it.dayStart }

        uiState = PatternsUiState(
            records = selectedRecords,
            hasHistory = snapshot.records.isNotEmpty(),
            notes = snapshot.notes,
            dailySummaries = summaries,
            activeDays = recordsByDay.size,
            averageEnergy = periodRecords.takeIf { it.any { record -> record.energyLevel != null } }?.mapNotNull { it.energyLevel }
                ?.average()?.toFloat(),
            averageMood = periodRecords.takeIf { it.isNotEmpty() }?.map { it.moodValue }
                ?.average()?.toInt(),
            recordCount = periodRecords.size,
            weeklyGoal = calculateWeeklyGoalProgress(snapshot.records, now, snapshot.weeklyGoalDays),
            currentStreak = calculateCurrentStreak(snapshot.records, now),
            selectedDateStart = selectedDay,
            historyRange = historyRange,
            canGoToNextDate = selectedPeriodStart < periodStart(todayStart),
            editingRecord = uiState.editingRecord,
            editingDraft = uiState.editingDraft,
            isUpdatingRecord = uiState.isUpdatingRecord,
            errorMessage = uiState.errorMessage,
        )
    }

    private fun periodStart(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = dayStart(timestamp)
        when (historyRange) {
            HistoryRange.DAY -> Unit

            HistoryRange.MONTH -> set(Calendar.DAY_OF_MONTH, 1)

            HistoryRange.YEAR -> {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }.timeInMillis

    private fun shiftPeriod(timestamp: Long, amount: Int): Long = Calendar.getInstance().apply {
        timeInMillis = periodStart(timestamp)
        when (historyRange) {
            HistoryRange.DAY -> add(Calendar.DAY_OF_YEAR, amount)
            HistoryRange.MONTH -> add(Calendar.MONTH, amount)
            HistoryRange.YEAR -> add(Calendar.YEAR, amount)
        }
    }.timeInMillis

    fun startEditRecord(record: MoodRecord) {
        if (uiState.isUpdatingRecord) return
        uiState.editingDraft?.discardAttachments(attachments)
        uiState = uiState.copy(editingRecord = record, editingDraft = EditRecordDraft(record), errorMessage = null)
    }

    fun updateEditDraft(record: MoodRecord, transform: (EditRecordDraft) -> EditRecordDraft) {
        val current = uiState.editingDraft
        val draft = transform(current?.takeIf { it.record.id == record.id } ?: EditRecordDraft(record))
        if (uiState.isUpdatingRecord || current?.record?.id != record.id) {
            (draft.newAttachments - current?.newAttachments.orEmpty()).forEach(attachments::delete)
            return
        }
        current.discardRemovedAttachments(draft, attachments)
        uiState = uiState.copy(editingDraft = draft)
    }

    fun dismissEditRecord() {
        if (uiState.isUpdatingRecord) return
        uiState.editingDraft?.discardAttachments(attachments)
        clearRecordEditor()
    }

    private fun clearRecordEditor() {
        uiState = uiState.copy(isUpdatingRecord = false, editingRecord = null, editingDraft = null, errorMessage = null)
    }

    fun updateRecord(
        id: Long,
        moodValue: Int,
        energyLevel: Int?,
        momentText: String?,
        recordedAt: Long,
        imageUris: List<String> = emptyList(),
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
    ) {
        if (uiState.isUpdatingRecord) return
        uiState = uiState.copy(isUpdatingRecord = true, errorMessage = null)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                try {
                    repository.updateCheckIn(id, moodValue, energyLevel, momentText, recordedAt, imageUris, audioUri, audioDurationMs)
                    clearRecordEditor()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (hasBeenCleared) uiState.editingDraft?.discardAttachments(attachments)
                    uiState = uiState.copy(isUpdatingRecord = false, errorMessage = "save_failed")
                }
            }
        }
    }

    fun deleteRecord(id: Long) {
        if (uiState.isUpdatingRecord) return
        uiState = uiState.copy(isUpdatingRecord = true, errorMessage = null)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                try {
                    repository.deleteCheckIn(id)
                    uiState.editingDraft?.discardAttachments(attachments)
                    clearRecordEditor()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (hasBeenCleared) uiState.editingDraft?.discardAttachments(attachments)
                    uiState = uiState.copy(isUpdatingRecord = false, errorMessage = "save_failed")
                }
            }
        }
    }

    override fun onCleared() {
        hasBeenCleared = true
        if (!uiState.isUpdatingRecord) uiState.editingDraft?.discardAttachments(attachments)
        super.onCleared()
    }
}
