package com.example.qlinic.ui.ui_state

import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.User
import java.time.YearMonth

data class HomeUiState(
    val isLoading: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val error: String? = null,
    val selectedTab: AppointmentStatus = AppointmentStatus.UPCOMING,
    val showTabs: Boolean = true, // False for Doctors/Staff
    val currentUser: User? = null,
    val currentYearMonth: YearMonth = YearMonth.now()
)