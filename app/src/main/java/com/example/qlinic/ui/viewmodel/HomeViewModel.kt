package com.example.qlinic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.User
import com.example.qlinic.data.model.UserRole
import com.example.qlinic.data.repository.AppointmentRepository
import com.example.qlinic.ui.ui_state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class HomeViewModel(
    private val repository: AppointmentRepository,
    private val currentUser: User

) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(currentUser = currentUser))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy - hh:mm a", Locale.getDefault())

    init {
        _uiState.update { it.copy(showTabs = currentUser.role == UserRole.PATIENT) }
        loadAppointments(_uiState.value.selectedTab)
    }

    private fun loadAppointments(status: AppointmentStatus) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val data = when (currentUser.role) {
                UserRole.PATIENT -> repository.getAppointmentsForPatient(currentUser.id, status)
                UserRole.DOCTOR -> repository.getAppointmentsForDoctor(currentUser.id)
                UserRole.STAFF -> repository.getAllUpcomingAppointments()
            }
            _uiState.update { it.copy(isLoading = false, appointments = data) }
        }
    }

    fun onTabSelected(status: AppointmentStatus) {
        _uiState.update { it.copy(selectedTab = status) }
        loadAppointments(status)
    }

    fun onAppointmentAction(appointmentId: String, action: String) {
        // Handle Cancel/Reschedule asynchronously if needed
        println("User clicked $action on appointment $appointmentId")
    }

    fun onNextMonth() {
        _uiState.update { it.copy(currentYearMonth = it.currentYearMonth.plusMonths(1)) }
    }

    fun onPreviousMonth() {
        _uiState.update { it.copy(currentYearMonth = it.currentYearMonth.minusMonths(1)) }
    }

    // --- NEW: Helper to group appointments for the UI ---
    // Returns a Map: [Date] -> [List of Appointments on that day]
    fun getGroupedAppointments(): Map<LocalDate, List<Appointment>> {
        val currentMonth = _uiState.value.currentYearMonth

        return _uiState.value.appointments
            .filter {
                // 1. Filter by Current Selected Month
                val date = parseDate(it.dateTime)
                date != null &&
                        date.month == currentMonth.month &&
                        date.year == currentMonth.year
            }
            .groupBy {
                // 2. Group by Day
                parseDate(it.dateTime)!!
            }
            .toSortedMap() // Sort by date (1st, 2nd, 3rd...)
    }

    // Helper to extract LocalDate from your String
    private fun parseDate(dateString: String): LocalDate? {
        return try {
            val date = dateFormat.parse(dateString)
            date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        } catch (e: Exception) {
            null
        }
    }

    // Helper to extract Time String (e.g. "10:00")
    fun parseTime(dateString: String): String {
        return try {
            val date = dateFormat.parse(dateString)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeFormat.format(date!!)
        } catch (e: Exception) {
            "00:00"
        }
    }
}

class HomeViewModelFactory(
    private val repository: AppointmentRepository,
    private val user: User
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, user) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
