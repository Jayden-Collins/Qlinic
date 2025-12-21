package com.example.qlinic.data.model

enum class UserRole { PATIENT, STAFF, DOCTOR }
enum class AppointmentStatus(val displayName: String) {
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    ONGOING("On Going"),
    NO_SHOW("No Show")
}

data class CurrentUserInfo(
    val id: String,
    val role: UserRole,
    val name: String
)
