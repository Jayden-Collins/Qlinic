package com.example.qlinic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.ClinicStaff
import com.example.qlinic.data.model.Doctor
import com.example.qlinic.data.repository.ClinicStaffRepository
import com.example.qlinic.data.repository.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI State for the list screen
data class ScheduleUiState(
    val isLoading: Boolean = false,
    val doctors: List<DoctorListItem> = emptyList(),
    val searchQuery: String = ""
)

data class DoctorListItem(
    val doctor: Doctor,
    val staff: ClinicStaff
)

class ScheduleViewModel(
    private val repository: DoctorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Holds the detail of the selected doctor
    private val _selectedDoctor = MutableStateFlow<Doctor?>(null)
    val selectedDoctor: StateFlow<Doctor?> = _selectedDoctor.asStateFlow()

    private val _selectedStaff = MutableStateFlow<ClinicStaff?>(null)
    val selectedStaff: StateFlow<ClinicStaff?> = _selectedStaff.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading: StateFlow<Boolean> = _isDetailLoading.asStateFlow()

    private val clinicStaffRepository = ClinicStaffRepository()

    init {
        loadDoctors()
    }

    fun loadDoctors() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val doctors = repository.getAllDoctors()
            val listItems = doctors.mapNotNull { doctor ->
                val staff = clinicStaffRepository.getStaffMember(doctor.doctorID)
                if (staff != null) DoctorListItem(doctor, staff) else null
            }
            _uiState.update { it.copy(isLoading = false, doctors = listItems) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // Called when user clicks a card
    fun selectDoctor(doctorId: String) {
        if (_selectedDoctor.value?.doctorID == doctorId && _selectedStaff.value != null) return
        
        _isDetailLoading.value = true
        viewModelScope.launch {
            val doctor = repository.getDoctorById(doctorId)
            _selectedDoctor.value = doctor
            if (doctor != null) {
                _selectedStaff.value = clinicStaffRepository.getStaffMember(doctor.doctorID)
            } else {
                _selectedStaff.value = null
            }
            _isDetailLoading.value = false
        }
    }

    fun clearSelectedDoctor() {
        _selectedDoctor.value = null
        _selectedStaff.value = null
        _isDetailLoading.value = false
    }
}

// Factory to create the ViewModel
class ScheduleViewModelFactory(private val repository: DoctorRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
