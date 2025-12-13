package com.example.qlinic.data.model

import java.util.Date

data class Appointment(
    val appointmentId: String = "",
    val appointmentDate: Date = Date(),
    val status: String = "Booked", // Booked, Cancelled, Ongoing, No-Show, Completed
    val isNotifSent: Boolean = false,
    val slotId: String = "",
    val patientId: String = "",
)
