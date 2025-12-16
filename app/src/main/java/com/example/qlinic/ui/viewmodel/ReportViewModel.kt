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

    private val departments = listOf("All Department", "Cardiology", "Dermatology", "Gastroenterology", "Gynecologist", "Neurology", "Orthopedics")

    private val userId: String? = "cp50kt2wZWW2TdDmYFCu"

    init {
        loadReportDocument()
    }

    fun updateFilter(newState: ReportFilterState) {
        _filterState.value = newState
        saveFilters(newState)

        if (newState.selectedType == "Custom Date Range") {
            fetchDataForCustomRange(newState.startDate, newState.endDate)
        } else {
            // When switching between Weekly/Monthly/Yearly, just reload the whole document.
            // This is safe, fast, and ensures data consistency.
            loadReportDocument()
        }
    }

    // Example logic to change date
    fun updateDate(isStart: Boolean, date: String) {
        val newFilterState = if (isStart) {
            _filterState.value.copy(startDate = date)
        } else {
            _filterState.value.copy(endDate = date)
        }
        // After updating the date, save the new state and refetch the data.
        updateFilter(newFilterState)
    }

    private fun loadPreCalculatedStats(reportType: String, department: String, savedReport: ReportDocument) {
        when (reportType) {
            "Weekly" -> {
                _stats.value = savedReport.weeklyStats[department] ?: AppointmentStatistics()
                _peakHoursReportData.value = savedReport.weeklyPeakHours[department] ?: PeakHoursReportData()
            }
            "Monthly" -> {
                _stats.value = savedReport.monthlyStats[department] ?: AppointmentStatistics()
                _peakHoursReportData.value = savedReport.monthlyPeakHours[department] ?: PeakHoursReportData()
            }
            "Yearly" -> {
                _stats.value = savedReport.yearlyStats[department] ?: AppointmentStatistics()
                _peakHoursReportData.value = savedReport.yearlyPeakHours[department] ?: PeakHoursReportData()
            }
            else -> {
                _stats.value = AppointmentStatistics()
                _peakHoursReportData.value = PeakHoursReportData()
            }
        }
    }


    private fun fetchDataForCustomRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getStatistics("Custom Date Range", _filterState.value.selectedDepartment, startDate, endDate)
            val chartResult = repository.getPeakHoursReportData("Custom Date Range", _filterState.value.selectedDepartment, startDate, endDate)
            _stats.value = result
            _peakHoursReportData.value = chartResult
            _isLoading.value = false
        }
    }

    private fun fetchAndCreateInitialDocument() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create maps to hold the data for all departments
                val weeklyStatsMap = mutableMapOf<String, AppointmentStatistics>()
                val monthlyStatsMap = mutableMapOf<String, AppointmentStatistics>()
                val yearlyStatsMap = mutableMapOf<String, AppointmentStatistics>()
                val weeklyPeakMap = mutableMapOf<String, PeakHoursReportData>()
                val monthlyPeakMap = mutableMapOf<String, PeakHoursReportData>()
                val yearlyPeakMap = mutableMapOf<String, PeakHoursReportData>()

                // Loop through each department and fetch its specific data
                for (department in departments) {
                    weeklyStatsMap[department] = repository.getStatistics("Weekly", department, "", "")
                    monthlyStatsMap[department] = repository.getStatistics("Monthly", department, "", "")
                    yearlyStatsMap[department] = repository.getStatistics("Yearly", department, "", "")
                    weeklyPeakMap[department] = repository.getPeakHoursReportData("Weekly", department, "", "")
                    monthlyPeakMap[department] = repository.getPeakHoursReportData("Monthly", department, "", "")
                    yearlyPeakMap[department] = repository.getPeakHoursReportData("Yearly", department, "", "")
                }

                // Create the complete document with the new map structure
                val initialReport = ReportDocument(
                    filters = ReportFilterState(),
                    weeklyStats = weeklyStatsMap,
                    monthlyStats = monthlyStatsMap,
                    yearlyStats = yearlyStatsMap,
                    weeklyPeakHours = weeklyPeakMap,
                    monthlyPeakHours = monthlyPeakMap,
                    yearlyPeakHours = yearlyPeakMap
                )

                // Use .set() only once to create the document.
                userId?.let { db.collection("report").document(it).set(initialReport).await() }

                _filterState.value = initialReport.filters
                _stats.value = initialReport.weeklyStats["All Department"] ?: AppointmentStatistics()
                _peakHoursReportData.value = initialReport.weeklyPeakHours["All Department"] ?: PeakHoursReportData()
            }  catch (e: Exception) {
                Log.e("Firestore", "Error creating initial document", e)
            }
            _isLoading.value = false // Hide loading
        }
    }

    private fun saveFilters(filterToSave: ReportFilterState) {
        userId?.let { id ->
            viewModelScope.launch {
                try {
                    db.collection("report").document(id).update("filters", filterToSave).await()
                } catch (e: Exception) {
                    Log.e("Firestore", "Error updating filters for user $id", e)
                }
            }
        }
    }

    private fun loadReportDocument() {
        viewModelScope.launch {
            _isLoading.value = true // Start loading
            userId?.let { id ->
                try {
                    val snapshot = db.collection("report").document(id).get().await()

                    if (snapshot.exists()) {
                        val savedReport = snapshot.toObject(ReportDocument::class.java)
                        if (savedReport != null) {
                            Log.d("Firestore", "Successfully loaded full report for user $id.")
                            _filterState.value = savedReport.filters
                            // After loading, immediately display the correct stats for the loaded filter type
                            loadPreCalculatedStats(savedReport.filters.selectedType, savedReport.filters.selectedDepartment, savedReport)
                        }
                    } else {
                        // Document does NOT exist, so create it for the first time.
                        Log.d("Firestore", "No saved report found. Creating initial document.")
                        fetchAndCreateInitialDocument()
                        return@launch // Stop here so isLoading is handled by the other function
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error loading report for user $id", e)
                    // It's safer to not create a new doc on a temporary network error.
                    // Just show empty state.
                }
            }
            _isLoading.value = false // Stop loading once data is processed
        }
    }
}