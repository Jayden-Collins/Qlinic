package com.example.qlinic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.AppointmentStatistics
import com.example.qlinic.data.model.ReportDocument
import com.example.qlinic.data.model.ReportFilterState
import com.example.qlinic.data.repository.AppointmentRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val userId: String? = "cp50kt2wZWW2TdDmYFCu"
    // private val userId: String? = Firebase.auth.currentUser?.uid


    init {
        loadFiltersFromFirestore()
        fetchData()
    }

    fun updateFilter(newState: ReportFilterState) {
        _filterState.value = newState
        fetchData()
        saveFiltersToFirestore(newState)
    }

    // Example logic to change date
    fun updateDate(isStart: Boolean, date: String) {
        _filterState.update { currentState ->
            if (isStart) currentState.copy(startDate = date)
            else currentState.copy(endDate = date)
        }
        // After updating the date, save the new state and refetch the data.
        updateFilter(_filterState.value)
    }

    private fun fetchData() {
        viewModelScope.launch {
            val currentFilters = _filterState.value
            val result = repository.getStatistics(
                currentFilters.selectedType,
                currentFilters.selectedDepartment
            )
            _stats.value = result
        }
    }

    private fun saveFiltersToFirestore(filterToSave: ReportFilterState) {
        userId?.let { id ->
            viewModelScope.launch {
                try {
                    // Create the combined object to save
                    val reportDocument = ReportDocument(
                        filters = filterToSave,
                        statistics = _stats.value // Use the current stats value
                    )

                    db.collection("report").document(id)
                        .set(reportDocument) // Save the combined object
                        .await()
                    Log.d("Firestore", "Report document for user $id saved successfully.")
                } catch (e: Exception) {
                    Log.e("Firestore", "Error saving report document for user $id", e)
                }
            }
        }
    }

    private fun loadFiltersFromFirestore() {
        userId?.let { id ->
            viewModelScope.launch {
                try {
                    val snapshot = db.collection("report").document(id).get().await()
                    // Convert the document back into our new ReportDocument data class
                    val savedReport = snapshot.toObject(ReportDocument::class.java)

                    if (savedReport != null) {
                        // Update both the filter and stats state from the loaded document
                        _filterState.value = savedReport.filters
                        _stats.value = savedReport.statistics
                        Log.d("Firestore", "Successfully loaded report for user $id.")
                    } else {
                        Log.d("Firestore", "No saved report found for user $id. Using defaults.")
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error loading report for user $id", e)
                }
            }
        }
    }
}