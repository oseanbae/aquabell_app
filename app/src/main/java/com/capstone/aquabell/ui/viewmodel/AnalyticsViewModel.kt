package com.capstone.aquabell.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.aquabell.data.FirebaseRepository
import com.capstone.aquabell.data.model.AnalyticsUiState
import com.capstone.aquabell.data.model.DailyAnalytics
import com.capstone.aquabell.data.model.SortOrder
import com.capstone.aquabell.data.model.LiveDataSnapshot
import com.capstone.aquabell.data.model.SensorLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private var currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private var lastComputedForDate: String? = null

    init {
        // Load historical once
        viewModelScope.launch { loadHistoricalOnly() }
        // Start live listener for today's averages
        viewModelScope.launch { observeTodayAndCompute() }
    }

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                withContext(Dispatchers.IO) {
                    val historicalData = loadHistoricalDailyLogs()
                    val sorted = if (_sortOrder.value == SortOrder.NEWEST_FIRST) historicalData.sortedByDescending { it.date } else historicalData.sortedBy { it.date }
                    _uiState.value = _uiState.value.copy(dailyAnalytics = sorted, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("AnalyticsViewModel", "Error loading analytics data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics data"
                )
            }
        }
    }

    private suspend fun loadHistoricalOnly() {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val historical = withContext(Dispatchers.IO) { loadHistoricalDailyLogs() }
            val sorted = if (_sortOrder.value == SortOrder.NEWEST_FIRST) historical.sortedByDescending { it.date } else historical.sortedBy { it.date }
            _uiState.value = _uiState.value.copy(dailyAnalytics = sorted, isLoading = false)
        } catch (t: Throwable) {
            Log.e("AnalyticsViewModel", "Historical load failed: ${t.message}", t)
            _uiState.value = _uiState.value.copy(isLoading = false, error = t.message)
        }
    }

    private suspend fun observeTodayAndCompute() {
        repository.observeTodaySensorLogs().collect { logs ->
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            // Day rollover detection
            if (currentDate != today) {
                // Persist previous day's final average if we computed it
                lastComputedForDate?.let { prevDate ->
                    val prev = _uiState.value.dailyAnalytics.firstOrNull { it.date == prevDate && it.isLive }
                    if (prev != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            try { repository.saveDailyAnalytics(prev.copy(isLive = false)) } catch (e: Exception) { Log.e("AnalyticsViewModel", "Failed to save day summary: ${e.message}") }
                        }
                    }
                }
                // Reset and reload historical for new day
                currentDate = today
                viewModelScope.launch { loadHistoricalOnly() }
            }

            if (logs.isEmpty()) return@collect

            val avg = calculateAverages(logs)
            lastComputedForDate = today
            val liveItem = avg.copy(date = today, isLive = true)

            // Merge into list: remove any existing live for today, add new
            val withoutToday = _uiState.value.dailyAnalytics.filterNot { it.date == today && it.isLive }
            val merged = withoutToday + liveItem
            val sorted = if (_sortOrder.value == SortOrder.NEWEST_FIRST) merged.sortedByDescending { it.date } else merged.sortedBy { it.date }
            _uiState.value = _uiState.value.copy(dailyAnalytics = sorted)
        }
    }

    private suspend fun loadHistoricalDailyLogs(): List<DailyAnalytics> {
        return try {
            val fromFirestore = repository.getDailyLogs()
            if (fromFirestore.isNotEmpty()) return fromFirestore
            // Optional mock data fallback for development/testing only
            //TODO: DELETE THIS
            listOf(
                DailyAnalytics(date = "2025-10-18", waterTemp = 25.8, airTemp = 27.4, airHumidity = 80.1, pH = 7.0, dissolvedOxygen = 6.2, turbidityNTU = 12.5, isLive = false),
                DailyAnalytics(date = "2025-10-19", waterTemp = 26.2, airTemp = 28.3, airHumidity = 81.4, pH = 7.1, dissolvedOxygen = 5.9, turbidityNTU = 10.9, isLive = false),
                DailyAnalytics(date = "2025-10-20", waterTemp = 26.7, airTemp = 29.0, airHumidity = 82.0, pH = 7.2, dissolvedOxygen = 5.8, turbidityNTU = 9.8, isLive = false)
            )
        } catch (e: Exception) {
            Log.e("AnalyticsViewModel", "Error loading historical data: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun getTodaySensorLogs(): List<SensorLog> {
        return try {
            repository.getTodaySensorLogs()
        } catch (e: Exception) {
            Log.e("AnalyticsViewModel", "Error fetching today's sensor logs: ${e.message}", e)
            emptyList()
        }
    }

    private fun calculateAverages(logs: List<SensorLog>): DailyAnalytics {
        if (logs.isEmpty()) {
            return DailyAnalytics(date = "", isLive = true)
        }

        return DailyAnalytics(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            airTemp = logs.map { it.airTemp }.average(),
            airHumidity = logs.map { it.airHumidity }.average(),
            waterTemp = logs.map { it.waterTemp }.average(),
            pH = logs.map { it.pH }.average(),
            dissolvedOxygen = logs.map { it.dissolvedOxygen }.average(),
            turbidityNTU = logs.map { it.turbidityNTU }.average(),
            isLive = true
        )
    }

    fun toggleSortOrder() {
        val newOrder = if (_sortOrder.value == SortOrder.NEWEST_FIRST) {
            SortOrder.OLDEST_FIRST
        } else {
            SortOrder.NEWEST_FIRST
        }
        _sortOrder.value = newOrder
        
        // Re-sort the data
        val currentData = _uiState.value.dailyAnalytics
        val sortedData = if (newOrder == SortOrder.NEWEST_FIRST) {
            currentData.sortedByDescending { it.date }
        } else {
            currentData.sortedBy { it.date }
        }
        
        _uiState.value = _uiState.value.copy(dailyAnalytics = sortedData)
    }

    fun refresh() {
        loadAnalyticsData()
    }
}
