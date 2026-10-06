package me.heartrace.feature.patterns

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.InsightsDataSnapshot
import me.heartrace.data.MoodRecord
import me.heartrace.feature.metrics.calculateCurrentStreak
import me.heartrace.feature.metrics.calculateWeeklyGoalProgress
import me.heartrace.feature.metrics.dayStart
import me.heartrace.feature.metrics.shiftDays
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PatternsViewModel
@Inject
constructor(
    private val repository: HeartraceDataSource,
) : ViewModel() {
    var uiState by mutableStateOf(PatternsUiState())
        private set

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
        uiState = uiState.copy(editingRecord = record, errorMessage = null)
    }

    fun dismissEditRecord() {
        uiState = uiState.copy(editingRecord = null, errorMessage = null)
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
        uiState = uiState.copy(errorMessage = null)
        viewModelScope.launch {
            try {
                repository.updateCheckIn(
                    id = id,
                    moodValue = moodValue,
                    energyLevel = energyLevel,
                    momentText = momentText,
                    recordedAt = recordedAt,
                    imageUris = imageUris,
                    audioUri = audioUri,
                    audioDurationMs = audioDurationMs,
                )
                dismissEditRecord()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = "save_failed")
            }
        }
    }

    fun deleteRecord(id: Long) {
        uiState = uiState.copy(errorMessage = null)
        viewModelScope.launch {
            try {
                repository.deleteCheckIn(id)
                dismissEditRecord()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = "save_failed")
            }
        }
    }
}
