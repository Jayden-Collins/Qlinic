package com.example.qlinic.data.model

import com.google.firebase.firestore.PropertyName

enum class UserRole { PATIENT, STAFF, DOCTOR }

enum class AppointmentStatus(val displayName: String) {
    @PropertyName("Upcoming")
    UPCOMING("Upcoming"),

    @PropertyName("Completed")
    COMPLETED("Completed"),

    @PropertyName("Cancelled")
    CANCELLED("Cancelled"),

    @PropertyName("On Going")
    ONGOING("On Going"),

    @PropertyName("No Show")
    NO_SHOW("No Show")
}

data class CurrentUserInfo(
    val id: String,
    val role: UserRole,
    val name: String
)
