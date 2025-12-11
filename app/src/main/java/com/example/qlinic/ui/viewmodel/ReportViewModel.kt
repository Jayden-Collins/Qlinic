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
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReportViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val repository = AppointmentRepository()

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

    private val userId: String = "cp50kt2wZWW2TdDmYFCu"
    // private val userId: String? = Firebase.auth.currentUser?.uid


    init {
        loadReportDocument()
    }

    fun updateFilter(newState: ReportFilterState) {
        _filterState.value = newState
        saveFilters(newState) // Always save the user's latest filter choice

        if (newState.selectedType == "Custom Date Range") {
            // For custom ranges, perform a live fetch from the repository.
            fetchDataForCustomRange(newState.startDate, newState.endDate)
        } else {
            // For pre-defined types, just display the stats we already have.
            loadPreCalculatedStats(newState.selectedType)
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

    private fun fetchData() {
        viewModelScope.launch {
            val currentFilters = _filterState.value
            // Fetch appointment stats
            val statsResult = repository.getStatistics(
                currentFilters.selectedType,
                currentFilters.selectedDepartment
            )
            _stats.value = statsResult

            val peakHoursResult = repository.getPeakHoursReportData(
                currentFilters.selectedType,
                currentFilters.selectedDepartment
            )
            _peakHoursReportData.value = peakHoursResult
        }
    }

    private fun loadPreCalculatedStats(reportType: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("report").document(userId!!).get().await()
                val savedReport = snapshot.toObject(ReportDocument::class.java)
                if (savedReport != null) {
                    _stats.value = when (reportType) {
                        "Weekly" -> savedReport.weeklyStats
                        "Monthly" -> savedReport.monthlyStats
                        "Yearly" -> savedReport.yearlyStats
                        else -> AppointmentStatistics()
                    }
                }
            } catch (e: Exception) {
                Log.e("Firestore", "Error switching to pre-calculated stats", e)
            }
        }
    }

    private fun fetchDataForCustomRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            val result = repository.getStatistics("Custom Date Range", _filterState.value.selectedDepartment)
            _stats.value = result
            // Note: We do not save custom range results to the main document.
            val chartResult = repository.getPeakHoursReportData(
                "Custom Date Range",
                _filterState.value.selectedDepartment
            )
            _peakHoursReportData.value = chartResult
        }
    }

    private fun fetchAndSaveAllReports() {
        viewModelScope.launch {
            try {
                // Fetch all three sets of stats from the repository.
                val weekly = repository.getStatistics("Weekly", "All")
                val monthly = repository.getStatistics("Monthly", "All")
                val yearly = repository.getStatistics("Yearly", "All")

                val chartData = repository.getPeakHoursReportData("Weekly", "All")

                // Create the complete document with all the fetched data.
                val fullReport = ReportDocument(
                    filters = _filterState.value,
                    weeklyStats = weekly,
                    monthlyStats = monthly,
                    yearlyStats = yearly
                )

                // Save the entire document to Firestore. This is the main "write" operation.
                saveReportDocument(fullReport)

                // Update the UI with the currently selected stats after fetching.
                loadPreCalculatedStats(_filterState.value.selectedType)

                _stats.value = when (_filterState.value.selectedType) {
                    "Weekly" -> weekly
                    "Monthly" -> monthly
                    "Yearly" -> yearly
                    else -> AppointmentStatistics()
                }
                _peakHoursReportData.value = chartData


            } catch (e: Exception) {
                Log.e("Firestore", "Error fetching and saving all reports", e)
            }
        }
    }

    private fun saveReportDocument(report: ReportDocument) {
        userId?.let { id ->
            viewModelScope.launch {
                try {
                    db.collection("report").document(id).set(report).await()
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
                    Log.d("Firestore", "Filters for user $id updated successfully.")
                } catch (e: Exception) {
                    Log.e("Firestore", "Error updating filters for user $id", e)
                }
            }
        }
    }

    private fun loadReportDocument() {
        userId?.let { id ->
            viewModelScope.launch {
                try {
                    val snapshot = db.collection("report").document(id).get().await()
                    val savedReport = snapshot.toObject(ReportDocument::class.java)

                    if (savedReport != null) {
                        _filterState.value = savedReport.filters
                        loadPreCalculatedStats(savedReport.filters.selectedType)
                        Log.d("Firestore", "Successfully loaded full report for user $id.")
                    } else {
                        Log.d("Firestore", "No saved report found. Fetching initial data.")
                        fetchAndSaveAllReports() // If no document, fetch and save everything.
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error loading report for user $id", e)
                    fetchAndSaveAllReports() // Also fetch if there's an error.
                }
            }
        } ?: run {
            Log.d("Firestore", "No user ID. Cannot load or save.")
        }
    }
}