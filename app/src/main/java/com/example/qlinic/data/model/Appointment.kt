package com.example.qlinic.data.model

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val details: String? = null, // e.g., Specialty for doctors, Age for patients
    val imageUrl: String? = null
)

data class Appointment(
    val id: String,
    val dateTime: String, // Use proper LocalDateTime in real app
    val doctor: User,     // The doctor involved
    val patient: User,    // The patient involved
    val locationOrRoom: String,
    val status: AppointmentStatus
)