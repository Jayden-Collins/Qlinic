package com.example.qlinic.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Appointment(
    val appointmentId: String = "",
    val appointmentDate: Date = Date(),
    val symptoms: String = "",
    val status: AppointmentStatus = AppointmentStatus.UPCOMING,
    @JvmField val isNotifSent: Boolean = false,
    val slotId: String = "",
    val patientId: String = "",

    // UI-only properties (not stored in Firestore Appointment collection)
    @get:Exclude @set:Exclude var patient: Patient? = null,
    @get:Exclude @set:Exclude var doctor: ClinicStaff? = null,
    @get:Exclude @set:Exclude var doctorSpecialty: String? = null,
    @get:Exclude @set:Exclude var roomId: String? = null
) {
    // Helper properties to minimize impact on existing UI code
    @get:Exclude
    val id: String get() = appointmentId

    @get:Exclude
    val dateTime: String
        get() = SimpleDateFormat("EEE, MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(appointmentDate)
}
