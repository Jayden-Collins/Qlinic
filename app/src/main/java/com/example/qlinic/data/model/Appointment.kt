package com.example.qlinic.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

data class Appointment(
    val appointmentID: String = "", // appointment id
    val appointmentDateTime : Timestamp = Timestamp.now(), // date and time of appointment
    val isReminderSent: Boolean = false, // whether a reminder has been sent
    val patientID: String = "", // reference to the patient
    val slotID: String = "", // reference to the time slot
    val status: String = "", // status of the appointment
    val symptoms: String = "" // symptoms described by the patient
)
