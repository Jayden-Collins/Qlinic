package com.example.qlinic.ui.navigation

import android.net.Uri

sealed class Routes(val route: String){
    object Home : Routes("home")
    object Report : Routes("report")
    object Notification : Routes("notification")
    object DoctorCalendar : Routes("DoctorCalendar")
    object Profile : Routes("profile")
    // create a schedule appointment screen when staff selects press view
    object DoctorAppointmentSchedule: Routes("doctor_appointment_schedule/{doctorID}"){
        fun createRoute(doctorID: String): String =
            "doctor_appointment_schedule/${Uri.encode(doctorID)}"
    }
    // create a reschedule appointment screen when staff selects press reschedule
    object DoctorAppointmentReschedule: Routes("doctor_appointment_reschedule/{appointmentID}/{doctorID}?rescheduleDate={rescheduleDate}"){
        fun createRoute(appointmentID: String, doctorID: String, rescheduleDateMillis: Long? = null): String {
            val base = "doctor_appointment_reschedule/${Uri.encode(appointmentID)}/${Uri.encode(doctorID)}"
            return if (rescheduleDateMillis != null) "$base?rescheduleDate=${rescheduleDateMillis}" else base
        }
    }

}