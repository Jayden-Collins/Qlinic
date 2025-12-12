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

    // private val _peakHoursInfo = MutableStateFlow(PeakHoursInfo())
    // val peakHoursInfo: StateFlow<PeakHoursInfo> = _peakHoursInfo.asStateFlow()

    private val _peakHoursReportData = MutableStateFlow(PeakHoursReportData())
    val peakHoursReportData: StateFlow<PeakHoursReportData> = _peakHoursReportData.asStateFlow()

    private val userId: String? = "cp50kt2wZWW2TdDmYFCu"
    // private val userId: String? = Firebase.auth.currentUser?.uid


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

    private fun loadPreCalculatedStats(reportType: String, savedReport: ReportDocument) {
        when (reportType) {
            "Weekly" -> {
                _stats.value = savedReport.weeklyStats
                _peakHoursReportData.value = savedReport.weeklyPeakHours
            }
            "Monthly" -> {
                _stats.value = savedReport.monthlyStats
                _peakHoursReportData.value = savedReport.monthlyPeakHours
            }
            "Yearly" -> {
                _stats.value = savedReport.yearlyStats
                _peakHoursReportData.value = savedReport.yearlyPeakHours
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
            val result = repository.getStatistics(
                "Custom Date Range",
                _filterState.value.selectedDepartment,
                startDate,
                endDate
            )
            // Note: We do not save custom range results to the main document.
            val chartResult = repository.getPeakHoursReportData(
                "Custom Date Range",
                _filterState.value.selectedDepartment,
                startDate,
                endDate
            )
            _stats.value = result
            _peakHoursReportData.value = chartResult
            _isLoading.value = false // Hide loading
        }
    }

    private fun fetchAndCreateInitialDocument() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch all three sets of stats from the repository.
                val weekly = repository.getStatistics("Weekly", "All", "", "")
                val monthly = repository.getStatistics("Monthly", "All", "", "")
                val yearly = repository.getStatistics("Yearly", "All", "", "")
                val weeklyPeak = repository.getPeakHoursReportData("Weekly", "All", "", "")
                val monthlyPeak = repository.getPeakHoursReportData("Monthly", "All", "", "")
                val yearlyPeak = repository.getPeakHoursReportData("Yearly", "All", "", "")

                // Create the complete document with all the fetched data.
                val initialReport = ReportDocument(
                    filters = ReportFilterState(),
                    weeklyStats = weekly,
                    monthlyStats = monthly,
                    yearlyStats = yearly,
                    weeklyPeakHours = weeklyPeak,
                    monthlyPeakHours = monthlyPeak,
                    yearlyPeakHours = yearlyPeak
                )

                // Use .set() only once to create the document.
                userId?.let { db.collection("report").document(it).set(initialReport).await() }

                _filterState.value = initialReport.filters
                _stats.value = initialReport.weeklyStats // Default to weekly
                _peakHoursReportData.value = initialReport.weeklyPeakHours

            } catch (e: Exception) {
                Log.e("Firestore", "Error creating initial document", e)
            }
            _isLoading.value = false // Hide loading
        }
    }

    private fun saveReportDocument(report: ReportDocument) {
        userId?.let { id ->
            viewModelScope.launch {
                try {
                    db.collection("report").document(id).set(report, SetOptions.merge()).await()
                    Log.d("Firestore", "Full report document for user $id saved successfully.")
                } catch (e: Exception) {
                    Log.e("Firestore", "Error saving full report for user $id", e)
                }
            }
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
                            loadPreCalculatedStats(savedReport.filters.selectedType, savedReport)
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
            } ?: run {
                Log.d("Firestore", "No user ID. Cannot load or save.")
            }
            _isLoading.value = false // Stop loading once data is processed
        }
    }
}