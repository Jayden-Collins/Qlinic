package com.example.qlinic.data.model

enum class UserRole { PATIENT, STAFF, DOCTOR }
enum class AppointmentStatus(val displayName: String) {
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}