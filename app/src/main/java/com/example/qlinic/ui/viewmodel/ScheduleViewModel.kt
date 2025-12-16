package com.example.qlinic.ui.viewmodel
/**
 * This is a mock up View Model solely build for testing, replace with desmond eh
 **/
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.DoctorProfile
import com.example.qlinic.data.repository.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI State for the list screen
data class ScheduleUiState(
    val isLoading: Boolean = false,
    val doctors: List<DoctorProfile> = emptyList(),
    val searchQuery: String = ""
)

class ScheduleViewModel(
    private val repository: DoctorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Holds the detail of the selected doctor
    private val _selectedDoctor = MutableStateFlow<DoctorProfile?>(null)
    val selectedDoctor: StateFlow<DoctorProfile?> = _selectedDoctor.asStateFlow()

    init {
        loadDoctors()
    }

    fun loadDoctors() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val doctors = repository.getAllDoctors()
            _uiState.update { it.copy(isLoading = false, doctors = doctors) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // In a real app, you would filter the list here
    }

    // Called when user clicks a card
    fun selectDoctor(doctorId: String) {
        viewModelScope.launch {
            val doctor = repository.getDoctorById(doctorId)
            _selectedDoctor.value = doctor
        }
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