package com.example.qlinic.data.model

import java.util.Date

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val details: String? = null, // e.g., Specialty for doctors, Age for patients
    val imageUrl: String? = null
)

//data class Appointment(
//    val id: String,
//    val dateTime: String, // Use proper LocalDateTime in real app
//    val doctor: User,     // The doctor involved
//    val patient: User,    // The patient involved
//    val locationOrRoom: String,
//    val status: AppointmentStatus
//)

data class Appointment(
    val appointmentId: String = "",
    val appointmentDate: Date = Date(),
    val symptoms: String = "",
    val status: String = "Booked", // Booked, Cancelled, Ongoing, No-Show, Completed
    @JvmField
    val isNotifSent: Boolean = false,
    val slotId: String = "",
    val patientId: String = "",
)
