package com.example.qlinic.ui.navigation

import android.net.Uri

sealed class Routes(val route: String){
    object Home : Routes("home")
    object Schedule : Routes("schedule")
    object Report : Routes("report")
    object Profile : Routes("profile")
    object DoctorCalendar : Routes("DoctorCalendar")
    object Notifications : Routes("notifications")

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

    companion object {
        const val USER_SELECTION = "user_selection"

        const val SIGNUP = "signup"
        const val FORGET_PASSWORD = "forget_password"

        const val PROFILE = "profile/{role}/{userId}/{staffId}"
        const val PROFILE_WITHOUT_STAFF = "profile/{role}/{userId}"
        const val PROFILE_ROLE_ONLY = "profile/{role}"

        const val EDIT_PROFILE = "edit_profile/{role}/{userId}/{staffId}"
        const val EDIT_PROFILE_WITHOUT_STAFF = "edit_profile/{role}/{userId}"

        const val BOOK_APPOINTMENT = "book_appointment/{doctorId}"

        @JvmStatic
        // Helper functions for navigation
        fun profileRoute(role: String, userId: String, staffId: String? = null): String {
            // Treat blank staffId as absent
            val staffPart = staffId?.takeIf { it.isNotBlank() }

            // If caller didn't provide a userId and no staffId is given, navigate to role-only variant.
            if (userId.isBlank() && staffPart == null) {
                return "profile/$role"
            }

            // determine ID to load
            val uidPart = userId.takeIf { it.isNotBlank() } ?: "current"

            return if (staffPart != null) {
                "profile/$role/$uidPart/$staffPart"
            } else {
                "profile/$role/$uidPart"
            }
        }
    }

    fun editProfileRoute(role: String, userId: String, staffId: String? = null): String {
        return if (staffId != null) {
            "edit_profile/$role/$userId/$staffId"
        } else {
            "edit_profile/$role/$userId"
        }
    }
}