package com.example.qlinic.ui.ui_state

import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.UserRole
import java.time.YearMonth

data class HomeUiState(
    val isLoading: Boolean = false,
    val appointmentItems: List<AppointmentCardUiState> = emptyList(),
    val error: String? = null,
    val selectedTab: AppointmentStatus = AppointmentStatus.UPCOMING,
    val showTabs: Boolean = true,
    val userId: String = "",
    val userRole: UserRole = UserRole.PATIENT,
    val currentYearMonth: YearMonth = YearMonth.now()
)

data class AppointmentCardUiState(
    val id: String,
    val rawAppointment: Appointment,
    val displayName: String,
    val displaySubtitle: String,
    val displayImageUrl: String?,
    val displayStatus: AppointmentStatus,
    val timeString: String,
    val isActionEnabled: Boolean
)
