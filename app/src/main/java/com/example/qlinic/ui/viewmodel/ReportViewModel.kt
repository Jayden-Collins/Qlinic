package com.example.qlinic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.AppointmentStatistics
import com.example.qlinic.data.model.ChartData
import com.example.qlinic.data.model.PeakHoursReportData
import com.example.qlinic.data.model.ReportDocument
import com.example.qlinic.data.model.ReportFilterState
import com.example.qlinic.data.repository.AppointmentRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReportViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val repository = AppointmentRepository()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // UI State for Filters
    private val _filterState = MutableStateFlow(ReportFilterState())
    val filterState: StateFlow<ReportFilterState> = _filterState.asStateFlow()

    // UI State for Data
    private val _stats = MutableStateFlow(AppointmentStatistics(0, 0, 0))
    val stats: StateFlow<AppointmentStatistics> = _stats.asStateFlow()

    private val _peakHoursReportData = MutableStateFlow(PeakHoursReportData())
    val peakHoursReportData: StateFlow<PeakHoursReportData> = _peakHoursReportData.asStateFlow()

    private val userId: String? = Firebase.auth.currentUser?.uid

    init {
        if (userId != null) {
            loadFiltersAndFetchInitialData()
        } else {
            _isLoading.value = false
        }
    }

    fun updateFilter(newState: ReportFilterState) {
        _filterState.value = newState
        saveFilters(newState)
        // Whenever a filter changes, fetch the new data live.
        fetchDataForCurrentFilter()
    }

    fun updateDate(isStart: Boolean, date: String) {
        val newFilterState = if (isStart) {
            _filterState.value.copy(startDate = date)
        } else {
            _filterState.value.copy(endDate = date)
        }
        updateFilter(newFilterState)
    }

    private fun fetchDataForCurrentFilter() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentFilters = _filterState.value
            // Fetch both stats and peak hours data based on the current filter state.
            try {
                // Fetch both statistics and peak hours data from the repository in parallel.
                val statsResult = repository.getStatistics(
                    currentFilters.selectedType,
                    currentFilters.selectedDepartment,
                    currentFilters.startDate,
                    currentFilters.endDate
                )
                val peakHoursResult = repository.getPeakHoursReportData(
                    currentFilters.selectedType,
                    currentFilters.selectedDepartment,
                    currentFilters.startDate,
                    currentFilters.endDate
                )

                // Update the UI state with the new data.
                _stats.value = statsResult
                _peakHoursReportData.value = peakHoursResult

            } catch (e: Exception) {
                Log.e("ViewModelFetch", "Error fetching report data", e)
                // Optionally reset to empty state on error
                _stats.value = AppointmentStatistics()
                _peakHoursReportData.value = PeakHoursReportData()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveFilters(filterToSave: ReportFilterState) {
        userId?.let { id ->
            viewModelScope.launch {
                try {
                    // We only save the filters object now.
                    db.collection("report").document(id).set(mapOf("filters" to filterToSave)).await()
                } catch (e: Exception) {
                    Log.e("Firestore", "Error saving filters", e)
                }
            }
        }
    }

    private fun loadFiltersAndFetchInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            userId?.let { id ->
                try {
                    val snapshot = db.collection("report").document(id).get().await()
                    if (snapshot.exists()) {
                        val savedFilters = snapshot.get("filters", ReportFilterState::class.java)
                        _filterState.value = savedFilters ?: ReportFilterState()
                    } else {
                        // If no document exists, save the default filters for the first time.
                        saveFilters(ReportFilterState())
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error loading filters", e)
                }
            }
            // After loading filters (or using defaults), fetch the initial data for the screen.
            fetchDataForCurrentFilter()
        }
    }
}