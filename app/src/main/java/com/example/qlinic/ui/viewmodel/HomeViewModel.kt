package com.example.qlinic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.SessionManager
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
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel(
    private val repository: AppointmentRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val currentUserId: String = sessionManager.getSavedUserId() ?: sessionManager.getSavedStaffId() ?: ""
    private val currentUserRole: UserRole = when {
        sessionManager.getSavedUserType()?.equals("PATIENT", ignoreCase = true) == true -> UserRole.PATIENT
        sessionManager.getSavedRole()?.equals("DOCTOR", ignoreCase = true) == true -> UserRole.DOCTOR
        else -> UserRole.STAFF
    }

    private val _uiState = MutableStateFlow(
        HomeUiState(
            userId = currentUserId,
            userRole = currentUserRole
        )
    )

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val pattern = "EEE, MMM dd, yyyy - hh:mm a"

    init {
        Log.d("HomeVM", "Init - User ID: '$currentUserId'")
        Log.d("HomeVM", "Init - UserType from Session: ${sessionManager.getSavedUserType()}")
        Log.d("HomeVM", "Init - Role from Session: ${sessionManager.getSavedRole()}")
        Log.d("HomeVM", "Init - Resolved Role: $currentUserRole")
        
        _uiState.update { it.copy(showTabs = true) }
        loadAppointments(_uiState.value.selectedTab)
    }

    private fun loadAppointments(status: AppointmentStatus) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            Log.d("HomeVM", "loadAppointments - Start. Role: $currentUserRole, Status: $status, UID: $currentUserId")
            
            val rawData = when (currentUserRole) {
                UserRole.PATIENT -> {
                    if (currentUserId.isBlank()) {
                        Log.e("HomeVM", "PATIENT role but currentUserId is empty!")
                    }
                    Log.d("HomeVM", "Fetching appointments for Patient: $currentUserId")
                    repository.getAppointmentsForPatient(currentUserId, status)
                }
                UserRole.DOCTOR -> {
                    Log.d("HomeVM", "Fetching appointments for Doctor: $currentUserId")
                    repository.getAppointmentsForDoctor(currentUserId, status)
                }
                UserRole.STAFF -> {
                    Log.d("HomeVM", "Fetching all appointments (STAFF view)")
                    repository.getAllAppointments(status)
                }
            }
            
            Log.d("HomeVM", "loadAppointments - Received ${rawData.size} items from repository")
            val uiItems = mapToUiState(rawData, currentUserRole)
            _uiState.update { it.copy(isLoading = false, appointmentItems = uiItems) }
        }
    }

    private fun mapToUiState(
        appointments: List<Appointment>,
        role: UserRole
    ): List<AppointmentCardUiState> {
        return appointments.map { appt ->
            val (name, subtitle, img) = if (role == UserRole.PATIENT) {
                val doc = appt.doctor
                Triple(
                    if (doc != null) "Dr. ${doc.firstName} ${doc.lastName}" else "Loading...",
                    appt.doctorSpecialty ?: "General Practice",
                    doc?.imageUrl
                )
            } else {
                val pat = appt.patient
                Triple("${pat?.firstName} ${pat?.lastName}", pat?.gender ?: "", pat?.imageUrl)
            }

            val doctorFullName = appt.doctor?.let { "Dr. ${it.firstName} ${it.lastName}" } ?: "Doctor info unavailable"

            val isStarted = isAppointmentStarted(appt.dateTime)
            val isEnded = isAppointmentEnded(appt.dateTime)
            val realStatus = if (role == UserRole.DOCTOR && appt.status == AppointmentStatus.UPCOMING && isStarted) {
                AppointmentStatus.ONGOING
            } else if (appt.status == AppointmentStatus.UPCOMING && isStarted && !isEnded) {
                AppointmentStatus.ONGOING
            } else {
                appt.status
            }

            // Parse appointment start and end times
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            val apptStart = try { LocalDateTime.parse(appt.dateTime, formatter) } catch (e: Exception) { LocalDateTime.now().minusYears(1) }
            val apptEnd = apptStart.plusMinutes(30) // Default to 30 minutes duration
            val nowTime = LocalDateTime.now()
            val isOngoing = nowTime.isAfter(apptStart) && nowTime.isBefore(apptEnd)
            val isFuture = nowTime.isBefore(apptStart)

            val buttonsEnabled = if (role == UserRole.DOCTOR) {
                isOngoing // Only enable if appointment is currently ongoing
            } else if (role == UserRole.PATIENT || role == UserRole.STAFF) {
                !isOngoing && isFuture // Only enable for future appointments
            } else {
                false
            }
            val timeStr = parseTime(appt.dateTime)

            AppointmentCardUiState(
                id = appt.id,
                rawAppointment = appt,
                displayName = name,
                displaySubtitle = subtitle,
                displayImageUrl = img,
                displayStatus = realStatus,
                timeString = timeStr,
                isActionEnabled = buttonsEnabled,
                doctorFullName = doctorFullName // Pass doctor's full name
            )
        }
    }

    fun onTabSelected(status: AppointmentStatus) {
        _uiState.update { it.copy(selectedTab = status) }
        loadAppointments(status)
    }

    fun onAppointmentAction(appointmentId: String, action: String) {
        viewModelScope.launch {
            when (action) {
                "Complete" -> repository.updateAppointmentStatus(appointmentId, "Completed")
                "NoShow" -> repository.updateAppointmentStatus(appointmentId, "No Show")
                "Undo" -> repository.updateAppointmentStatus(appointmentId, "Upcoming")
                "Cancel" -> repository.updateAppointmentStatus(appointmentId, "Cancelled")
            }
            loadAppointments(_uiState.value.selectedTab)
        }
    }

    fun isAppointmentStarted(dateString: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            val apptTime = LocalDateTime.parse(dateString, formatter)
            LocalDateTime.now().isAfter(apptTime)
        } catch (e: Exception) {
            true
        }
    }

    private fun isAppointmentEnded(dateString: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            val apptTime = LocalDateTime.parse(dateString, formatter)
            val endTime = apptTime.plusMinutes(30) // Assuming 30 minute duration
            LocalDateTime.now().isAfter(endTime)
        } catch (e: Exception) {
            true // If parsing fails, assume it's ended to be safe
        }
    }

    fun onNextMonth() {
        _uiState.update { it.copy(currentYearMonth = it.currentYearMonth.plusMonths(1)) }
    }

    fun onPreviousMonth() {
        _uiState.update { it.copy(currentYearMonth = it.currentYearMonth.minusMonths(1)) }
    }

    fun getGroupedUiItems(): Map<LocalDate, List<AppointmentCardUiState>> {
        val currentMonth = _uiState.value.currentYearMonth
        return _uiState.value.appointmentItems
            .filter {
                val date = parseDateToLocalDate(it.rawAppointment.dateTime)
                date != null && date.month == currentMonth.month && date.year == currentMonth.year
            }
            .groupBy { parseDateToLocalDate(it.rawAppointment.dateTime)!! }
            .toSortedMap()
    }

    private fun parseTime(dateString: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            val date = LocalDateTime.parse(dateString, formatter)
            val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            date.format(timeFormat)
        } catch (e: Exception) {
            "00:00"
        }
    }

    private fun parseDateToLocalDate(dateString: String): LocalDate? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            LocalDateTime.parse(dateString, formatter).toLocalDate()
        } catch (e: Exception) { null }
    }
}

class HomeViewModelFactory(
    private val repository: AppointmentRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository, sessionManager) as T
    }
}
