package com.example.qlinic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.User
import com.example.qlinic.data.model.UserRole
import com.example.qlinic.data.repository.AppointmentRepository
import com.example.qlinic.ui.ui_state.AppointmentCardUiState
import com.example.qlinic.ui.ui_state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel(
    private val repository: AppointmentRepository,
    private val currentUser: User
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(currentUser = currentUser))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(showTabs = true) }
        loadAppointments(_uiState.value.selectedTab)
    }

    private fun loadAppointments(status: AppointmentStatus) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            var rawData = when (currentUser.role) {
                UserRole.PATIENT -> repository.getAppointmentsForPatient(currentUser.id, status)
                UserRole.DOCTOR -> repository.getAppointmentsForDoctor(currentUser.id, status)
                UserRole.STAFF -> repository.getAllAppointments(status)
            }

            if (status == AppointmentStatus.UPCOMING) {
                val (expired, active) = rawData.partition { shouldAutoComplete(it.dateTime) }

                if (expired.isNotEmpty()) {
                    expired.forEach { appt ->
                        launch {
                            repository.updateAppointmentStatus(appt.id, "Completed")
                            println("Auto-completing appointment: ${appt.id}")
                        }
                    }
                    rawData = active
                }
            }

            val uiItems = mapToUiState(rawData, currentUser.role)

            _uiState.update { it.copy(isLoading = false, appointmentItems = uiItems) }
        }
    }

    private fun shouldAutoComplete(date: java.util.Date): Boolean {
        return try {
            val apptTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            apptTime.plusMinutes(30).isBefore(LocalDateTime.now())
        } catch (e: Exception) {
            false
        }
    }

    private fun mapToUiState(
        appointments: List<Appointment>,
        role: UserRole
    ): List<AppointmentCardUiState> {
        return appointments.map { appt ->

            val (name, subtitle, img) = if (role == UserRole.PATIENT || role == UserRole.STAFF) {
                Triple(appt.doctor.name, appt.doctor.details ?: "", appt.doctor.imageUrl)
            } else {
                Triple(appt.patient.name, appt.patient.details ?: "", appt.patient.imageUrl)
            }

            val timeStr = parseTime(appt.dateTime)

            val isStarted = isAppointmentStarted(appt.dateTime)
            val realStatus = if (appt.status == AppointmentStatus.UPCOMING && isStarted) {
                AppointmentStatus.ONGOING
            } else {
                appt.status
            }

            val buttonsEnabled = if (role == UserRole.PATIENT || role == UserRole.STAFF) {
                !isStarted
            } else {
                isStarted
            }

            AppointmentCardUiState(
                id = appt.id,
                rawAppointment = appt,
                displayName = name,
                displaySubtitle = subtitle,
                displayImageUrl = img,
                displayStatus = realStatus,
                timeString = timeStr,
                isActionEnabled = buttonsEnabled
            )
        }
    }

    fun onTabSelected(status: AppointmentStatus) {
        _uiState.update { it.copy(selectedTab = status) }
        loadAppointments(status)
    }

    fun onAppointmentAction(appointmentId: String, action: String) {
        println("User clicked $action on appointment $appointmentId")

        viewModelScope.launch {
            when (action) {
                "Complete" -> repository.updateAppointmentStatus(appointmentId, "Completed")
                "NoShow" -> repository.updateAppointmentStatus(appointmentId, "Cancelled")
                "Undo" -> repository.updateAppointmentStatus(appointmentId, "Booked")
            }
            loadAppointments(_uiState.value.selectedTab)
        }
    }

//    fun isAppointmentStarted(dateString: String): Boolean {
//        return try {
//            val formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy - hh:mm a", Locale.getDefault())
//            val apptTime = LocalDateTime.parse(dateString, formatter)
//
//            LocalDateTime.now().isAfter(apptTime)
//        } catch (e: Exception) {
//            true
//        }
//    }

    fun onNextMonth() {
        _uiState.update { it.copy(currentYearMonth = it.currentYearMonth.plusMonths(1)) }
    }

    fun onPreviousMonth() {
        _uiState.update { it.copy(currentYearMonth = it.currentYearMonth.minusMonths(1)) }
    }

    private fun parseTime(date: java.util.Date): String {
        val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        return localDate.format(formatter)
    }

    // Check if Date object is in the past
    private fun isAppointmentStarted(date: java.util.Date): Boolean {
        val apptTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        return LocalDateTime.now().isAfter(apptTime)
    }

    // Extract LocalDate for grouping
    private fun parseDateToLocalDate(date: java.util.Date): LocalDate {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    // Update the grouping logic to use the new helper
    fun getGroupedUiItems(): Map<LocalDate, List<AppointmentCardUiState>> {
        val currentMonth = _uiState.value.currentYearMonth
        return _uiState.value.appointmentItems
            .filter {
                val date = parseDateToLocalDate(it.rawAppointment.dateTime)
                date.month == currentMonth.month && date.year == currentMonth.year
            }
            .groupBy { parseDateToLocalDate(it.rawAppointment.dateTime) }
            .toSortedMap()
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
